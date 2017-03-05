package com.github.guwenk.smuradio;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.widget.RemoteViews;


public class NotificationService extends Service{
    //private String LOG_TAG = "NotificationService";
    protected RemoteViews views;
    private MyBinder binder = new MyBinder();
    private MainActivity activity;


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent.getAction().equals(Constants.ACTION.STARTFOREGROUND_ACTION)) {
            showNotification();
        } else if (intent.getAction().equals(Constants.ACTION.PLAY_ACTION)) {
            activity.doPlayPause(false);
        } else if (intent.getAction().equals(Constants.ACTION.STOPFOREGROUND_ACTION)) {
            activity.killPlayer();
            activity.changeRadioStatus();
            stopForeground(true);
            stopSelf();
        }
        return START_STICKY;
    }

    Notification status;
    private void showNotification() {
        views = new RemoteViews(getPackageName(),
                R.layout.notification_layout);

        Intent playIntent = new Intent(this, NotificationService.class);
        playIntent.setAction(Constants.ACTION.PLAY_ACTION);
        PendingIntent pplayIntent = PendingIntent.getService(this, 0,
                playIntent, 0);


        Intent closeIntent = new Intent(this, NotificationService.class);
        closeIntent.setAction(Constants.ACTION.STOPFOREGROUND_ACTION);
        PendingIntent pcloseIntent = PendingIntent.getService(this, 0,
                closeIntent, 0);

        views.setOnClickPendingIntent(R.id.status_bar_play, pplayIntent);

        views.setOnClickPendingIntent(R.id.status_bar_collapse, pcloseIntent);

        views.setImageViewResource(R.id.status_bar_play, R.drawable.ic_stop);
        views.setInt(R.id.small_notification_bg, "setBackgroundResource", R.color.notificationBackground);

        views.setTextViewText(R.id.status_bar_track_name, getString(R.string.app_name));

        status = new Notification.Builder(this).build();
        status.contentView = views;
        status.flags = Notification.FLAG_ONGOING_EVENT;
        status.icon = R.drawable.ic_radio_24dp;
        //status.contentIntent = pendingIntent;
        startForeground(Constants.NOTIFICATION_ID.FOREGROUND_SERVICE, status);
    }

    public void toPlayButton(){
        views.setImageViewResource(R.id.status_bar_play, R.drawable.ic_play_arrow);
        startForeground(Constants.NOTIFICATION_ID.FOREGROUND_SERVICE, status);
    }
    public void toStopButton(){
        views.setImageViewResource(R.id.status_bar_play, R.drawable.ic_stop);
        startForeground(Constants.NOTIFICATION_ID.FOREGROUND_SERVICE, status);
    }
    public void refreshTitle(String title){
        views.setTextViewText(R.id.status_bar_track_name, title);
        startForeground(Constants.NOTIFICATION_ID.FOREGROUND_SERVICE, status);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        stopSelf();
        return super.onUnbind(intent);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    public void registerClient(MainActivity activity){
        this.activity = activity;
    }
    class MyBinder extends Binder{
        NotificationService getService(){
            return NotificationService.this;
        }
    }
}
