package com.github.guwenk.smuradio;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.preference.SwitchPreference;
import android.provider.MediaStore;
import android.support.v7.app.AlertDialog;
import android.text.Html;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.Menu;
import android.widget.Toast;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class SettingsActivity extends PreferenceActivity implements SharedPreferences.OnSharedPreferenceChangeListener {
    static final int GALLERY_REQUEST = 1;
    private SharedPreferences sPref;
    private boolean isRestore = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.settings);

        sPref = PreferenceManager.getDefaultSharedPreferences(this);
        sPref.registerOnSharedPreferenceChangeListener(this);

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            SwitchPreference switchPreference = (SwitchPreference) findPreference(Constants.PREFERENCES.HEADSET_BUTTON);
            sPref.edit().putBoolean(Constants.PREFERENCES.HEADSET_BUTTON, false).apply();
            switchPreference.setChecked(false);
            switchPreference.setEnabled(false);
            switchPreference.setSummary(R.string.unavailable_below_5_0);
        }

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
                i.putExtra(Intent.EXTRA_EMAIL, new String[]{getString(R.string.email)});
                i.putExtra(Intent.EXTRA_SUBJECT, "SomeRadio");
                try {
                    startActivity(Intent.createChooser(i, getString(R.string.send_email)));
                } catch (android.content.ActivityNotFoundException ex) {
                    Toast.makeText(SettingsActivity.this, R.string.no_email_client_installed, Toast.LENGTH_SHORT).show();
                }
                return true;
            }
        });

        Preference btnToPlayMarket = findPreference(Constants.PREFERENCES.CHECK_FOR_UPDATES);
        btnToPlayMarket.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                final String appPackageName = getPackageName();
                try {
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + appPackageName)));
                } catch (android.content.ActivityNotFoundException anfe) {
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=" + appPackageName)));
                }
                return true;
            }
        });

        Preference btnRestoreSettings = findPreference(Constants.PREFERENCES.RESTORE_SETTINGS);
        btnRestoreSettings.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                AlertDialog.Builder builder = new AlertDialog.Builder(SettingsActivity.this);
                builder.setTitle(R.string.warning)
                        .setMessage(R.string.settings_will_be_restored)
                        .setCancelable(false)
                        .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                isRestore = true;
                                sPref.edit()
                                        .putString(Constants.PREFERENCES.BUFFER_SIZE, getString(R.string.default_buffer))
                                        .putBoolean(Constants.PREFERENCES.HEADSET_BUTTON, Boolean.parseBoolean(getString(R.string.default_headset_reconnect)))
                                        .putBoolean(Constants.PREFERENCES.RECONNECT, Boolean.parseBoolean(getString(R.string.default_autoreconnect)))
                                        .putBoolean(Constants.PREFERENCES.CHECK_UPDATES_AT_STARTUP, Boolean.parseBoolean(getString(R.string.default_check_updates_at_startup)))
                                        .putString(Constants.PREFERENCES.BACKGROUND_PATH, "")
                                        .putString(Constants.PREFERENCES.LINK, getString(R.string.link_128))
                                        .putString(Constants.PREFERENCES.LANGUAGE, getString(R.string.default_lang))
                                        .apply();
                                try {
                                    finishAffinity();
                                } catch (NullPointerException ignored) {
                                }
                                Intent i = getBaseContext().getPackageManager().getLaunchIntentForPackage(getBaseContext().getPackageName());
                                i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                                startActivity(i);
                            }
                        })
                        .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.cancel();
                            }
                        });
                AlertDialog dialog = builder.create();
                dialog.show();
                return true;
            }
        });

        Preference preferenceInfo = findPreference(Constants.PREFERENCES.INFO);
        preferenceInfo.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                SimpleDateFormat format = new SimpleDateFormat("dd.MM.yyyy", Locale.ENGLISH);
                String build_time = format.format(new Date(BuildConfig.TIMESTAMP));
                AlertDialog.Builder builder = new AlertDialog.Builder(SettingsActivity.this);
                builder.setTitle(Html.fromHtml("<u>" + getString(R.string.about) + "</u>"))
                        .setMessage(Html.fromHtml("<i><b><u>v" + BuildConfig.VERSION_NAME + ", Build " + BuildConfig.VERSION_CODE + " (" + build_time + ")</u></b></i><br>" + getString(R.string.programming) + "<b>Guwenk</b>" + "<br>" + getString(R.string.design) + "<b>Stronger197</b>" + "<br><br>" + getString(R.string.special_thanks) + "<br>&emsp;• Aliksmen"))
                        .setCancelable(true);
                AlertDialog alertDialog = builder.create();
                alertDialog.show();
                return false;
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    protected void onStart() {
        super.onStart();
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

    void localize() {
        String lang = sPref.getString(Constants.PREFERENCES.LANGUAGE, "default");
        if (lang.equals("default")) {
            lang = sPref.getString(Constants.PREFERENCES.SYSTEM_LANGUAGE, getResources().getConfiguration().locale.getCountry());
        }
        Resources res = getResources();
        DisplayMetrics dm = res.getDisplayMetrics();
        android.content.res.Configuration conf = res.getConfiguration();
        conf.locale = new Locale(lang.toLowerCase());
        res.updateConfiguration(conf, dm);
        if (!isRestore) {
            try {
                finishAffinity();
            } catch (NullPointerException ignored) {
            }
            Intent i = getBaseContext().getPackageManager().getLaunchIntentForPackage(getBaseContext().getPackageName());
            i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(i);
        }
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (!isRestore) {
            if (key.equals(Constants.PREFERENCES.BUFFER_SIZE)) {
                EditTextPreference et = (EditTextPreference) findPreference(key);
                int buffer_size;
                try {
                    buffer_size = Integer.parseInt(sPref.getString(Constants.PREFERENCES.BUFFER_SIZE, "0"));
                } catch (NumberFormatException e) {
                    buffer_size = -1;
                }
                if (buffer_size < 0) {
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
        if (key.equals(Constants.PREFERENCES.LANGUAGE)) {
            localize();
        }
    }
}
