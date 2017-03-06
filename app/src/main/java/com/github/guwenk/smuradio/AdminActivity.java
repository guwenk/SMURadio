package com.github.guwenk.smuradio;

import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;


public class AdminActivity extends AppCompatActivity {
    private String inputPass;
    private ImageView backgroundImage;
    SharedPreferences sp;
    TextView tv;
    DatabaseReference mRootRef = FirebaseDatabase.getInstance().getReference();
    DatabaseReference mRequestsRef = mRootRef.child("Requests");

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
                mRequestsRef.child("pass").setValue(inputPass);
                mRequestsRef.child("cmd").setValue("next");
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
                    mRequestsRef.child("pass").setValue(inputPass);
                    mRequestsRef.child("cmd").setValue("prev");
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        String path = sp.getString("backgroundPath", "");
        Bitmap backgroundBitmap;
        if (path.equals("")){
            backgroundImage.setImageResource(R.drawable.main_background);
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
                    Thread.sleep(100);
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
