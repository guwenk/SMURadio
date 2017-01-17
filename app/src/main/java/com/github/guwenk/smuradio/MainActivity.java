package com.github.guwenk.smuradio;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import java.io.IOException;


public class MainActivity extends AppCompatActivity {

    MediaPlayer mediaPlayer;
    AudioManager am;
    boolean bRadioCreate = false;
    String radioUrl = "http://free.radioheart.ru:8000/guwenk";
    String bitrate;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        am = (AudioManager) getSystemService(AUDIO_SERVICE);

        final ToggleButton toggleButton = (ToggleButton)findViewById(R.id.toggleButton);
        toggleButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (toggleButton.isChecked()){
                    if (new InternetChecker().hasConnection(getApplicationContext())){
                        if (radioUrl != null) {
                            mediaPlayer = new MediaPlayer();
                            new RadioPlayer().execute();
                        }else {
                            Toast.makeText(getApplicationContext(), R.string.choose_bitrate, Toast.LENGTH_SHORT).show();
                            toggleButton.setChecked(false);
                        }
                    } else {
                        toggleButton.setChecked(false);
                    }
                }else{
                    mediaPlayer.release();
                    mediaPlayer = null;
                }
            }
        });

        final Button btnToTrackOrder = (Button)findViewById(R.id.btnToTrackOrder);
        btnToTrackOrder.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (new InternetChecker().hasConnection(getApplicationContext())){
                    Intent intent = new Intent(MainActivity.this, VoteActivity.class);
                    startActivity(intent);
                }
            }
        });

        final Button btnCopyToClipboard = (Button)findViewById(R.id.btnCopyToClipBoard);
        btnCopyToClipboard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (radioUrl != null) {
                    ClipboardManager clipboard = (ClipboardManager) getApplicationContext().getSystemService(Context.CLIPBOARD_SERVICE);
                    ClipData clipData = ClipData.newPlainText("", radioUrl+bitrate);
                    clipboard.setPrimaryClip(clipData);
                    Toast.makeText(getApplicationContext(), getString(R.string.link_was_copied) + " (" + bitrate + "kbps)", Toast.LENGTH_SHORT).show();
                }else Toast.makeText(getApplicationContext(), R.string.choose_bitrate, Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
        bitrate = sp.getString("bitrate", "128");
    }

    @Override
    protected void onResume() {
        super.onResume();
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
        bitrate = sp.getString("bitrate", "128");
    }

    protected class RadioPlayer extends AsyncTask<String, Void, Void>{
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            Toast.makeText(getApplicationContext(), "Загрузка может занять более 10 секунд", Toast.LENGTH_LONG).show();
        }

        @Override
        protected Void doInBackground(String... strings) {
            try {
                mediaPlayer.setDataSource(radioUrl+bitrate);
                mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
                mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                    @Override
                    public void onPrepared(MediaPlayer mediaPlayer) {
                        mediaPlayer.start();
                    }
                });
                mediaPlayer.prepareAsync();
                bRadioCreate = true;
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
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
                //Toast.makeText(getApplicationContext(), "Coming soon...", Toast.LENGTH_SHORT).show();
                intent = new Intent(MainActivity.this, SettingsActivity.class);
                startActivity(intent);
                return true;
            case R.id.action_toAdminActivity:
                if (new InternetChecker().hasConnection(getApplicationContext())){
                    intent = new Intent(MainActivity.this, AdminActivity.class);
                    startActivity(intent);
                }
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}
