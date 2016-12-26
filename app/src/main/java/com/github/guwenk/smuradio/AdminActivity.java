package com.github.guwenk.smuradio;

import android.os.Build;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import java.util.Objects;

public class AdminActivity extends AppCompatActivity {
    private String pass = "8887f6929b1af320d05e4aaac0c79f24121f91c5058bac47bc53bdf4b3bd2c8f";
    private String inputPass;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin);


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
                        new UrlRequest("next").start();
                    } else {
                        Toast.makeText(getApplicationContext(), "Wrong password", Toast.LENGTH_SHORT).show();
                    }
                }

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
                        new UrlRequest("prev").start();
                    } else {
                        Toast.makeText(getApplicationContext(), "Wrong password", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });
    }
}
