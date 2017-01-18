package com.github.guwenk.smuradio;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.un4seen.bass.BASS;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;


public class MainActivity extends AppCompatActivity {

    ProgressDialog pDialog;

    AudioManager am;
    String radioUrl = "http://free.radioheart.ru:8000/guwenk";
    String bitrate;

    int req; // request number/counter
    int chan; // stream handle

    static final int BASS_SYNC_HLS_SEGMENT = 0x10300;
    static final int BASS_TAG_HLS_EXTINF = 0x14000;

    Handler handler=new Handler();
    Runnable timer;
    final Object lock = new Object();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        am = (AudioManager) getSystemService(AUDIO_SERVICE);

        final ToggleButton toggleButton = (ToggleButton) findViewById(R.id.toggleButton);
        toggleButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (toggleButton.isChecked()) {
                    if (new InternetChecker().hasConnection(getApplicationContext())) {
                        if (radioUrl != null) {
                            if (!BASS.BASS_Init(-1, 44100, 0)) {
                                Error("Can't initialize device");
                                return;
                            }
                            BASS.BASS_SetConfig(BASS.BASS_CONFIG_NET_PLAYLIST, 1); // enable playlist processing
                            BASS.BASS_SetConfig(BASS.BASS_CONFIG_NET_PREBUF, 0); // minimize automatic pre-buffering, so we can do it (and display it) instead

                            // load AAC and HLS add-ons (if present)
                            BASS.BASS_PluginLoad("libbass_aac.so", 0);
                            BASS.BASS_PluginLoad("libbasshls.so", 0);
                            pDialog = new ProgressDialog(MainActivity.this);
                            pDialog.setTitle("Loading");
                            pDialog.setMessage("");
                            pDialog.setIndeterminate(false);
                            pDialog.show();
                            new Thread(new MainActivity.OpenURL(radioUrl + bitrate)).start();
                        } else {
                            Toast.makeText(getApplicationContext(), getString(R.string.error) + " ToggleButton, MediaPlayer, Bitrate", Toast.LENGTH_SHORT).show();
                            toggleButton.setChecked(false);
                        }
                    } else {
                        toggleButton.setChecked(false);
                    }
                } else {
                    BASS.BASS_Free();
                    ((TextView) findViewById(R.id.status1)).setText("");
                    findViewById(R.id.status1).setVisibility(View.INVISIBLE);
                }
            }
        });

        final Button btnToTrackOrder = (Button) findViewById(R.id.btnToTrackOrder);
        btnToTrackOrder.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (new InternetChecker().hasConnection(getApplicationContext())) {
                    Intent intent = new Intent(MainActivity.this, VoteActivity.class);
                    startActivity(intent);
                }
            }
        });

        timer = new Runnable() {
            public void run() {
                // monitor prebuffering progress
                int progress = (int) BASS.BASS_StreamGetFilePosition(chan, BASS.BASS_FILEPOS_BUFFER);
                if (progress < 0) return; // failed, eg. stream freed
                progress = progress * 100 / (int) BASS.BASS_StreamGetFilePosition(chan, BASS.BASS_FILEPOS_END); // percentage of buffer filled
                if (progress > 75 || BASS.BASS_StreamGetFilePosition(chan, BASS.BASS_FILEPOS_CONNECTED) == 0) { // over 75% full (or end of download)
                    pDialog.dismiss();
                    findViewById(R.id.status1).setVisibility(View.VISIBLE);
                    DoMeta();
                    BASS.BASS_ChannelSetSync(chan, BASS.BASS_SYNC_META, 0, MetaSync, 0); // Shoutcast
                    BASS.BASS_ChannelSetSync(chan, BASS.BASS_SYNC_OGG_CHANGE, 0, MetaSync, 0); // Icecast/OGG
                    BASS.BASS_ChannelSetSync(chan, BASS_SYNC_HLS_SEGMENT, 0, MetaSync, 0); // HLS
                    // set sync for end of stream
                    BASS.BASS_ChannelSetSync(chan, BASS.BASS_SYNC_END, 0, EndSync, 0);
                    // play it!
                    BASS.BASS_ChannelPlay(chan, false);
                } else {
                    pDialog.setMessage("Buffering... (" + progress +")");
                    //((TextView) findViewById(R.id.status1)).setText(String.format("buffering... %d%%", progress));
                    handler.postDelayed(this, 50);
                }
            }
        };
    }

    @Override
    protected void onStart() {
        super.onStart();
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
        bitrate = sp.getString("bitrate", "128");
    }
    @Override
    protected void onResume() {
        super.onResume();
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
        bitrate = sp.getString("bitrate", "128");
    }

    class RunnableParam implements Runnable {
        Object param;
        RunnableParam(Object p) { param=p; }
        public void run() {}
    }

    void Error(String es) {
        // get error code in current thread for display in UI thread
        @SuppressLint("DefaultLocale") String s=String.format("%s\n(error code: %d)", es, BASS.BASS_ErrorGetCode());
        runOnUiThread(new MainActivity.RunnableParam(s) {
            public void run() {
                new AlertDialog.Builder(MainActivity.this)
                        .setMessage((String)param)
                        .setPositiveButton("OK", null)
                        .show();
            }
        });
    }

    void DoMeta() {
        String meta=(String)BASS.BASS_ChannelGetTags(chan, BASS.BASS_TAG_META);
        if (meta!=null) { // got Shoutcast metadata
            int ti=meta.indexOf("StreamTitle='");
            if (ti>=0) {
                String title=meta.substring(ti+13, meta.indexOf("';", ti+13));
                ((TextView)findViewById(R.id.status1)).setText(title);
            }
        } else {
            String[] ogg=(String[])BASS.BASS_ChannelGetTags(chan, BASS.BASS_TAG_OGG);
            if (ogg!=null) { // got Icecast/OGG tags
                String artist=null, title=null;
                for (String s: ogg) {
                    if (s.regionMatches(true, 0, "artist=", 0, 7))
                        artist=s.substring(7);
                    else if (s.regionMatches(true, 0, "title=", 0, 6))
                        title=s.substring(6);
                }
                if (title!=null) {
                    if (artist!=null)
                        ((TextView)findViewById(R.id.status1)).setText(title+" - "+title);
                    else
                        ((TextView)findViewById(R.id.status1)).setText(title);
                }
            } else {
                meta=(String)BASS.BASS_ChannelGetTags(chan, BASS_TAG_HLS_EXTINF);
                if (meta!=null) { // got HLS segment info
                    int i=meta.indexOf(',');
                    if (i>0)
                        ((TextView)findViewById(R.id.status1)).setText(meta.substring(i+1));
                }
            }
        }
    }

    BASS.SYNCPROC MetaSync=new BASS.SYNCPROC() {
        public void SYNCPROC(int handle, int channel, int data, Object user) {
            runOnUiThread(new Runnable() {
                public void run() {
                    DoMeta();
                }
            });
        }
    };

    BASS.SYNCPROC EndSync=new BASS.SYNCPROC() {
        public void SYNCPROC(int handle, int channel, int data, Object user) {
            runOnUiThread(new Runnable() {
                public void run() {
                    ((TextView)findViewById(R.id.status1)).setText("");
                }
            });
        }
    };

    BASS.DOWNLOADPROC StatusProc=new BASS.DOWNLOADPROC() {
        public void DOWNLOADPROC(ByteBuffer buffer, int length, Object user) {
            if ((Integer)user!=req) return; // make sure this is still the current request
            if (buffer!=null && length==0) { // got HTTP/ICY tags
                String[] s;
                try {
                    CharsetDecoder dec= Charset.forName("ISO-8859-1").newDecoder();
                    ByteBuffer temp=ByteBuffer.allocate(buffer.limit()); // CharsetDecoder doesn't like a direct buffer?
                    temp.put(buffer);
                    temp.position(0);
                    s=dec.decode(temp).toString().split("\0"); // convert buffer to string array
                } catch (Exception e) {
                    return;
                }
                runOnUiThread(new MainActivity.RunnableParam(s[0]) { // 1st string = status
                    public void run() {
                        //((TextView)findViewById(R.id.status1)).setText((String)param);
                        Log.i("Connection",(String)param);
                    }
                });
            }
        }
    };

    public class OpenURL implements Runnable {
        String url;
        OpenURL(String p) {
            url=p;
        }
        public void run() {
            int r;
            synchronized(lock) { // make sure only 1 thread at a time can do the following
                r=++req; // increment the request counter for this request
            }
            BASS.BASS_StreamFree(chan); // close old stream
            runOnUiThread(new Runnable() {
                public void run() {
                    pDialog.setMessage("Connecting...");
                    //((TextView)findViewById(R.id.status1)).setText("connecting...");
                }
            });
            int c=BASS.BASS_StreamCreateURL(url, 0, BASS.BASS_STREAM_BLOCK|BASS.BASS_STREAM_STATUS|BASS.BASS_STREAM_AUTOFREE, StatusProc, r); // open URL
            synchronized(lock) {
                if (r!=req) { // there is a newer request, discard this stream
                    if (c!=0) BASS.BASS_StreamFree(c);
                    return;
                }
                chan=c; // this is now the current stream
            }
            if (chan==0) { // failed to open
                runOnUiThread(new Runnable() {
                    public void run() {
                        ((TextView)findViewById(R.id.status1)).setText("");
                    }
                });
                Error("Can't play the stream");
            } else {
                handler.postDelayed(timer, 50); // start prebuffer monitoring
            }
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
            case R.id.action_toAdminActivity:
                if (new InternetChecker().hasConnection(getApplicationContext())){
                    intent = new Intent(MainActivity.this, AdminActivity.class);
                    startActivity(intent);
                }
                return true;
            case R.id.action_copy:
                if (radioUrl != null) {
                    ClipboardManager clipboard = (ClipboardManager) getApplicationContext().getSystemService(Context.CLIPBOARD_SERVICE);
                    ClipData clipData = ClipData.newPlainText("", radioUrl+bitrate);
                    clipboard.setPrimaryClip(clipData);
                    Toast.makeText(getApplicationContext(), getString(R.string.link_was_copied) + " (" + bitrate + "kbps)", Toast.LENGTH_SHORT).show();
                }else Toast.makeText(getApplicationContext(), getString(R.string.error) + " Button, Clipboard, Bitrate", Toast.LENGTH_SHORT).show();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}
