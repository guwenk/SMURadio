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
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
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

    private String radioUrl;
    private String bitrate;
    protected RadioPlayer radioPlayer;
    private SharedPreferences sp;
    protected NotificationService notifService;
    private ImageView backgroundImage;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        backgroundImage = (ImageView)findViewById(R.id.main_backgroundImage);
        radioUrl = getString(R.string.stream_host);
        final Button btnToTrackOrder = (Button) findViewById(R.id.main_btnToTrackOrder);
        btnToTrackOrder.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (new InternetChecker().hasConnection(getApplicationContext())) {
                    Intent intent = new Intent(MainActivity.this, VoteActivity.class);
                    startActivity(intent);
                }
            }
        });
        final ImageButton btnPlay = (ImageButton)findViewById(R.id.main_play_button);
        btnPlay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                doPlayPause();
            }
        });
    }

    void doPlayPause(){
        if (radioPlayer == null){
            if (new InternetChecker().hasConnection(getApplicationContext())) {
                if (notifService == null){
                    Intent intentNotification = new Intent(MainActivity.this, NotificationService.class);
                    intentNotification.setAction(Constants.ACTION.STARTFOREGROUND_ACTION);
                    startService(intentNotification);
                    ServiceConnection serviceConnection = new ServiceConnection() {
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
                radioPlayer.startPlayer(radioUrl, bitrate);
                changeRadioStatus();
            }
        } else {
            killPlayer();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        sp = PreferenceManager.getDefaultSharedPreferences(this);
        bitrate = sp.getString("bitrate", "128");

        String path = sp.getString("backgroundPath", "");
        Bitmap backgroundBitmap = null;
        if (path.equals("")){
            backgroundImage.setImageResource(R.drawable.clocks_bg);
        } else {
            backgroundBitmap = new FileManager(getApplicationContext()).loadBitmap(path, "background");
            backgroundImage.setImageBitmap(backgroundBitmap);
        }
        Log.d("Load_Background", backgroundBitmap+"");
    }


    @Override
    protected void onResume() {
        super.onResume();
        sp = PreferenceManager.getDefaultSharedPreferences(this);
        bitrate = sp.getString("bitrate", "128");

        String path = sp.getString("backgroundPath", "");
        Bitmap backgroundBitmap = null;
        if (path.equals("")){
            backgroundImage.setImageResource(R.drawable.clocks_bg);
        } else {
            backgroundBitmap = new FileManager(getApplicationContext()).loadBitmap(path, "background");
            backgroundImage.setImageBitmap(backgroundBitmap);
        }
    }


    @Override
    public void onBackPressed() {
    }

    void killPlayer(){
        if (radioPlayer != null){
            radioPlayer.stopBASS();
        }
        changeRadioStatus();
    }

    class RunnableParam implements Runnable {
        Object param;
        RunnableParam(Object p) { param=p; }
        public void run() {}
    }

    void Error(final String es) {
        // get error code in current thread for display in UI thread
        final int errorCode = BASS.BASS_ErrorGetCode();
        @SuppressLint("DefaultLocale") String s=String.format("%s\n(error code: %d)", es, errorCode);
        runOnUiThread(new MainActivity.RunnableParam(s) {
            public void run() {
                SharedPreferences sPref = PreferenceManager.getDefaultSharedPreferences(MainActivity.this);
                if (sPref.getBoolean("showBASS_alerts", true))
                    new AlertDialog.Builder(MainActivity.this).setMessage((String)param).setPositiveButton("OK", null).show();
                killPlayer();
                SharedPreferences.Editor ed = sPref.edit();
                SimpleDateFormat format= new SimpleDateFormat("MM-dd HH:mm:ss", Locale.getDefault());
                String myDate = format.format(new Date());
                String savedText = sPref.getString("SAVED_TEXT", "");
                ed.putString("SAVED_TEXT", savedText + myDate+" | E:"+errorCode+ " " + new Constants().getBASS_ErrorFromCode(errorCode)+"\n");
                ed.apply();
                if (sPref.getBoolean("reconnect", false) && errorCode != 14){
                    doPlayPause();
                }
            }
        });
    }

    void changeRadioStatus(){
        ImageButton imgBtn = (ImageButton)findViewById(R.id.main_play_button);
        if (radioPlayer != null){
            findViewById(R.id.main_status1).setVisibility(View.VISIBLE);
            imgBtn.setImageResource(R.drawable.ic_stop);
            if (notifService != null)
                notifService.toStopButton();
        }else{
            ((TextView) findViewById(R.id.main_status1)).setText("");
            findViewById(R.id.main_status1).setVisibility(View.INVISIBLE);
            imgBtn.setImageResource(R.drawable.ic_play_arrow);
            if (notifService != null){
                notifService.refreshTitle(getString(R.string.app_name));
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
                if (radioUrl != null) {
                    ClipboardManager clipboard = (ClipboardManager) getApplicationContext().getSystemService(Context.CLIPBOARD_SERVICE);
                    ClipData clipData = ClipData.newPlainText("", radioUrl+bitrate);
                    clipboard.setPrimaryClip(clipData);
                    Toast.makeText(getApplicationContext(), getString(R.string.link_was_copied) + " (" + bitrate + "kbps)", Toast.LENGTH_SHORT).show();
                }else Toast.makeText(getApplicationContext(), getString(R.string.error) + " Button, Clipboard, Bitrate", Toast.LENGTH_SHORT).show();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        killPlayer();
    }
}
