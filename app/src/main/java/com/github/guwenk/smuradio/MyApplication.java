package com.github.guwenk.smuradio;

import android.app.Application;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.preference.PreferenceManager;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

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
        sendBroadcasts();
        FirebaseDatabase.getInstance().getReference().child(Constants.FIREBASE.SERVER_STATUS).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                serverStatus = dataSnapshot.getValue(Boolean.class);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
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
                sendBroadcasts();
            }
        } else if (songTitle == null && this.songTitle != null) {
            this.songTitle = null;
            sendBroadcasts();
        } else if (songTitle != null) {
            this.songTitle = songTitle;
            sendBroadcasts();
        }
    }

    private void sendBroadcasts() {
        Intent intent = new Intent(Constants.ACTION.UPDATE_ACTIVITY_ACTION);
        sendBroadcast(intent);
        intent = new Intent(Constants.ACTION.WIDGET_REFRESH_UI);
        intent.putExtra(Constants.OTHER.SONG_TITLE_INTENT, songTitle);
        sendBroadcast(intent);

        intent = new Intent(this, WidgetPlayer.class);
        intent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
        int[] ids = AppWidgetManager.getInstance(this).getAppWidgetIds(new ComponentName(this, WidgetPlayer.class));
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids);
        sendBroadcast(intent);

    }

    String loadTitle() {
        return songTitle;
    }

    boolean getServerStatus() {
        return serverStatus;
    }
}
