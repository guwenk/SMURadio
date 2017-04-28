package com.github.guwenk.smuradio;

import android.app.Dialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;


public class MainActivity extends AppCompatActivity implements SharedPreferences.OnSharedPreferenceChangeListener {

    private static long back_pressed;
    final String FBDB_RATE_VAL = "rate";
    final String FBDB_RATE_COUNT = "count";
    protected PlayerService playerService;
    protected ServiceConnection serviceConnection;
    protected float rateValue;
    protected long rateCount;
    private TitleString titleString;
    private DatabaseReference mRootRef = FirebaseDatabase.getInstance().getReference();
    private DatabaseReference mRatingRef = mRootRef.child("Rating");
    private SharedPreferences sPref;
    private ImageView backgroundImage;
    private TextView titleTV;
    private TextView ratingTV;
    private Dialog infoDialog;

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        unbindService(serviceConnection);
        super.onDestroy();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        sPref = PreferenceManager.getDefaultSharedPreferences(this);
        backgroundImage = (ImageView) findViewById(R.id.main_backgroundImage);
        titleTV = (TextView) findViewById(R.id.main_status1);
        titleString = new TitleString();
        ratingTV = (TextView) findViewById(R.id.main_ratingTV);

        infoDialog = new Dialog(MainActivity.this);
        infoDialog.setContentView(R.layout.info_layout);
        TextView infoTV = (TextView) infoDialog.findViewById(R.id.info_TextView);
        infoTV.setText("SomeRadio\nAuthor: Guwenk\nVersion: 0.9.1");

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
                        Toast.makeText(getApplicationContext(), getString(R.string.order_freeze_msg_one) + seconds_left + getString(R.string.order_freeze_msg_two), Toast.LENGTH_SHORT).show();
                    }
                } else
                    Toast.makeText(getApplicationContext(), getString(R.string.check_internet_connection), Toast.LENGTH_SHORT).show();
            }
        });
        titleTV.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                ClipboardManager clipboard = (ClipboardManager) getApplicationContext().getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clipData = ClipData.newPlainText("", titleTV.getText());
                clipboard.setPrimaryClip(clipData);
                Toast.makeText(getApplicationContext(), R.string.name_copied, Toast.LENGTH_SHORT).show();
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
        sPref.registerOnSharedPreferenceChangeListener(this);

        RatingBar ratingBar = (RatingBar) findViewById(R.id.main_RatingBar);
        ratingBar.setOnRatingBarChangeListener(new RatingBar.OnRatingBarChangeListener() {
            @Override
            public void onRatingChanged(final RatingBar ratingBar, final float rating, boolean fromUser) {
                if (fromUser) {
                    final String song_title = del_bad_symbols(titleString.getTitle());
                    final float user_rate_from_pref = sPref.getFloat(Constants.OTHER.USER_RATE + titleString.getTitle(), 0);
                    if (user_rate_from_pref == 0) {
                        mRatingRef.child(song_title).addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(DataSnapshot dataSnapshot) {
                                rateCount = 0;
                                rateValue = 0;
                                try {
                                    rateCount = dataSnapshot.child(FBDB_RATE_COUNT).getValue(Long.class);
                                    rateValue = dataSnapshot.child(FBDB_RATE_VAL).getValue(Float.class);
                                } catch (NullPointerException ignored) {
                                }

                                rateValue += rating;
                                rateCount += 1;
                                mRatingRef.child(song_title).child(FBDB_RATE_VAL).setValue(rateValue);
                                mRatingRef.child(song_title).child(FBDB_RATE_COUNT).setValue(rateCount);
                                sPref.edit().putFloat(Constants.OTHER.USER_RATE + titleString.getTitle(), rating).apply();

                                String s = String.format("%.2f", rateValue / rateCount);
                                ratingTV.setText(!s.equals("NaN") ? s : getString(R.string.zero));
                            }

                            @Override
                            public void onCancelled(DatabaseError databaseError) {

                            }
                        });
                    } else if (rating != 0){
                        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                        builder.setTitle(R.string.ask_update_rating)
                                .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        mRatingRef.child(song_title).addListenerForSingleValueEvent(new ValueEventListener() {
                                            @Override
                                            public void onDataChange(DataSnapshot dataSnapshot) {
                                                try {
                                                    rateValue = dataSnapshot.child(FBDB_RATE_VAL).getValue(Float.class);
                                                } catch (NullPointerException ignored) {
                                                }
                                                rateValue += rating - user_rate_from_pref;
                                                mRatingRef.child(song_title).child(FBDB_RATE_VAL).setValue(rateValue);
                                                sPref.edit().putFloat(Constants.OTHER.USER_RATE + titleString.getTitle(), rating).apply();

                                                String s = String.format("%.2f", rateValue / rateCount);
                                                ratingTV.setText(!s.equals("NaN") ? s : getString(R.string.zero));
                                            }

                                            @Override
                                            public void onCancelled(DatabaseError databaseError) {
                                            }
                                        });
                                    }
                                })
                                .setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        ratingBar.setRating(user_rate_from_pref);
                                    }
                                })
                                .setCancelable(true)
                                .setOnCancelListener(new DialogInterface.OnCancelListener() {
                                    @Override
                                    public void onCancel(DialogInterface dialog) {
                                        ratingBar.setRating(user_rate_from_pref);
                                    }
                                });
                        AlertDialog alertDialog = builder.create();
                        alertDialog.show();
                    } else ratingBar.setRating(user_rate_from_pref);
                }
            }
        });
    }


    @Override
    protected void onStart() {
        super.onStart();
        onSharedPreferenceChanged(sPref, Constants.MESSAGE.PLAYER_STATUS);
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
                ClipData clipData = ClipData.newPlainText("", sPref.getString(Constants.PREFERENCES.LINK, getString(R.string.link_128)));
                clipboard.setPrimaryClip(clipData);
                Toast.makeText(getApplicationContext(), getString(R.string.link_was_copied), Toast.LENGTH_SHORT).show();
                return true;
            case R.id.action_info:
                infoDialog.show();
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        switch (key) {
            case Constants.MESSAGE.MUSIC_TITLE:
            case Constants.MESSAGE.PLAYER_STATUS: {
                ImageButton imageButton = (ImageButton) findViewById(R.id.main_play_button);
                int player_status = sharedPreferences.getInt(Constants.MESSAGE.PLAYER_STATUS, -1);
                String player_title = sharedPreferences.getString(Constants.MESSAGE.MUSIC_TITLE, "");
                switch (player_status) {
                    case 0: {
                        titleTV.setText("");
                        titleString.setTitle("");
                        findViewById(R.id.main_status1).setVisibility(View.INVISIBLE);
                        imageButton.setImageResource(R.drawable.ic_play_arrow);
                        break;
                    }
                    case 1: {
                        titleTV.setText(player_title);
                        if (!titleString.getTitle().equals(player_title))
                            titleString.setTitle(player_title);
                        findViewById(R.id.main_status1).setVisibility(View.VISIBLE);
                        imageButton.setImageResource(R.drawable.ic_stop);
                        break;
                    }
                }
                break;
            }
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
        private RatingBar ratingBar = (RatingBar) findViewById(R.id.main_RatingBar);

        TitleString() {
            title = "";
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;

            ratingBar.setRating((float) 0);
            if (!titleString.getTitle().equals("") && !titleString.getTitle().equals(getString(R.string.connecting)) && !titleString.getTitle().equals(getString(R.string.default_status))) {
                String song_title = del_bad_symbols(titleString.getTitle());
                mRatingRef.child(song_title).addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        if (!titleString.getTitle().equals("") && !titleString.getTitle().equals(getString(R.string.connecting)) && !titleString.getTitle().equals(getString(R.string.default_status))) {
                            rateCount = 0;
                            rateValue = 0;
                            try {
                                rateCount = dataSnapshot.child(FBDB_RATE_COUNT).getValue(Long.class);
                                rateValue = dataSnapshot.child(FBDB_RATE_VAL).getValue(Float.class);
                            } catch (NullPointerException ignored) {
                            }
                            String s = String.format("%.2f", rateValue / rateCount);
                            ratingTV.setText(!s.equals("NaN") ? s : getString(R.string.zero));
                            ratingBar.setVisibility(View.VISIBLE);
                            ratingTV.setVisibility(View.VISIBLE);
                            float user_rate_from_pref = sPref.getFloat(Constants.OTHER.USER_RATE + getTitle(), 0);
                            if (user_rate_from_pref != 0) {
                                ratingBar.setRating(user_rate_from_pref);
                            }
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {

                    }
                });
            } else {
                ratingTV.setText("");
                ratingTV.setVisibility(View.INVISIBLE);
                ratingBar.setVisibility(View.INVISIBLE);
            }
        }
    }
}
