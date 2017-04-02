package com.github.guwenk.smuradio;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;


public class MainActivity extends AppCompatActivity implements SharedPreferences.OnSharedPreferenceChangeListener {


    private static long back_pressed;
    PlayerService playerService;
    private SharedPreferences sPref;
    private ImageView backgroundImage;
    ServiceConnection serviceConnection;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        sPref = PreferenceManager.getDefaultSharedPreferences(this);
        backgroundImage = (ImageView) findViewById(R.id.main_backgroundImage);
        final Button btnToTrackOrder = (Button) findViewById(R.id.main_btnToTrackOrder);
        btnToTrackOrder.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (new InternetChecker().hasConnection(getApplicationContext())) {
                    Intent intent = new Intent(MainActivity.this, OrderActivity.class);
                    startActivity(intent);
                }
            }
        });
        final ImageButton btnPlay = (ImageButton) findViewById(R.id.main_play_button);
        btnPlay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ImageButton imgBtn = (ImageButton) findViewById(R.id.main_play_button);
                findViewById(R.id.main_status1).setVisibility(View.VISIBLE);
                imgBtn.setImageResource(R.drawable.ic_stop);
                Intent intent = new Intent(MainActivity.this, PlayerService.class);
                intent.setAction(Constants.ACTION.STARTFOREGROUND_ACTION);
                startService(intent);
                serviceConnection = new ServiceConnection() {
                    @Override
                    public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
                        playerService = ((PlayerService.MyBinder) iBinder).getService();
                        //playerService.registerClient(MainActivity.this);
                    }
                    @Override
                    public void onServiceDisconnected(ComponentName componentName) {
                        playerService = null;
                    }
                };
                bindService(intent, serviceConnection, 0);
            }
        });
        sPref.registerOnSharedPreferenceChangeListener(this);
    }


    @Override
    protected void onStart() {
        super.onStart();
        String path = sPref.getString("backgroundPath", "");
        Bitmap backgroundBitmap = null;
        if (path.equals("")) {
            backgroundImage.setImageResource(R.drawable.main_background);
        } else {
            backgroundBitmap = new FileManager(getApplicationContext()).loadBitmap(path, "background");
            backgroundImage.setImageBitmap(backgroundBitmap);
        }
        Log.i("Load_Background", backgroundBitmap + "");

        if (playerService != null) playerService.updateUI(Constants.UI.BUTTON, null);
    }


    @Override
    public void onBackPressed() {
        if (back_pressed + 2000 > System.currentTimeMillis())
            super.onBackPressed();
        else
            Toast.makeText(getBaseContext(), R.string.on_back_pressed,
                    Toast.LENGTH_SHORT).show();
        back_pressed = System.currentTimeMillis();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
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
            case R.id.action_toAdminActivity:
                intent = new Intent(MainActivity.this, AdminActivity.class);
                startActivity(intent);
                return true;
            case R.id.action_copy:
                ClipboardManager clipboard = (ClipboardManager) getApplicationContext().getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clipData = ClipData.newPlainText("", sPref.getString("link", "http://free.radioheart.ru:8000/guwenk128"));
                clipboard.setPrimaryClip(clipData);
                Toast.makeText(getApplicationContext(), getString(R.string.link_was_copied), Toast.LENGTH_SHORT).show();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String s) {
        ImageButton imageButton = (ImageButton)findViewById(R.id.main_play_button);
        int player_status = sharedPreferences.getInt(Constants.MESSAGE.PLAYER_STATUS, -1);
        String player_title = sharedPreferences.getString(Constants.MESSAGE.MUSIC_TITLE, "");
        switch (player_status){
            case 0:{
                ((TextView)findViewById(R.id.main_status1)).setText("");
                findViewById(R.id.main_status1).setVisibility(View.INVISIBLE);
                imageButton.setImageResource(R.drawable.ic_play_arrow);
                break;
            }
            case 1:{
                ((TextView)findViewById(R.id.main_status1)).setText(player_title);
                findViewById(R.id.main_status1).setVisibility(View.VISIBLE);
                imageButton.setImageResource(R.drawable.ic_stop);
                break;
            }
        }
    }
}
