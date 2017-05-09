package com.github.guwenk.smuradio;

import android.app.Application;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.preference.PreferenceManager;

import java.util.Locale;


public class MyApplication extends Application {
    private Locale locale;
    private String lang;
    private String songTitle;
    private boolean serverStatus = true;

    @Override
    public void onCreate() {
        super.onCreate();
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        lang = preferences.getString(Constants.PREFERENCES.LANGUAGE, "default");
        if (lang.equals("default")) {
            lang = getResources().getConfiguration().locale.getCountry();
        }
        locale = new Locale(lang);
        Locale.setDefault(locale);
        Configuration config = new Configuration();
        config.locale = locale;
        getBaseContext().getResources().updateConfiguration(config, null);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        locale = new Locale(lang);
        Locale.setDefault(locale);
        Configuration config = new Configuration();
        config.locale = locale;
        getBaseContext().getResources().updateConfiguration(config, null);
    }

    void saveTitle(String songTitle) {
        if (songTitle != null && this.songTitle != null) {
            if (!this.songTitle.equals(songTitle)) {
                this.songTitle = songTitle;
                Intent intent = new Intent(Constants.ACTION.UPDATE_ACTIVITY_ACTION);
                sendBroadcast(intent);
            }
        } else if (songTitle == null && this.songTitle != null) {
            this.songTitle = null;
            Intent intent = new Intent(Constants.ACTION.UPDATE_ACTIVITY_ACTION);
            sendBroadcast(intent);
        } else if (songTitle != null) {
            this.songTitle = songTitle;
            Intent intent = new Intent(Constants.ACTION.UPDATE_ACTIVITY_ACTION);
            sendBroadcast(intent);
        }

    }

    String loadTitle() {
        return songTitle;
    }

    void saveServerStatus(boolean b) {
        serverStatus = b;
    }

    boolean loadServerStatus() {
        return serverStatus;
    }
}
