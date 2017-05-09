package com.github.guwenk.smuradio;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;

public class WidgetPlayer extends AppWidgetProvider {
    int[] appWidgetIds;
    AppWidgetManager appWidgetManager;
    private RemoteViews remoteViews;

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        super.onUpdate(context, appWidgetManager, appWidgetIds);
        remoteViews = new RemoteViews(context.getPackageName(), R.layout.widget_layout);
        this.appWidgetIds = appWidgetIds;
        this.appWidgetManager = appWidgetManager;

        Intent btnPlayIntent = new Intent(context, WidgetPlayer.class);
        btnPlayIntent.setAction(Constants.ACTION.WIDGET_BUTTON_PLAY);
        PendingIntent btnPlayPI = PendingIntent.getBroadcast(context, 0, btnPlayIntent, 0);
        remoteViews.setOnClickPendingIntent(R.id.widget_status_bar_play, btnPlayPI);

        Intent toMainActivityIntent = new Intent(context, MainActivity.class);
        toMainActivityIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        PendingIntent toMainActivityPI = PendingIntent.getActivity(context, 0, toMainActivityIntent, 0);
        remoteViews.setOnClickPendingIntent(R.id.widget_layout, toMainActivityPI);

        MyApplication myApplication = (MyApplication) context.getApplicationContext();
        String title = myApplication.loadTitle();
        if (title != null) {
            remoteViews.setTextViewText(R.id.widget_status_bar_track_name, title);
            remoteViews.setImageViewResource(R.id.widget_status_bar_play, R.drawable.ic_pause_circle_outline_24px);
        } else {
            remoteViews.setTextViewText(R.id.widget_status_bar_track_name, context.getString(R.string.default_status));
            remoteViews.setImageViewResource(R.id.widget_status_bar_play, R.drawable.ic_play_circle_outline_24px);
        }
        appWidgetManager.updateAppWidget(appWidgetIds, remoteViews);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        String action = intent.getAction();
        switch (action) {
            case Constants.ACTION.WIDGET_BUTTON_PLAY: {
                Intent playIntent = new Intent(context, PlayerService.class);
                playIntent.setAction(Constants.ACTION.STARTFOREGROUND_ACTION);
                context.startService(playIntent);
                break;
            }
            case Constants.ACTION.WIDGET_REFRESH_UI: {
                remoteViews = new RemoteViews(context.getPackageName(), R.layout.widget_layout);
                String title = intent.getStringExtra(Constants.OTHER.SONG_TITLE_INTENT);
                if (title != null) {
                    remoteViews.setTextViewText(R.id.widget_status_bar_track_name, title);
                    remoteViews.setImageViewResource(R.id.widget_status_bar_play, R.drawable.ic_stop);
                } else {
                    remoteViews.setTextViewText(R.id.widget_status_bar_track_name, context.getString(R.string.default_status));
                    remoteViews.setImageViewResource(R.id.widget_status_bar_play, R.drawable.ic_play_circle_outline_24px);
                }
                if (appWidgetManager != null && appWidgetIds != null) {
                    appWidgetManager.updateAppWidget(appWidgetIds, remoteViews);
                }
                break;
            }
        }
    }
}
