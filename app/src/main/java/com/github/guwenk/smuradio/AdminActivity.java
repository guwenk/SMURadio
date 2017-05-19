package com.github.guwenk.smuradio;

import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;


public class AdminActivity extends AppCompatActivity {

    DatabaseReference mRootRef = FirebaseDatabase.getInstance().getReference();
    DatabaseReference mRequestsRef = mRootRef.child("Requests");
    private String inputPass;
    private ImageView backgroundImage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin);
        backgroundImage = (ImageView) findViewById(R.id.aa_backgroundImage);
        final EditText etPass = (EditText) findViewById(R.id.etPass);
        final Button btnNext = (Button) findViewById(R.id.btnNext);
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
                Toast.makeText(getApplicationContext(), "Запрос отправлен (next)", Toast.LENGTH_SHORT).show();
            }
        });

        final Button btnPrev = (Button) findViewById(R.id.btnPrev);
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
                Toast.makeText(getApplicationContext(), "Запрос отправлен (prev)", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
        String path = sp.getString(Constants.PREFERENCES.BACKGROUND_PATH, "");
        Bitmap backgroundBitmap;
        if (path.equals("")) {
            backgroundImage.setImageResource(R.drawable.main_background);
        } else {
            backgroundBitmap = new FileManager(getApplicationContext()).loadBitmap(path, Constants.PREFERENCES.BACKGROUND);
            backgroundImage.setImageBitmap(backgroundBitmap);
        }
    }

}
