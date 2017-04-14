package com.github.guwenk.smuradio;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.RemoteViews;
import android.widget.Toast;

import com.un4seen.bass.BASS;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;


public class PlayerService extends Service {
    private static final int BASS_SYNC_HLS_SEGMENT = 0x10300;
    private static final int BASS_TAG_HLS_EXTINF = 0x14000;
    private final Object lock = new Object();
    boolean isStarted = false;
    private String LOG_TAG = "PLAYER_SERVICE";
    private AFListener afListener;
    private String AF_LOG_TAG = "AudioFocusListener";
    private AudioManager audioManager;
    private MyBinder binder = new MyBinder();
    private RemoteViews views;
    private Notification status;
    private SharedPreferences sPref;
    private boolean isPlaying = false;
    private boolean reconnectCancel = false;

    private int req, chan;
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
                updateUI(Constants.UI.STATUS, getString(R.string.buffering) + " (" + progress + "%)");
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
        sPref = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        Log.d(LOG_TAG, "CREATED");
    }

    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent.getAction().equals(Constants.ACTION.STARTFOREGROUND_ACTION)) {
            if (!isStarted) {
                showNotification();
                Log.d(LOG_TAG, "START_BASS");
                if (BASS.BASS_Init(-1, 44100, 0)) {
                    BASS.BASS_SetConfig(BASS.BASS_CONFIG_NET_READTIMEOUT, 15000); // read timeout
                    BASS.BASS_SetConfig(BASS.BASS_CONFIG_NET_TIMEOUT, 5000); // connection timeout
                    BASS.BASS_SetConfig(BASS.BASS_CONFIG_NET_PLAYLIST, 1); // enable playlist processing
                    BASS.BASS_SetConfig(BASS.BASS_CONFIG_NET_PREBUF, 0); // minimize automatic pre-buffering, so we can do it (and display it) instead
                    // load AAC and HLS add-ons (if present)
                    BASS.BASS_PluginLoad("libbass_aac.so", 0);
                    BASS.BASS_PluginLoad("libbasshls.so", 0);
                }
                startPlayer();
                isStarted = true;
            } else {
                if (isPlaying)
                    stopBASS();
                else {
                    afListener = new AFListener();
                    int requestResult = audioManager.requestAudioFocus(afListener, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
                    Log.i(AF_LOG_TAG, "Music request focus, result: " + requestResult);
                    startPlayer();
                }
                updateUI(Constants.UI.BUTTON, null);
            }
        } else if (intent.getAction().equals(Constants.ACTION.PLAY_ACTION)) {
            if (isPlaying)
                stopBASS();
            else {
                afListener = new AFListener();
                int requestResult = audioManager.requestAudioFocus(afListener, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
                Log.i(AF_LOG_TAG, "Music request focus, result: " + requestResult);
                startPlayer();
            }
        } else if (intent.getAction().equals(Constants.ACTION.STOPFOREGROUND_ACTION)) {
            fullStopBASS();
            updateUI(Constants.UI.BUTTON, null);
            stopForeground(true);
            stopSelf();
        }
        return super.onStartCommand(intent, flags, startId);
    }

    private void startPlayer() {
        if (new InternetChecker().hasConnection(getApplicationContext())) {
            new Thread(new BASS_OpenURL(sPref.getString(Constants.PREFERENCES.LINK, getString(R.string.link_128)))).start();
            isPlaying = true;
            updateUI(Constants.UI.BUTTON, null);
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
        if (new InternetChecker().hasConnection(getApplicationContext()) && !reconnectCancel){
            new Thread(new BASS_OpenURL(sPref.getString(Constants.PREFERENCES.LINK, getString(R.string.link_128)))).start();
        } else if (reconnectCancel) {
            reconnectCancel = false;
        } else {
            updateUI(Constants.UI.STATUS, getString(R.string.waiting_for_internet));
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            reconnectPlayer();
        }
    }


    private void updateActivity(String message_type, String message) {
        SharedPreferences.Editor ed = sPref.edit();
        if (message_type.equals(Constants.MESSAGE.MUSIC_TITLE)) {
            ed.putString(Constants.MESSAGE.MUSIC_TITLE, message);
            Log.d("SharedPref", "put title");
        } else if (message_type.equals(Constants.MESSAGE.PLAYER_STATUS)) {
            if (isPlaying) ed.putInt(Constants.MESSAGE.PLAYER_STATUS, 1);
            else ed.putInt(Constants.MESSAGE.PLAYER_STATUS, 0);
            Log.d("SharedPref", "put status");
        }
        ed.apply();
    }

    private void showNotification() {
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

    void updateUI(String element, String text) {
        switch (element) {
            case Constants.UI.STATUS: {
                refreshTitle(text);
                updateActivity(Constants.MESSAGE.MUSIC_TITLE, text);
                break;
            }
            case Constants.UI.BUTTON: {
                if (isPlaying) toStopButton();
                else {
                    toPlayButton();
                    updateUI(Constants.UI.STATUS, getString(R.string.default_status));
                }
                updateActivity(Constants.MESSAGE.PLAYER_STATUS, null);
                break;
            }
        }
    }

    private void fullStopBASS() {
        audioManager.abandonAudioFocus(afListener);
        Log.d(LOG_TAG, "FULL_STOP_BASS");
        BASS.BASS_StreamFree(chan);
        BASS.BASS_Free();
        isPlaying = false;
        updateUI(Constants.UI.BUTTON, null);
    }

    private void stopBASS() {
        audioManager.abandonAudioFocus(afListener);
        reconnectCancel = true;
        Log.d(LOG_TAG, "STOP_BASS");
        BASS.BASS_StreamFree(chan);
        isPlaying = false;
        updateUI(Constants.UI.BUTTON, null);
    }

    private void toPlayButton() {
        views.setImageViewResource(R.id.status_bar_play, R.drawable.ic_play_circle_outline_24px);
        startForeground(Constants.NOTIFICATION_ID.FOREGROUND_SERVICE, status);
    }

    private void toStopButton() {
        views.setImageViewResource(R.id.status_bar_play, R.drawable.ic_pause_circle_outline_24px);
        startForeground(Constants.NOTIFICATION_ID.FOREGROUND_SERVICE, status);
    }

    private void refreshTitle(String title) {
        views.setTextViewText(R.id.status_bar_track_name, title);
        startForeground(Constants.NOTIFICATION_ID.FOREGROUND_SERVICE, status);
    }

    public IBinder onBind(Intent intent) {
        return binder;
    }


    @Override
    public boolean onUnbind(Intent intent) {
        if (isStarted){
            stopBASS();
            stopSelf();
        }
        return super.onUnbind(intent);
    }


    private void bassError(final String es) {
        final int errorCode = BASS.BASS_ErrorGetCode();
        //@SuppressLint("DefaultLocale") String s = String.format("%s\n(error cod: %d)", es, errorCode);
        SharedPreferences.Editor ed = sPref.edit();
        SimpleDateFormat format = new SimpleDateFormat("MM-dd HH:mm:ss", Locale.getDefault());
        String myDate = format.format(new Date());
        String savedText = sPref.getString(Constants.UI.BASS_ERROR_LOG, "");
        ed.putString(Constants.UI.BASS_ERROR_LOG, savedText + myDate + " | E:" + errorCode + " " + new Constants().getBASS_ErrorFromCode(errorCode) + " (" + es + ")\n");
        ed.apply();
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
                updateUI(Constants.UI.STATUS, title);
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
                        updateUI(Constants.UI.STATUS, title + " - " + title);
                    } else {
                        updateUI(Constants.UI.STATUS, title);
                    }
                }
            } else {
                meta = (String) BASS.BASS_ChannelGetTags(chan, BASS_TAG_HLS_EXTINF);
                if (meta != null) {
                    int i = meta.indexOf(',');
                    if (i > 0) {
                        updateUI(Constants.UI.STATUS, meta.substring(i + 1));
                    }
                }
            }
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
            updateUI(Constants.UI.STATUS, getString(R.string.connecting));

            int connection = BASS.BASS_StreamCreateURL(url, 0, BASS.BASS_STREAM_BLOCK | BASS.BASS_STREAM_STATUS | BASS.BASS_STREAM_AUTOFREE, StatusProc, r);
            synchronized (lock) {
                if (r != req) {
                    if (connection != 0) BASS.BASS_StreamFree(connection);
                    return;
                }
                chan = connection;
            }
            if (chan == 0) {
                bassError(getString(R.string.cant_play_the_stream));
            } else {
                handler.postDelayed(timer, 50);
                Log.d("BASS_VOLUME", BASS.BASS_GetVolume() + "");
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
}
