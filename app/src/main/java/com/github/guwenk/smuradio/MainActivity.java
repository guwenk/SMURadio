package com.github.guwenk.smuradio;

import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;


public class MainActivity extends AppCompatActivity {

    protected PlayerService playerService;
    protected ServiceConnection serviceConnection;
    private TitleString titleString;
    private DatabaseReference mRootRef = FirebaseDatabase.getInstance().getReference();
    private SharedPreferences sPref;
    private ImageView backgroundImage;
    private TextView titleTV;
    private MyApplication myApplication;
    private BroadcastReceiver broadcastReceiver;
    private Toast toastInternetConnection;
    private Toast toastOrderWait;

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        unbindService(serviceConnection);
        unregisterReceiver(broadcastReceiver);
        super.onDestroy();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        PreferenceManager.setDefaultValues(this, R.xml.settings, false);
        sPref = PreferenceManager.getDefaultSharedPreferences(this);
        backgroundImage = (ImageView) findViewById(R.id.main_backgroundImage);
        titleTV = (TextView) findViewById(R.id.main_status1);
        titleString = new TitleString();

        myApplication = (MyApplication) getApplication();

        if (!sPref.getBoolean(Constants.PREFERENCES.LIC_AGREEMENT, false)) {
            AlertDialog.Builder licBuilder = new AlertDialog.Builder(this);
            licBuilder.setView(R.layout.dialog_license)
                    .setPositiveButton(R.string.accept, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            sPref.edit().putBoolean(Constants.PREFERENCES.LIC_AGREEMENT, true).apply();
                            dialog.dismiss();
                        }
                    })
                    .setNegativeButton(R.string.decline, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            Toast.makeText(MainActivity.this, getString(R.string.toast_decline_lic_agreement), Toast.LENGTH_SHORT).show();
                            finishAffinity();
                        }
                    })
                    .setCancelable(false);
            AlertDialog licDialog = licBuilder.create();
            licDialog.show();
        }

        final Button btnToTrackOrder = (Button) findViewById(R.id.main_btnToTrackOrder);
        btnToTrackOrder.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (new InternetChecker().hasConnection(getApplicationContext())) {
                    long order_freeze = sPref.getLong(Constants.OTHER.ORDER_FREEZE, 0);
                    long current_time = System.currentTimeMillis();
                    if (current_time >= order_freeze || order_freeze == 0) {
                        Intent intent = new Intent(MainActivity.this, OrderActivity.class);
                        startActivity(intent);
                    } else {
                        int seconds_left = (int) ((order_freeze - current_time) / 1000);
                        if (toastOrderWait != null) toastOrderWait.cancel();
                        toastOrderWait = Toast.makeText(getApplicationContext(), getString(R.string.order_freeze_msg_one) + seconds_left + getString(R.string.order_freeze_msg_two), Toast.LENGTH_SHORT);
                        toastOrderWait.show();
                    }
                } else {
                    if (toastInternetConnection != null)
                        toastInternetConnection.cancel();
                    toastInternetConnection = Toast.makeText(getApplicationContext(), getString(R.string.check_internet_connection), Toast.LENGTH_SHORT);
                    toastInternetConnection.show();
                }

            }
        });
        titleTV.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                ClipboardManager clipboard = (ClipboardManager) getApplicationContext().getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clipData = ClipData.newPlainText("", titleTV.getText());
                clipboard.setPrimaryClip(clipData);
                Toast.makeText(getApplicationContext(), R.string.name_copied, Toast.LENGTH_SHORT).show();
                return true;
            }
        });

        Intent intent = new Intent(MainActivity.this, PlayerService.class);
        startService(intent);
        serviceConnection = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
                playerService = ((PlayerService.MyBinder) iBinder).getService();
            }

            @Override
            public void onServiceDisconnected(ComponentName componentName) {
                playerService = null;
            }
        };
        bindService(intent, serviceConnection, 0);

        final ImageButton btnPlay = (ImageButton) findViewById(R.id.main_play_button);
        btnPlay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, PlayerService.class);
                intent.setAction(Constants.ACTION.STARTFOREGROUND_ACTION);
                startService(intent);
            }
        });

        broadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                updateActivity();
            }
        };
        IntentFilter intentFilter = new IntentFilter(Constants.ACTION.UPDATE_ACTIVITY_ACTION);
        registerReceiver(broadcastReceiver, intentFilter);
    }

    @Override
    protected void onStart() {
        super.onStart();

        updateActivity();

        String path = sPref.getString(Constants.PREFERENCES.BACKGROUND_PATH, "");
        Bitmap backgroundBitmap;
        if (path.equals("")) {
            backgroundImage.setImageResource(R.drawable.main_background);
        } else {
            backgroundBitmap = new FileManager(getApplicationContext()).loadBitmap(path, Constants.PREFERENCES.BACKGROUND);
            backgroundImage.setImageBitmap(backgroundBitmap);
        }

        Intent intent = new Intent(MainActivity.this, PlayerService.class);
        bindService(intent, serviceConnection, 0);
    }

    void updateActivity() {
        ImageButton imageButton = (ImageButton) findViewById(R.id.main_play_button);
        String songTitle = myApplication.loadTitle();
        if (songTitle != null) {
            titleTV.setText(songTitle);
            if (!titleString.getTitle().equals(songTitle))
                //titleString.setTitle(songTitle);
                titleTV.setVisibility(View.VISIBLE);
            imageButton.setImageResource(R.drawable.ic_stop);
        } else {
            titleTV.setText("");
            titleTV.setVisibility(View.INVISIBLE);
            imageButton.setImageResource(R.drawable.ic_play_arrow);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        try {
            if (!getString(R.string.dev_id).equals(Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID)))
                getMenuInflater().inflate(R.menu.menu_main, menu);
            else {
                getMenuInflater().inflate(R.menu.menu_main_dev, menu);
            }
        } catch (NullPointerException ignored) {
            getMenuInflater().inflate(R.menu.menu_main, menu);
        }
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Intent intent;
        int id = item.getItemId();
        switch (id) {
            case R.id.action_settings:
                intent = new Intent(MainActivity.this, SettingsActivity.class);
                startActivity(intent);
                return true;
            case R.id.telegram_bot:
                intent = new Intent(Intent.ACTION_VIEW, Uri.parse("http://t.me/SomeRadio_bot"));
                startActivity(intent);
                return true;
            case R.id.copy_link_to_clipboard:
                ClipboardManager clipboard = (ClipboardManager) getApplicationContext().getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clipData = ClipData.newPlainText("", sPref.getString(Constants.PREFERENCES.LINK, getString(R.string.link_128)));
                clipboard.setPrimaryClip(clipData);
                Toast.makeText(getApplicationContext(), getString(R.string.link_was_copied), Toast.LENGTH_SHORT).show();
                return true;
            case R.id.admin_menu:
                intent = new Intent(MainActivity.this, AdminActivity.class);
                startActivity(intent);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private String del_bad_symbols(String s) {
        char[] chars = s.toCharArray();
        for (int i = 0; i < chars.length; i++) {
            if (chars[i] == '.' || chars[i] == '#' || chars[i] == '$' || chars[i] == '[' || chars[i] == ']') {
                chars[i] = ' ';
            }
        }
        return String.valueOf(chars);
    }

    private class TitleString {
        private String title;

        TitleString() {
            title = "";
        }

        public String getTitle() {
            return title;
        }
    }
}
