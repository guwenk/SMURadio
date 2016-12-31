package com.github.guwenk.smuradio;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.Toast;
import android.widget.ToggleButton;

import java.io.IOException;


public class MainActivity extends AppCompatActivity {

    MediaPlayer mediaPlayer;
    AudioManager am;
    boolean bRadioCreate = false;
    String radioUrl;
    int bitrate;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        am = (AudioManager) getSystemService(AUDIO_SERVICE);

        final RadioButton radioBitrate256 = (RadioButton)findViewById(R.id.radioBtn256);
        radioBitrate256.setOnClickListener(radioBitrateClickListener);

        final RadioButton radioBitrate128 = (RadioButton)findViewById(R.id.radioBtn128);
        radioBitrate128.setOnClickListener(radioBitrateClickListener);

        final RadioButton radioBitrate96 = (RadioButton)findViewById(R.id.radioBtn96);
        radioBitrate96.setOnClickListener(radioBitrateClickListener);

        final RadioButton radioBitrate48 = (RadioButton)findViewById(R.id.radioBtn48);
        radioBitrate48.setOnClickListener(radioBitrateClickListener);


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

        final Button btnCopyToClipboard = (Button)findViewById(R.id.btnCopyToClipBoard);
        btnCopyToClipboard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (radioUrl != null) {
                    ClipboardManager clipboard = (ClipboardManager) getApplicationContext().getSystemService(Context.CLIPBOARD_SERVICE);
                    ClipData clipData = ClipData.newPlainText("", radioUrl);
                    clipboard.setPrimaryClip(clipData);
                    switch (bitrate){
                        case 256:
                            Toast.makeText(getApplicationContext(), R.string.link_was_copied256, Toast.LENGTH_SHORT).show();
                            break;
                        case 128:
                            Toast.makeText(getApplicationContext(), R.string.link_was_copied128, Toast.LENGTH_SHORT).show();
                            break;
                        case 96:
                            Toast.makeText(getApplicationContext(), R.string.link_was_copied96, Toast.LENGTH_SHORT).show();
                            break;
                        case 48:
                            Toast.makeText(getApplicationContext(), R.string.link_was_copied48, Toast.LENGTH_SHORT).show();
                            break;
                        default: Toast.makeText(getApplicationContext(), R.string.unknown_error, Toast.LENGTH_SHORT).show();
                    }

                }else Toast.makeText(getApplicationContext(), R.string.choose_bitrate, Toast.LENGTH_SHORT).show();
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
                mediaPlayer.setDataSource(radioUrl);
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


    View.OnClickListener radioBitrateClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            RadioButton rb = (RadioButton)view;
            switch (rb.getId()){
                case R.id.radioBtn256:
                    radioUrl = "http://free.radioheart.ru:8000/guwenk256";
                    bitrate = 256;
                    break;
                case R.id.radioBtn128:
                    radioUrl = "http://free.radioheart.ru:8000/guwenk128";
                    bitrate = 128;
                    break;
                case R.id.radioBtn96:
                    radioUrl = "http://free.radioheart.ru:8000/guwenk96";
                    bitrate = 96;
                    break;
                case R.id.radioBtn48:
                    radioUrl = "http://free.radioheart.ru:8000/guwenk48";
                    bitrate = 48;
                    break;
                default:
                    Toast.makeText(getApplicationContext(), R.string.unknown_error, Toast.LENGTH_SHORT).show();
            }
        }
    };
}
