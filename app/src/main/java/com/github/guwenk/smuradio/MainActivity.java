package com.github.guwenk.smuradio;

import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;
import android.widget.ToggleButton;

import java.io.IOException;


public class MainActivity extends AppCompatActivity {

    MediaPlayer mediaPlayer;
    AudioManager am;
    boolean bRadioCreate = false;


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
                        mediaPlayer = new MediaPlayer();
                        new RadioPlayer().execute();
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

        final Button btnToAdminMenu = (Button)findViewById(R.id.btnToAdminMenu);
        btnToAdminMenu.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (new InternetChecker().hasConnection(getApplicationContext())){
                    Intent intent = new Intent(MainActivity.this, AdminActivity.class);
                    startActivity(intent);
                }
            }
        });
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
                mediaPlayer.setDataSource("http://free.radioheart.ru:8000/guwenk");
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
}
