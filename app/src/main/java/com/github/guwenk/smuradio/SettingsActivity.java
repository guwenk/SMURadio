package com.github.guwenk.smuradio;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Display;
import android.widget.Toast;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class SettingsActivity extends PreferenceActivity implements SharedPreferences.OnSharedPreferenceChangeListener {
    static final int GALLERY_REQUEST = 1;
    private int adminCounter;
    private SharedPreferences sPref;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.settings);

        sPref = PreferenceManager.getDefaultSharedPreferences(this);
        sPref.registerOnSharedPreferenceChangeListener(this);
        adminCounter = 0;

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
                Toast.makeText(getApplicationContext(), R.string.background_restored, Toast.LENGTH_SHORT).show();
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
                i.putExtra(Intent.EXTRA_SUBJECT, "SomeRadio");
                try {
                    startActivity(Intent.createChooser(i, getString(R.string.send_email)));
                } catch (android.content.ActivityNotFoundException ex) {
                    Toast.makeText(SettingsActivity.this, R.string.no_email_client_installed, Toast.LENGTH_SHORT).show();
                }
                return true;
            }
        });
        Preference btnCopyLink = findPreference(Constants.PREFERENCES.COPY_LINK_TO_CLIPBOARD);
        btnCopyLink.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                ClipboardManager clipboard = (ClipboardManager) getApplicationContext().getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clipData = ClipData.newPlainText("", sPref.getString(Constants.PREFERENCES.LINK, getString(R.string.link_128)));
                clipboard.setPrimaryClip(clipData);
                Toast.makeText(getApplicationContext(), getString(R.string.link_was_copied), Toast.LENGTH_SHORT).show();
                return true;
            }
        });

        SimpleDateFormat format= new SimpleDateFormat("yyMMddkkmm", Locale.getDefault());
        String build = format.format(new Date(BuildConfig.TIMESTAMP));
        Preference preferenceInfo = findPreference(Constants.PREFERENCES.INFO);
        preferenceInfo.setSummary(getString(R.string.author) + "Guwenk" + "\n" + getString(R.string.version_title) + getString(R.string.version) + "\n" + getString(R.string.build) + build);
        preferenceInfo.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                if (adminCounter == 4) {
                    Intent intent = new Intent(SettingsActivity.this, AdminActivity.class);
                    startActivity(intent);
                } else adminCounter++;
                return false;
            }
        });
    }


    @Override
    protected void onStart() {
        super.onStart();
        adminCounter = 0;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent imageReturnedIntent) {
        super.onActivityResult(requestCode, resultCode, imageReturnedIntent);
        Bitmap bitmap_temp = null;

        switch (requestCode) {
            case GALLERY_REQUEST:
                if (resultCode == RESULT_OK) {
                    Uri selectedImage = imageReturnedIntent.getData();
                    try {
                        bitmap_temp = MediaStore.Images.Media.getBitmap(getContentResolver(), selectedImage);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    Bitmap bitmap = resizer(bitmap_temp);

                    String path = new FileManager(getApplicationContext()).saveBitmap(bitmap, Constants.PREFERENCES.BACKGROUND, "imageDir");
                    SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
                    SharedPreferences.Editor ed = sp.edit();
                    ed.putString(Constants.PREFERENCES.BACKGROUND_PATH, path);
                    ed.apply();
                    Toast.makeText(getApplicationContext(), R.string.background_changed, Toast.LENGTH_SHORT).show();
                }
        }
    }

    private Bitmap resizer(Bitmap original) {
        Display display = getWindowManager().getDefaultDisplay();
        int displayHeight = display.getHeight();
        int displayWidth = display.getWidth();
        int originalHeight = original.getHeight();
        int originalWidth = original.getWidth();
        float scaleH = (float) originalHeight / displayHeight;
        float scaleW = (float) originalWidth / displayWidth;
        float scale = 1;
        if (scaleH > 1 && scaleW > 1) {
            if (scaleH < scaleW) scale = scaleH;
            else scale = scaleW;
        }
        int resultHeight = (int) (originalHeight / scale);
        int resultWidth = (int) (originalWidth / scale);
        Log.i("RESIZER", "\nDisplayW: " + displayWidth
                + "\nDisplayH: " + displayHeight
                + "\nInputBitmapW: " + originalWidth
                + "\nInputBitmapH: " + originalHeight
                + "\nSCALE: " + scale
                + "\nOutputBitmapW: " + resultWidth
                + "\nOutputBitmapH: " + resultHeight);
        return Bitmap.createScaledBitmap(original, resultWidth, resultHeight, false);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (key.equals(Constants.PREFERENCES.BUFFER_SIZE)) {
            EditTextPreference et = (EditTextPreference)findPreference(key);
            int buffer_size;
            try {
                buffer_size = Integer.parseInt(sPref.getString(Constants.PREFERENCES.BUFFER_SIZE, "0"));
            } catch (NumberFormatException e){
                buffer_size = -1;
            }
            if (buffer_size < 0){
                Toast.makeText(SettingsActivity.this, R.string.wrong_value, Toast.LENGTH_SHORT).show();
            } else if (buffer_size < 1000) {
                Toast.makeText(SettingsActivity.this, R.string.min_buffer, Toast.LENGTH_SHORT).show();
                sharedPreferences.edit().putString(key, "1000").apply();
                et.setText("1000");
            } else if (buffer_size > 60000) {
                Toast.makeText(SettingsActivity.this, R.string.max_buffer, Toast.LENGTH_SHORT).show();
                sharedPreferences.edit().putString(key, "60000").apply();
                et.setText("60000");
            }
        }
    }
}
