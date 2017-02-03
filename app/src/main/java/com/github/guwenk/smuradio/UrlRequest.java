package com.github.guwenk.smuradio;


import android.content.res.Resources;

import java.io.IOException;
import java.net.URL;

class UrlRequest extends Thread{
    private String command = "";
    UrlRequest(String command){
        this.command = command;
    }

    @Override
    public void run() {
        try {
            new URL("http://" + Resources.getSystem().getString(R.string.request_address)
                    + "/?pass=" + Resources.getSystem().getString(R.string.request_pass)
                    + "&cmd="+command).openConnection().getInputStream();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


}
