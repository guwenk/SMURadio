package com.github.guwenk.smuradio;

import android.annotation.SuppressLint;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.un4seen.bass.BASS;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;


public class MainActivity extends AppCompatActivity {


    private String streamLink;
    protected RadioPlayer radioPlayer;
    private SharedPreferences sp;
    protected NotificationService notifService;
    private ImageView backgroundImage;
    boolean userStop = false;
    AudioManager audioManager;
    AFListener afListener;
    String AF_LOG_TAG = "AudioFocusListener";
    boolean radioStatus = false;
    String focusLastReason;
    ServiceConnection serviceConnection;
    private static long back_pressed;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        backgroundImage = (ImageView) findViewById(R.id.main_backgroundImage);
        final Button btnToTrackOrder = (Button) findViewById(R.id.main_btnToTrackOrder);
        btnToTrackOrder.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (new InternetChecker().hasConnection(getApplicationContext())) {
                    Intent intent = new Intent(MainActivity.this, OrderActivity.class);
                    startActivity(intent);
                }
            }
        });
        final ImageButton btnPlay = (ImageButton) findViewById(R.id.main_play_button);
        btnPlay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                userClickPlayStop();
            }
        });
    }

    void userClickPlayStop() {
        userStop = radioStatus;
        if (!radioStatus) {
            if (new InternetChecker().hasConnection(getApplicationContext())) {
                doPlayPause(false);
                radioStatus = !radioStatus;
                changeRadioStatus();
            } else
                Toast.makeText(getApplicationContext(), "Проверьте подключение к интернету", Toast.LENGTH_LONG).show();
        } else {
            killPlayer();
            radioStatus = !radioStatus;
            changeRadioStatus();
        }
    }

    void doPlayPause(boolean isAutoReconnect) {
        if (radioPlayer == null) {
            if (new InternetChecker().hasConnection(getApplicationContext()) || isAutoReconnect) {
                if (!isAutoReconnect) {
                    afListener = new AFListener();
                    int requestResult = audioManager.requestAudioFocus(afListener, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
                    Log.i(AF_LOG_TAG, "Music request focus, result: " + requestResult);
                }
                if (notifService == null) {
                    Intent intentNotification = new Intent(MainActivity.this, NotificationService.class);
                    intentNotification.setAction(Constants.ACTION.STARTFOREGROUND_ACTION);
                    startService(intentNotification);
                    serviceConnection = new ServiceConnection() {
                        @Override
                        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
                            notifService = ((NotificationService.MyBinder) iBinder).getService();
                            notifService.registerClient(MainActivity.this);
                        }

                        @Override
                        public void onServiceDisconnected(ComponentName componentName) {
                            notifService = null;
                        }
                    };
                    bindService(intentNotification, serviceConnection, 0);
                }
                radioPlayer = new RadioPlayer(this);
                radioPlayer.startPlayer(streamLink);
            }
        } else if (!isAutoReconnect) {
            killPlayer();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        sp = PreferenceManager.getDefaultSharedPreferences(this);
        streamLink = sp.getString("link", "http://free.radioheart.ru:8000/guwenk128");

        String path = sp.getString("backgroundPath", "");
        Bitmap backgroundBitmap = null;
        if (path.equals("")) {
            backgroundImage.setImageResource(R.drawable.main_background);
        } else {
            backgroundBitmap = new FileManager(getApplicationContext()).loadBitmap(path, "background");
            backgroundImage.setImageBitmap(backgroundBitmap);
        }
        Log.i("Load_Background", backgroundBitmap + "");
    }


    @Override
    protected void onResume() {
        super.onResume();
        sp = PreferenceManager.getDefaultSharedPreferences(this);
        streamLink = sp.getString("link", "http://free.radioheart.ru:8000/guwenk128");
    }


    @Override
    public void onBackPressed() {
        if (back_pressed + 2000 > System.currentTimeMillis())
            super.onBackPressed();
        else
            Toast.makeText(getBaseContext(), R.string.on_back_pressed,
                    Toast.LENGTH_SHORT).show();
        back_pressed = System.currentTimeMillis();
    }

    void stopPlayer() {
        if (radioPlayer != null) {
            radioPlayer.stopBASS();
        }
    }

    void killPlayer() {
        if (radioPlayer != null) {
            radioPlayer.stopBASS();
        }
        BASS.BASS_Free();
        changeRadioStatus();
        audioManager.abandonAudioFocus(afListener);
        Log.i(AF_LOG_TAG, "abandoned");
    }

    private class RunnableParam implements Runnable {
        Object param;

        RunnableParam(Object p) {
            param = p;
        }

        public void run() {
        }
    }

    void Error(final String es) {
        // get error code in current thread for display in UI thread
        final int errorCode = BASS.BASS_ErrorGetCode();
        @SuppressLint("DefaultLocale") String s = String.format("%s\n(error code: %d)", es, errorCode);
        runOnUiThread(new MainActivity.RunnableParam(s) {
            public void run() {
                SharedPreferences sPref = PreferenceManager.getDefaultSharedPreferences(MainActivity.this);
                if (sPref.getBoolean("showBASS_alerts", false))
                    new AlertDialog.Builder(MainActivity.this).setMessage((String) param).setPositiveButton("OK", null).show();
                stopPlayer();
                SharedPreferences.Editor ed = sPref.edit();
                SimpleDateFormat format = new SimpleDateFormat("MM-dd HH:mm:ss", Locale.getDefault());
                String myDate = format.format(new Date());
                String savedText = sPref.getString("SAVED_TEXT", "");
                ed.putString("SAVED_TEXT", savedText + myDate + " | E:" + errorCode + " " + new Constants().getBASS_ErrorFromCode(errorCode) + "\n");
                ed.apply();
                if (sPref.getBoolean("reconnect", true) && errorCode != 14 && !userStop) {
                    Thread timeoutThread = new Thread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                Thread.sleep(500);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    doPlayPause(true);
                                }
                            });
                        }
                    });
                    timeoutThread.start();
                }
            }
        });
    }

    void changeRadioStatus() {
        ImageButton imgBtn = (ImageButton) findViewById(R.id.main_play_button);
        if (radioStatus) {
            findViewById(R.id.main_status1).setVisibility(View.VISIBLE);
            imgBtn.setImageResource(R.drawable.ic_stop);
            if (notifService != null)
                notifService.toStopButton();
        } else {
            ((TextView) findViewById(R.id.main_status1)).setText("");
            findViewById(R.id.main_status1).setVisibility(View.INVISIBLE);
            imgBtn.setImageResource(R.drawable.ic_play_arrow);
            if (notifService != null) {
                notifService.refreshTitle(getString(R.string.someradio));
                notifService.toPlayButton();
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Intent intent;
        int id = item.getItemId();
        switch (id) {
            case R.id.action_settings:
                intent = new Intent(MainActivity.this, SettingsActivity.class);
                startActivity(intent);
                return true;
            case R.id.action_toAdminActivity:
                intent = new Intent(MainActivity.this, AdminActivity.class);
                startActivity(intent);
                return true;
            case R.id.action_copy:
                if (streamLink != null) {
                    ClipboardManager clipboard = (ClipboardManager) getApplicationContext().getSystemService(Context.CLIPBOARD_SERVICE);
                    ClipData clipData = ClipData.newPlainText("", streamLink);
                    clipboard.setPrimaryClip(clipData);
                    Toast.makeText(getApplicationContext(), getString(R.string.link_was_copied), Toast.LENGTH_SHORT).show();
                } else
                    Toast.makeText(getApplicationContext(), getString(R.string.error) + " Stream link not found!", Toast.LENGTH_SHORT).show();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (radioStatus) userClickPlayStop();
        unbindService(serviceConnection);
    }

    private class AFListener implements AudioManager.OnAudioFocusChangeListener {
        @Override
        public void onAudioFocusChange(int focusChange) {
            String event = "";
            switch (focusChange) {
                case AudioManager.AUDIOFOCUS_LOSS:
                    event = "AUDIOFOCUS_LOSS";
                    focusLastReason = event;
                    userClickPlayStop();
                    break;
                case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                    event = "AUDIOFOCUS_LOSS_TRANSIENT";
                    focusLastReason = event;
                    stopPlayer();
                    break;
                case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                    event = "AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK";
                    focusLastReason = event;
                    BASS.BASS_SetVolume((float) 0.5);
                    break;
                case AudioManager.AUDIOFOCUS_GAIN:
                    event = "AUDIOFOCUS_GAIN";
                    if (focusLastReason.equals("AUDIOFOCUS_LOSS_TRANSIENT")) {
                        userClickPlayStop();
                    }
                    BASS.BASS_SetVolume((float) 1.0);
                    break;
            }
            Log.i(AF_LOG_TAG, "onAudioFocusChange: " + event);
        }
    }
}
