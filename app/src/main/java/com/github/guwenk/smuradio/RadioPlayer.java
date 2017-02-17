package com.github.guwenk.smuradio;

import android.os.Handler;
import android.widget.TextView;
import com.un4seen.bass.BASS;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;


class RadioPlayer{
    private int req, chan;
    private MainActivity mainActivity;

    private static final int BASS_SYNC_HLS_SEGMENT = 0x10300;
    private static final int BASS_TAG_HLS_EXTINF = 0x14000;
    private int connection;

    private Handler handler = new Handler();
    private Runnable timer;
    private final Object lock = new Object();


    private void DoMeta() {
        String meta=(String)BASS.BASS_ChannelGetTags(chan, BASS.BASS_TAG_META);
        if (meta!=null) { // got Shoutcast metadata
            int ti=meta.indexOf("StreamTitle='");
            if (ti>=0) {
                String title=meta.substring(ti+13, meta.indexOf("';", ti+13));
                ((TextView)mainActivity.findViewById(R.id.main_status1)).setText(title);
                if (mainActivity.notifService != null)
                    mainActivity.notifService.refreshTitle(title);
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
                    if (artist!=null) {
                        ((TextView) mainActivity.findViewById(R.id.main_status1)).setText(title + " - " + title);
                        if (mainActivity.notifService != null)
                            mainActivity.notifService.refreshTitle(title + " - " + title);
                    }
                    else{
                        ((TextView)mainActivity.findViewById(R.id.main_status1)).setText(title);
                        if (mainActivity.notifService != null)
                            mainActivity.notifService.refreshTitle(title);
                    }

                }
            } else {
                meta=(String)BASS.BASS_ChannelGetTags(chan, BASS_TAG_HLS_EXTINF);
                if (meta!=null) { // got HLS segment info
                    int i=meta.indexOf(',');
                    if (i>0) {
                        ((TextView) mainActivity.findViewById(R.id.main_status1)).setText(meta.substring(i + 1));
                        if (mainActivity.notifService != null)
                            mainActivity.notifService.refreshTitle(meta.substring(i + 1));
                    }
                }
            }
        }
    }

    private BASS.SYNCPROC MetaSync=new BASS.SYNCPROC() {
        public void SYNCPROC(int handle, int channel, int data, Object user) {
            mainActivity.runOnUiThread(new Runnable() {
                public void run() {
                    DoMeta();
                }
            });
        }
    };

    private BASS.SYNCPROC EndSync=new BASS.SYNCPROC() {
        public void SYNCPROC(int handle, int channel, int data, Object user) {
            mainActivity.runOnUiThread(new Runnable() {
                public void run() {
                    ((TextView)mainActivity.findViewById(R.id.main_status1)).setText("");
                    if (mainActivity.notifService != null)
                        mainActivity.notifService.refreshTitle(mainActivity.getString(R.string.app_name));
                }
            });
        }
    };

    private BASS.DOWNLOADPROC StatusProc=new BASS.DOWNLOADPROC() {
        public void DOWNLOADPROC(ByteBuffer buffer, int length, Object user) {
            if ((Integer)user!=req) return; // make sure this is still the current request
            if (buffer!=null && length==0) { // got HTTP/ICY tags
                try {
                    Charset.forName("ISO-8859-1").newDecoder();
                    ByteBuffer temp=ByteBuffer.allocate(buffer.limit()); // CharsetDecoder doesn't like a direct buffer?
                    temp.put(buffer);
                    temp.position(0);
                } catch (Exception ignored) {
                }
            }
        }
    };


    private class OpenURL implements Runnable {
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
            mainActivity.runOnUiThread(new Runnable() {
                public void run() {
                    ((TextView)mainActivity.findViewById(R.id.main_status1)).setText(R.string.connecting);
                    if (mainActivity.notifService != null)
                        mainActivity.notifService.refreshTitle(mainActivity.getString(R.string.connecting));
                }
            });
            connection = BASS.BASS_StreamCreateURL(url, 0, BASS.BASS_STREAM_BLOCK|BASS.BASS_STREAM_STATUS|BASS.BASS_STREAM_AUTOFREE, StatusProc, r); // open URL
            synchronized(lock) {
                if (r!=req) { // there is a newer request, discard this stream
                    if (connection!=0) BASS.BASS_StreamFree(connection);
                    return;
                }
                chan=connection; // this is now the current stream
            }
            if (chan==0) { // failed to open
                mainActivity.runOnUiThread(new Runnable() {
                    public void run() {
                        ((TextView)mainActivity.findViewById(R.id.main_status1)).setText("");
                        if (mainActivity.notifService != null)
                            mainActivity.notifService.refreshTitle(mainActivity.getString(R.string.app_name));
                    }
                });
                mainActivity.Error(mainActivity.getString(R.string.cant_play_the_stream));
            } else {
                handler.postDelayed(timer, 50); // start prebuffer monitoring
            }
        }
    }


    RadioPlayer(final MainActivity mainActivity) {
        this.mainActivity = mainActivity;
        // initialize output device
        if (!BASS.BASS_Init(-1, 44100, 0)) {
            mainActivity.Error("Can't initialize device");
            return;
        }
        BASS.BASS_SetConfig(BASS.BASS_CONFIG_NET_PLAYLIST, 1); // enable playlist processing
        BASS.BASS_SetConfig(BASS.BASS_CONFIG_NET_PREBUF, 0); // minimize automatic pre-buffering, so we can do it (and display it) instead

        // load AAC and HLS add-ons (if present)
        BASS.BASS_PluginLoad("libbass_aac.so", 0);
        BASS.BASS_PluginLoad("libbasshls.so", 0);

        timer = new Runnable() {
            public void run() {
                // monitor prebuffering progress
                int progress = (int) BASS.BASS_StreamGetFilePosition(chan, BASS.BASS_FILEPOS_BUFFER);
                if (progress < 0) return; // failed, eg. stream freed
                progress = progress * 100 / (int) BASS.BASS_StreamGetFilePosition(chan, BASS.BASS_FILEPOS_END); // percentage of buffer filled
                if (progress > 75 || BASS.BASS_StreamGetFilePosition(chan, BASS.BASS_FILEPOS_CONNECTED) == 0) { // over 75% full (or end of download)
                    DoMeta();
                    BASS.BASS_ChannelSetSync(chan, BASS.BASS_SYNC_META, 0, MetaSync, 0); // Shoutcast
                    BASS.BASS_ChannelSetSync(chan, BASS.BASS_SYNC_OGG_CHANGE, 0, MetaSync, 0); // Icecast/OGG
                    BASS.BASS_ChannelSetSync(chan, BASS_SYNC_HLS_SEGMENT, 0, MetaSync, 0); // HLS
                    // set sync for end of stream
                    BASS.BASS_ChannelSetSync(chan, BASS.BASS_SYNC_END, 0, EndSync, 0);
                    // play it!
                    BASS.BASS_ChannelPlay(chan, false);
                } else {
                    ((TextView) mainActivity.findViewById(R.id.main_status1)).setText(mainActivity.getString(R.string.buffering)+" (" + progress +"%)");
                    if (mainActivity.notifService != null)
                        mainActivity.notifService.refreshTitle(mainActivity.getString(R.string.buffering)+" (" + progress +"%)");
                    handler.postDelayed(this, 50);
                }
            }
        };
    }
    void startPlayer(String radioUrl, String bitrate){
        new Thread(new OpenURL(radioUrl + bitrate)).start();
    }
    void stopBASS(){
        BASS.BASS_StreamFree(connection);
        BASS.BASS_Free();
    }

}
