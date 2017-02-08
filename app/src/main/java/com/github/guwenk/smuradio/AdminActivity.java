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
    private String pass = "6f9ed6e8c912cd72694616d106316d3c70d8411a5f80e7808fdd3ca00ea34e06";
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
                        new UrlRequest("next", getString(R.string.request_address), getString(R.string.request_pass)).start();
                    } else {
                        Toast.makeText(getApplicationContext(), R.string.wrong_password, Toast.LENGTH_SHORT).show();
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
                        new UrlRequest("prev", getString(R.string.request_address), getString(R.string.request_pass)).start();
                    } else {
                        Toast.makeText(getApplicationContext(), R.string.wrong_password, Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });
    }
}
