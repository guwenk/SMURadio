package com.github.guwenk.smuradio;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.media.session.MediaSession;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.KeyEvent;
import android.widget.RemoteViews;
import android.widget.Toast;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.un4seen.bass.BASS;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;


public class PlayerService extends Service {
    private static final int BASS_SYNC_HLS_SEGMENT = 0x10300;
    private static final int BASS_TAG_HLS_EXTINF = 0x14000;
    private static long mediaKeyPressed = 0;
    private final Object lock = new Object();
    private Toast toast;
    private boolean isStarted = false;
    private AFListener afListener;
    private String AF_LOG_TAG = "AudioFocusListener";
    private AudioManager audioManager;
    private MyBinder binder = new MyBinder();
    private RemoteViews views;
    private Notification status;
    private SharedPreferences sPref;
    private boolean isPlaying = false;
    private boolean reconnectCancel = false;
    private SpeakerChecker speakerChecker;
    private String temp_title = "";
    private boolean btnStatus = true;
    private int req, chan;
    private MediaSession mediaSession;
    private Handler handler = new Handler();
    private BASS.SYNCPROC MetaSync = new BASS.SYNCPROC() {
        public void SYNCPROC(int handle, int channel, int data, Object user) {
            doMeta();
        }
    };
    private BASS.SYNCPROC EndSync = new BASS.SYNCPROC() {
        @Override
        public void SYNCPROC(int handle, int channel, int data, Object user) {
            bassError("EndSync");
        }
    };
    private Runnable timer = new Runnable() {
        @Override
        public void run() {
            int progress = (int) BASS.BASS_StreamGetFilePosition(chan, BASS.BASS_FILEPOS_BUFFER);
            if (progress < 0) return;
            progress = progress * 100 / (int) BASS.BASS_StreamGetFilePosition(chan, BASS.BASS_FILEPOS_END);
            if (progress > 75 || BASS.BASS_StreamGetFilePosition(chan, BASS.BASS_FILEPOS_CONNECTED) == 0) {
                doMeta();
                BASS.BASS_ChannelSetSync(chan, BASS.BASS_SYNC_META, 0, MetaSync, 0); // Shoutcast
                BASS.BASS_ChannelSetSync(chan, BASS.BASS_SYNC_OGG_CHANGE, 0, MetaSync, 0); // Icecast/OGG
                BASS.BASS_ChannelSetSync(chan, BASS_SYNC_HLS_SEGMENT, 0, MetaSync, 0); // HLS
                BASS.BASS_ChannelSetSync(chan, BASS.BASS_SYNC_END, 0, EndSync, 0);
                BASS.BASS_ChannelPlay(chan, false);
            } else {
                updateUI(getString(R.string.buffering) + " (" + progress + "%)");
                handler.postDelayed(this, 50);
            }
        }
    };
    private BASS.DOWNLOADPROC StatusProc = new BASS.DOWNLOADPROC() {
        @Override
        public void DOWNLOADPROC(ByteBuffer buffer, int length, Object user) {
            if ((Integer) user != req) return;
            if (buffer != null && length == 0) {
                try {
                    Charset.forName("ISO-8859-1").newDecoder();
                    ByteBuffer temp = ByteBuffer.allocate(buffer.limit());
                    temp.put(buffer);
                    temp.position(0);
                } catch (Exception ignored) {
                }
            }
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            mediaSession = new MediaSession(getApplicationContext(), "PLAYER_SERVICE");
            mediaSession.setCallback(new MediaSession.Callback() {
                @Override
                public boolean onMediaButtonEvent(@NonNull Intent mediaButtonEvent) {
                    if (PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).getBoolean(Constants.PREFERENCES.HEADSET_BUTTON, true)) {
                        if (mediaButtonEvent.getAction().equals(Intent.ACTION_MEDIA_BUTTON)) {
                            KeyEvent keyEvent = mediaButtonEvent.getParcelableExtra(Intent.EXTRA_KEY_EVENT);
                            if (keyEvent.getKeyCode() == KeyEvent.KEYCODE_HEADSETHOOK) {
                                if (mediaKeyPressed + 1000 > System.currentTimeMillis() && mediaKeyPressed + 100 < System.currentTimeMillis()) {
                                    Intent intentReconnect = new Intent(getApplicationContext(), PlayerService.class);
                                    intentReconnect.setAction(Constants.ACTION.RECONNECT);
                                    getApplicationContext().startService(intentReconnect);
                                }
                                mediaKeyPressed = System.currentTimeMillis();
                            }
                        }
                    }
                    return super.onMediaButtonEvent(mediaButtonEvent);
                }
            });
            mediaSession.setFlags(MediaSession.FLAG_HANDLES_MEDIA_BUTTONS |
                    MediaSession.FLAG_HANDLES_TRANSPORT_CONTROLS);
        }

        speakerChecker = new SpeakerChecker();
        sPref = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
    }

    public int onStartCommand(Intent intent, int flags, int startId) {
        String action = String.valueOf(intent.getAction());

        switch (action) {
            case Constants.ACTION.STARTFOREGROUND_ACTION: {
                if (!isStarted) {
                    showNotification();
                    startPlayer();
                    isStarted = true;
                } else {
                    if (isPlaying)
                        stopBASS();
                    else {
                        startPlayer();
                    }
                }
                break;
            }
            case Constants.ACTION.PLAY_ACTION: {
                if (isPlaying)
                    stopBASS();
                else {
                    startPlayer();
                }
                break;
            }
            case Constants.ACTION.STOPFOREGROUND_ACTION: {
                stopPlayer();
                updateUI(null);
                stopForeground(true);
                stopSelf();
                break;
            }
            case Constants.ACTION.RECONNECT: {
                if (sPref.getBoolean(Constants.PREFERENCES.HEADSET_BUTTON, true)) {
                    if (isPlaying) {
                        BASS.BASS_StreamFree(chan);
                        try {
                            Thread.sleep(100);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        reconnectPlayer();
                    } else {
                        int buffer_size = Integer.parseInt(sPref.getString(Constants.PREFERENCES.BUFFER_SIZE, "5000"));
                        if (BASS.BASS_Init(-1, 44100, 0)) {
                            BASS.BASS_SetConfig(BASS.BASS_CONFIG_NET_READTIMEOUT, 15000); // read timeout
                            BASS.BASS_SetConfig(BASS.BASS_CONFIG_NET_TIMEOUT, 5000); // connection timeout
                            BASS.BASS_SetConfig(BASS.BASS_CONFIG_NET_PLAYLIST, 1); // enable playlist processing
                            BASS.BASS_SetConfig(BASS.BASS_CONFIG_NET_PREBUF, 0); // minimize automatic pre-buffering, so we can do it (and display it) instead
                            if (buffer_size >= 1000 && buffer_size <= 60000)
                                BASS.BASS_SetConfig(BASS.BASS_CONFIG_NET_BUFFER, buffer_size);

                            // load AAC and HLS add-ons (if present)
                            BASS.BASS_PluginLoad("libbass_aac.so", 0);
                            BASS.BASS_PluginLoad("libbasshls.so", 0);
                        }

                        afListener = new AFListener();
                        int requestResult = audioManager.requestAudioFocus(afListener, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
                        Log.i(AF_LOG_TAG, "Music request focus, result: " + requestResult);
                        new Thread(new BASS_OpenURL(sPref.getString(Constants.PREFERENCES.LINK, getString(R.string.link_128)))).start();
                        IntentFilter intentFilter = new IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY);
                        registerReceiver(speakerChecker, intentFilter);
                        isPlaying = true;
                        updateUI(null);
                    }
                }
                break;
            }
        }
        return super.onStartCommand(intent, flags, startId);
    }

    private void startPlayer() {
        reconnectCancel = false;
        if (new InternetChecker().hasConnection(getApplicationContext())) {

            FirebaseDatabase.getInstance().getReference().child(Constants.FIREBASE.SERVER_STATUS).addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    if (!dataSnapshot.getValue(Boolean.class)) {
                        showToast(getBaseContext(), getString(R.string.server_is_off), Toast.LENGTH_LONG);
                        stopBASS();
                    }
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {

                }
            });
            int buffer_size = Integer.parseInt(sPref.getString(Constants.PREFERENCES.BUFFER_SIZE, "5000"));
            if (BASS.BASS_Init(-1, 44100, 0)) {
                BASS.BASS_SetConfig(BASS.BASS_CONFIG_NET_READTIMEOUT, 15000); // read timeout
                BASS.BASS_SetConfig(BASS.BASS_CONFIG_NET_TIMEOUT, 5000); // connection timeout
                BASS.BASS_SetConfig(BASS.BASS_CONFIG_NET_PLAYLIST, 1); // enable playlist processing
                BASS.BASS_SetConfig(BASS.BASS_CONFIG_NET_PREBUF, 0); // minimize automatic pre-buffering, so we can do it (and display it) instead
                if (buffer_size >= 1000 && buffer_size <= 60000)
                    BASS.BASS_SetConfig(BASS.BASS_CONFIG_NET_BUFFER, buffer_size);

                // load AAC and HLS add-ons (if present)
                BASS.BASS_PluginLoad("libbass_aac.so", 0);
                BASS.BASS_PluginLoad("libbasshls.so", 0);
            }

            afListener = new AFListener();
            int requestResult = audioManager.requestAudioFocus(afListener, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
            Log.i(AF_LOG_TAG, "Music request focus, result: " + requestResult);
            isPlaying = true;
            new Thread(new BASS_OpenURL(sPref.getString(Constants.PREFERENCES.LINK, getString(R.string.link_128)))).start();
            IntentFilter intentFilter = new IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY);
            registerReceiver(speakerChecker, intentFilter);
        } else {
            Handler handler = new Handler(Looper.getMainLooper());
            handler.post(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(getApplicationContext(), getString(R.string.check_internet_connection), Toast.LENGTH_SHORT).show();
                }
            });
            stopBASS();
        }
    }

    private void reconnectPlayer() {
        if (new InternetChecker().hasConnection(getApplicationContext()) && !reconnectCancel) {
            new Thread(new BASS_OpenURL(sPref.getString(Constants.PREFERENCES.LINK, getString(R.string.link_128)))).start();
        } else if (reconnectCancel) {
            reconnectCancel = false;
        } else {
            updateUI(getString(R.string.waiting_for_internet));
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            reconnectPlayer();
        }
    }

    @Override
    public void onDestroy() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            mediaSession.setActive(false);
        }
        updateUI(null);
        super.onDestroy();
    }

    private void showNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            mediaSession.setActive(true);
        }

        views = new RemoteViews(getPackageName(), R.layout.notification_layout);

        Intent intentToMainActivity = new Intent(this, MainActivity.class);
        intentToMainActivity.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        PendingIntent pintentToMainActivity = PendingIntent.getActivity(this, 0, intentToMainActivity, 0);

        Intent intentPlayBtn = new Intent(this, PlayerService.class);
        intentPlayBtn.setAction(Constants.ACTION.PLAY_ACTION);
        PendingIntent pintentPlayBtn = PendingIntent.getService(this, 0, intentPlayBtn, 0);
        views.setOnClickPendingIntent(R.id.status_bar_play, pintentPlayBtn);

        Intent intentCloseBtn = new Intent(this, PlayerService.class);
        intentCloseBtn.setAction(Constants.ACTION.STOPFOREGROUND_ACTION);
        PendingIntent pintentCloseBtn = PendingIntent.getService(this, 0, intentCloseBtn, 0);
        views.setOnClickPendingIntent(R.id.status_bar_collapse, pintentCloseBtn);

        views.setImageViewResource(R.id.status_bar_play, R.drawable.ic_pause_circle_outline_24px);
        views.setInt(R.id.small_notification_bg, "setBackgroundResource", R.color.notificationBackground);
        views.setTextViewText(R.id.status_bar_track_name, getString(R.string.default_status));

        status = new Notification.Builder(this).build();
        status.contentView = views;
        status.flags = Notification.FLAG_ONGOING_EVENT;
        status.icon = R.drawable.ic_radio_24dp;
        status.contentIntent = pintentToMainActivity;
        startForeground(Constants.NOTIFICATION_ID.FOREGROUND_SERVICE, status);
    }

    private void updateActivity(String title) {
        MyApplication myApplication = (MyApplication) getApplication();
        myApplication.saveTitle(title);
    }

    void updateUI(String text) {
        if (text != null) {
            refreshTitle(text);
        }
        if (isPlaying) {
            updateActivity(text);
            toStopButton();
        } else {
            refreshTitle(getString(R.string.default_status));
            updateActivity(null);
            toPlayButton();
        }

    }

    private void stopPlayer() {
        stopBASS();
        isStarted = false;
    }

    private void stopBASS() {
        audioManager.abandonAudioFocus(afListener);
        try {
            unregisterReceiver(speakerChecker);
        } catch (IllegalArgumentException ignored) {
        }
        reconnectCancel = true;
        BASS.BASS_StreamFree(chan);
        BASS.BASS_Free();
        isPlaying = false;
        updateUI(null);
    }

    private void toPlayButton() {
        if (isStarted && !btnStatus) {
            btnStatus = true;
            views.setImageViewResource(R.id.status_bar_play, R.drawable.ic_play_circle_outline_24px);
            startForeground(Constants.NOTIFICATION_ID.FOREGROUND_SERVICE, status);
        }
    }

    private void toStopButton() {
        if (isStarted && btnStatus) {
            btnStatus = false;
            views.setImageViewResource(R.id.status_bar_play, R.drawable.ic_pause_circle_outline_24px);
            startForeground(Constants.NOTIFICATION_ID.FOREGROUND_SERVICE, status);
        }
    }

    private void refreshTitle(String title) {
        if (isStarted && !title.equals(temp_title)) {
            temp_title = title;
            views.setTextViewText(R.id.status_bar_track_name, title);
            startForeground(Constants.NOTIFICATION_ID.FOREGROUND_SERVICE, status);
        }
    }

    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        if (!isPlaying) {
            stopPlayer();
            stopSelf();
        }
        return true;
    }

    private void bassError(final String es) {
        final int errorCode = BASS.BASS_ErrorGetCode();
        //@SuppressLint("DefaultLocale") String s = String.format("%s\n(error cod: %d)", es, errorCode);
        //SharedPreferences.Editor ed = sPref.edit();
        //SimpleDateFormat format = new SimpleDateFormat("MM-dd HH:mm:ss", Locale.getDefault());
        //String myDate = format.format(new Date());
        //String savedText = sPref.getString(Constants.UI.BASS_ERROR_LOG, "");
        //ed.putString(Constants.UI.BASS_ERROR_LOG, savedText + myDate + " | E:" + errorCode + " " + new Constants().getBASS_ErrorFromCode(errorCode) + " (" + es + ")\n");
        //ed.apply();
        Log.i("PLAYER_BASS", errorCode + " " + new Constants().getBASS_ErrorFromCode(errorCode) + " (" + es + ")");
        BASS.BASS_StreamFree(chan);
        if (sPref.getBoolean(Constants.PREFERENCES.RECONNECT, true)) {
            reconnectPlayer();
        } else
            stopBASS();
    }

    private void doMeta() {
        String meta = (String) BASS.BASS_ChannelGetTags(chan, BASS.BASS_TAG_META);
        if (meta != null) {
            int ti = meta.indexOf("StreamTitle='");
            if (ti >= 0) {
                String title = meta.substring(ti + 13, meta.indexOf("';", ti + 13));
                updateUI(title);
            }
        } else {
            String[] ogg = (String[]) BASS.BASS_ChannelGetTags(chan, BASS.BASS_TAG_OGG);
            if (ogg != null) {
                String artist = null, title = null;
                for (String s : ogg) {
                    if (s.regionMatches(true, 0, "artist=", 0, 7))
                        artist = s.substring(7);
                    else if (s.regionMatches(true, 0, "title=", 0, 6))
                        title = s.substring(6);
                }
                if (title != null) {
                    if (artist != null) {
                        updateUI(title + " - " + title);
                    } else {
                        updateUI(title);
                    }
                }
            } else {
                meta = (String) BASS.BASS_ChannelGetTags(chan, BASS_TAG_HLS_EXTINF);
                if (meta != null) {
                    int i = meta.indexOf(',');
                    if (i > 0) {
                        updateUI(meta.substring(i + 1));
                    }
                }
            }
        }
    }

    void showToast(final Context appContext, final String message, final int duration) {
        if (null != appContext) {
            Handler handler = new Handler(Looper.getMainLooper());
            handler.post(new Runnable() {
                @Override
                public void run() {
                    if (toast != null) toast.cancel();
                    toast = Toast.makeText(appContext, message, duration);
                    toast.show();
                }
            });
        }
    }

    private class BASS_OpenURL implements Runnable {
        String url;

        BASS_OpenURL(String url) {
            this.url = url;
        }

        @Override
        public void run() {
            int r;
            synchronized (lock) {
                r = ++req;
            }
            BASS.BASS_StreamFree(chan);
            updateUI(getString(R.string.connecting));

            int connection = BASS.BASS_StreamCreateURL(url, 0, BASS.BASS_STREAM_BLOCK | BASS.BASS_STREAM_STATUS | BASS.BASS_STREAM_AUTOFREE, StatusProc, r);
            synchronized (lock) {
                if (r != req) {
                    if (connection != 0) BASS.BASS_StreamFree(connection);
                    return;
                }
                chan = connection;
            }
            if (chan == 0) {
                Log.e("ERRORRRRRRR", url);
                bassError(getString(R.string.cant_play_the_stream));
            } else {
                handler.postDelayed(timer, 50);
            }
        }
    }

    private class AFListener implements AudioManager.OnAudioFocusChangeListener {
        @Override
        public void onAudioFocusChange(int focusChange) {
            String event = "";
            switch (focusChange) {
                case AudioManager.AUDIOFOCUS_LOSS:
                    event = "AUDIOFOCUS_LOSS";
                    stopBASS();
                    break;
                case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                    event = "AUDIOFOCUS_LOSS_TRANSIENT";
                    stopBASS();
                    break;
                case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                    event = "AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK";
                    BASS.BASS_SetVolume((float) 0.5);
                    break;
                case AudioManager.AUDIOFOCUS_GAIN:
                    event = "AUDIOFOCUS_GAIN";
                    if (!isPlaying)
                        startPlayer();
                    BASS.BASS_SetVolume((float) 1.0);
                    break;
            }
            Log.i(AF_LOG_TAG, "onAudioFocusChange: " + event);
        }
    }

    class MyBinder extends Binder {
        PlayerService getService() {
            return PlayerService.this;
        }
    }

    private class SpeakerChecker extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            stopBASS();
        }
    }
}
