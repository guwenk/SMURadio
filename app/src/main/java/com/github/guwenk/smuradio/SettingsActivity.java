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
import android.util.Log;

import java.io.IOException;

public class SettingsActivity extends PreferenceActivity {
    static final int GALLERY_REQUEST = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.settings);
        setContentView(R.layout.activity_settings);
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
                ed.putString("backgroundPath", "");
                ed.apply();
                return true;
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent imageReturnedIntent) {
        super.onActivityResult(requestCode, resultCode, imageReturnedIntent);
        Bitmap bitmap = null;
        //ImageView imageView = (ImageView) findViewById(R.id.imageView2);

        switch (requestCode) {
            case GALLERY_REQUEST:
                if (resultCode == RESULT_OK) {
                    Uri selectedImage = imageReturnedIntent.getData();
                    try {
                        bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), selectedImage);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    String path = new FileManager(getApplicationContext()).saveBitmap(bitmap, "background", "imageDir");
                    //bitmap = resize(path);
                    //new FileManager(getApplicationContext()).saveBitmap(bitmap, "background", "imageDir");
                    Log.d("Save_Background", path);
                    SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
                    SharedPreferences.Editor ed = sp.edit();
                    ed.putString("backgroundPath", path);
                    ed.apply();
                }
        }
    }
/*
    Bitmap resize(String path) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(path, options);
        options.inSampleSize = calculateInSampleSize(options);
        options.inJustDecodeBounds = false;
        return BitmapFactory.decodeFile(path, options);
    }

    int calculateInSampleSize(BitmapFactory.Options options) {
        Display display = getWindowManager().getDefaultDisplay();
        int displayHeight = display.getHeight();
        int displayWidth = display.getWidth();
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > displayHeight || width > displayWidth) {

            final int halfHeight = height / 2;
            final int halfWidth = width / 2;

            while ((halfHeight / inSampleSize) > displayHeight
                    && (halfWidth / inSampleSize) > displayWidth) {
                inSampleSize *= 2;
            }
        }

        return inSampleSize;
    }
*/

}
