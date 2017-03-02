package com.github.guwenk.smuradio;

import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.os.Build;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Objects;


public class AdminActivity extends AppCompatActivity {
    private String pass = "6f9ed6e8c912cd72694616d106316d3c70d8411a5f80e7808fdd3ca00ea34e06";
    private String inputPass;
    private ImageView backgroundImage;
    SharedPreferences sp;
    TextView tv;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin);

        backgroundImage = (ImageView)findViewById(R.id.aa_backgroundImage);
        sp = PreferenceManager.getDefaultSharedPreferences(this);
        tv = (TextView)findViewById(R.id.tvLog);
        refreshLog();
        tv.setMovementMethod(new ScrollingMovementMethod());

        final EditText etPass = (EditText)findViewById(R.id.etPass);

        final Button btnNext = (Button)findViewById(R.id.btnNext);
        btnNext.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                inputPass = etPass.getText().toString();
                try {
                    inputPass = new SHA_256().hashing(inputPass);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                    if (Objects.equals(inputPass, pass)){
                        new UrlRequest("next", getString(R.string.request_address), getString(R.string.request_pass)).start();
                    } else {
                        Toast.makeText(getApplicationContext(), R.string.wrong_password, Toast.LENGTH_SHORT).show();
                    }
                }

            }
        });

        final Button btnClear = (Button)findViewById(R.id.buttonClearLog);
        btnClear.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                tv.setText("");
                SharedPreferences.Editor ed = sp.edit();
                ed.putString("SAVED_TEXT", "");
                ed.apply();
            }
        });

        final Button btnPrev = (Button)findViewById(R.id.btnPrev);
        btnPrev.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                inputPass = etPass.getText().toString();
                try {
                    inputPass = new SHA_256().hashing(inputPass);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                    if (Objects.equals(inputPass, pass)){
                        new UrlRequest("prev", getString(R.string.request_address), getString(R.string.request_pass)).start();
                    } else {
                        Toast.makeText(getApplicationContext(), R.string.wrong_password, Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        String path = sp.getString("backgroundPath", "");
        Bitmap backgroundBitmap = null;
        if (path.equals("")){
            backgroundImage.setImageResource(R.drawable.clocks_bg);
        } else {
            backgroundBitmap = new FileManager(getApplicationContext()).loadBitmap(path, "background");
            backgroundImage.setImageBitmap(backgroundBitmap);
        }
    }

    void refreshLog(){
        new Thread(new Runnable() {
            public void run() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        tv.setText(sp.getString("SAVED_TEXT", ""));
                        tv.setGravity(Gravity.BOTTOM);
                    }
                });
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (!sp.getString("SAVED_TEXT", "").equals(tv.getText()))
                            tv.setText(sp.getString("SAVED_TEXT", ""));
                        tv.setGravity(Gravity.NO_GRAVITY);
                    }
                });
                while (!Thread.currentThread().isInterrupted()) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (!sp.getString("SAVED_TEXT", "").equals(tv.getText()))
                                tv.setText(sp.getString("SAVED_TEXT", ""));
                        }
                    });
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }).start();
    }
}
