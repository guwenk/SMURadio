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


public class MainActivity extends AppCompatActivity {

    private static long back_pressed;
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
        ratingTV = (TextView) findViewById(R.id.main_ratingTV);

        myApplication = (MyApplication) getApplication();

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

        RatingBar ratingBar = (RatingBar) findViewById(R.id.main_RatingBar);
        ratingBar.setOnRatingBarChangeListener(new RatingBar.OnRatingBarChangeListener() {
            @Override
            public void onRatingChanged(final RatingBar ratingBar, final float rating, boolean fromUser) {
                if (fromUser) {
                    final Thread pauseRate = new Thread(new Runnable() {
                        @Override
                        public void run() {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    ratingBar.setIsIndicator(false);
                                }
                            });
                            try {
                                Thread.sleep(1500);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    ratingBar.setIsIndicator(false);
                                }
                            });

                        }
                    });
                    final String song_title = del_bad_symbols(titleString.getTitle());
                    final float user_rate_from_pref = sPref.getFloat(Constants.OTHER.USER_RATE + titleString.getTitle(), 0);
                    if (user_rate_from_pref == 0) {
                        pauseRate.start();
                        mRatingRef.child(song_title).addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(DataSnapshot dataSnapshot) {
                                rateCount = 0;
                                rateValue = 0;
                                try {
                                    rateValue = dataSnapshot.child(Constants.FIREBASE.RATE_VAL).getValue(Float.class);
                                    rateCount = dataSnapshot.child(Constants.FIREBASE.RATE_COUNT).getValue(Long.class);
                                } catch (NullPointerException ignored) {
                                }

                                rateValue += rating;
                                rateCount += 1;
                                mRatingRef.child(song_title).child(Constants.FIREBASE.RATE_VAL).setValue(rateValue);
                                mRatingRef.child(song_title).child(Constants.FIREBASE.RATE_COUNT).setValue(rateCount);
                                sPref.edit().putFloat(Constants.OTHER.USER_RATE + titleString.getTitle(), rating).apply();

                                String s = String.format("%.2f", rateValue / rateCount);
                                ratingTV.setText(!s.equals("NaN") ? s : getString(R.string.zero));
                            }

                            @Override
                            public void onCancelled(DatabaseError databaseError) {

                            }
                        });
                    } else if (rating != 0) {
                        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                        builder.setTitle(R.string.ask_update_rating)
                                .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        pauseRate.start();
                                        mRatingRef.child(song_title).addListenerForSingleValueEvent(new ValueEventListener() {
                                            @Override
                                            public void onDataChange(DataSnapshot dataSnapshot) {
                                                try {
                                                    rateValue = dataSnapshot.child(Constants.FIREBASE.RATE_VAL).getValue(Float.class);
                                                } catch (NullPointerException ignored) {
                                                }
                                                rateValue += rating - user_rate_from_pref;
                                                if (rateValue > 0) {
                                                    mRatingRef.child(song_title).child(Constants.FIREBASE.RATE_VAL).setValue(rateValue);
                                                    sPref.edit().putFloat(Constants.OTHER.USER_RATE + titleString.getTitle(), rating).apply();

                                                    String s = String.format("%.2f", rateValue / rateCount);
                                                    ratingTV.setText(!s.equals("NaN") ? s : getString(R.string.zero));
                                                } else {
                                                    sPref.edit().putFloat(Constants.OTHER.USER_RATE + titleString.getTitle(), 0).apply();
                                                    ratingBar.setRating(0);
                                                    Toast.makeText(getApplicationContext(), "Something wrong! Your rate cleared!", Toast.LENGTH_SHORT).show();
                                                }
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
                titleString.setTitle(songTitle);
            titleTV.setVisibility(View.VISIBLE);
            imageButton.setImageResource(R.drawable.ic_stop);
        } else {
            titleTV.setText("");
            titleString.setTitle("");
            titleTV.setVisibility(View.INVISIBLE);
            imageButton.setImageResource(R.drawable.ic_play_arrow);
        }
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
            if (!titleString.getTitle().equals("") && !titleString.getTitle().equals(getString(R.string.connecting)) && !titleString.getTitle().equals(getString(R.string.default_status)) && !titleString.getTitle().contains(getString(R.string.buffering))) {
                String song_title = del_bad_symbols(titleString.getTitle());
                mRatingRef.child(song_title).addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        if (!titleString.getTitle().equals("") && !titleString.getTitle().equals(getString(R.string.connecting)) && !titleString.getTitle().equals(getString(R.string.default_status))) {
                            rateCount = 0;
                            rateValue = 0;
                            try {
                                rateCount = dataSnapshot.child(Constants.FIREBASE.RATE_COUNT).getValue(Long.class);
                                rateValue = dataSnapshot.child(Constants.FIREBASE.RATE_VAL).getValue(Float.class);
                            } catch (NullPointerException ignored) {
                            }
                            String s = String.format("%.2f", rateValue / rateCount);
                            ratingTV.setText(!s.equals("NaN") ? s : getString(R.string.zero));
                            ratingBar.setVisibility(View.VISIBLE);
                            ratingTV.setVisibility(View.VISIBLE);
                            float user_rate_from_pref = sPref.getFloat(Constants.OTHER.USER_RATE + getTitle(), 0);
                            if (user_rate_from_pref != 0) {
                                if (rateValue != 0 && rateCount != 0)
                                    ratingBar.setRating(user_rate_from_pref);
                                else {
                                    sPref.edit().putFloat(Constants.OTHER.USER_RATE + getTitle(), 0).apply();
                                    ratingBar.setRating(0);
                                }
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
