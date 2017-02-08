package com.github.guwenk.smuradio;



import java.io.IOException;
import java.net.URL;

class UrlRequest extends Thread{
    private String address;
    private String pass;
    private String command = "";
    UrlRequest(String command, String address, String pass){
        this.command = command;
        this.address = address;
        this.pass = pass;
    }

    @Override
    public void run() {
        try {
            new URL("http://" + address + "/?pass=" + pass + "&cmd="+command).openConnection().getInputStream();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


}
