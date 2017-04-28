package com.github.guwenk.smuradio;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.widget.Toast;

import java.io.IOException;

public class SettingsActivity extends PreferenceActivity {
    static final int GALLERY_REQUEST = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.settings);
        Preference btnSetBG = findPreference(Constants.PREFERENCES.SET_BACKGROUND);
        btnSetBG.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                Intent photoPickerIntent = new Intent(Intent.ACTION_PICK);
                photoPickerIntent.setType("image/*");
                startActivityForResult(photoPickerIntent, GALLERY_REQUEST);
                return true;
            }
        });
        Preference btnRestoreBG = findPreference(Constants.PREFERENCES.RESTORE_BACKGROUND);
        btnRestoreBG.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(SettingsActivity.this);
                SharedPreferences.Editor ed = sp.edit();
                ed.putString(Constants.PREFERENCES.BACKGROUND_PATH, "");
                ed.apply();
                return true;
            }
        });
        Preference btnBugReport = findPreference(Constants.PREFERENCES.BUG_REPORT);
        btnBugReport.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                Intent i = new Intent(Intent.ACTION_SEND);
                i.setType("message/rfc822");
                i.putExtra(Intent.EXTRA_EMAIL, new String[]{"guwenk@rambler.ru"});
                i.putExtra(Intent.EXTRA_SUBJECT, "SomeRadio bug");
                try {
                    startActivity(Intent.createChooser(i, getString(R.string.send_email)));
                } catch (android.content.ActivityNotFoundException ex) {
                    Toast.makeText(SettingsActivity.this, R.string.no_email_client_installed, Toast.LENGTH_SHORT).show();
                }
                return true;
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent imageReturnedIntent) {
        super.onActivityResult(requestCode, resultCode, imageReturnedIntent);
        Bitmap bitmap = null;

        switch (requestCode) {
            case GALLERY_REQUEST:
                if (resultCode == RESULT_OK) {
                    Uri selectedImage = imageReturnedIntent.getData();
                    try {
                        bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), selectedImage);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    String path = new FileManager(getApplicationContext()).saveBitmap(bitmap, Constants.PREFERENCES.BACKGROUND, "imageDir");
                    SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
                    SharedPreferences.Editor ed = sp.edit();
                    ed.putString(Constants.PREFERENCES.BACKGROUND_PATH, path);
                    ed.apply();
                }
        }
    }
}
