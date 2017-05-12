package com.github.guwenk.smuradio;

import android.app.Application;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.preference.PreferenceManager;
import android.util.DisplayMetrics;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.Locale;


public class MyApplication extends Application {
    private String songTitle;
    private boolean serverStatus = true;

    @Override
    public void onCreate() {
        super.onCreate();
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);

        String lang = preferences.getString(Constants.PREFERENCES.LANGUAGE, "default");
        String sys_lang = getResources().getConfiguration().locale.getCountry();
        if (!preferences.getString(Constants.PREFERENCES.SYSTEM_LANGUAGE, "").equals(sys_lang))
            preferences.edit().putString(Constants.PREFERENCES.SYSTEM_LANGUAGE, sys_lang).apply();
        if (lang.equals("default")) {
            lang = sys_lang;
        }
        Resources res = getResources();
        DisplayMetrics dm = res.getDisplayMetrics();
        android.content.res.Configuration conf = res.getConfiguration();
        conf.locale = new Locale(lang.toLowerCase());
        res.updateConfiguration(conf, dm);

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
        new Thread(new Runnable() {
            @Override
            public void run() {
                send();
                try {
                    Thread.sleep(100);
                    send();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            void send(){
                Intent intent = new Intent(Constants.ACTION.UPDATE_ACTIVITY_ACTION);
                sendBroadcast(intent);
                intent = new Intent(Constants.ACTION.WIDGET_REFRESH_UI);
                intent.putExtra(Constants.OTHER.SONG_TITLE_INTENT, songTitle);
                sendBroadcast(intent);

                intent = new Intent(MyApplication.this, WidgetPlayer.class);
                intent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
                int[] ids = AppWidgetManager.getInstance(MyApplication.this).getAppWidgetIds(new ComponentName(MyApplication.this, WidgetPlayer.class));
                intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids);
                sendBroadcast(intent);
            }
        }).start();
    }

    String loadTitle() {
        return songTitle;
    }

    boolean getServerStatus() {
        return serverStatus;
    }
}
