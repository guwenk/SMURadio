package com.github.guwenk.smuradio;

import android.widget.Button;

import be.rijckaert.tim.animatedvector.FloatingMusicActionButton;


class ButtonTimeout extends Thread{
    private FloatingMusicActionButton musicFab;
    private int millis;
    private Button button;
    private boolean isMFab;

    ButtonTimeout(FloatingMusicActionButton musicFab, int millis){
        this.musicFab = musicFab;
        this.millis = millis;
        isMFab = true;
    }
    ButtonTimeout(Button button, int millis){
        this.button = button;
        this.millis = millis;
        isMFab = false;
    }

    @Override
    public void run() {
        if (isMFab) musicFab.setClickable(false);
        else button.setClickable(false);
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        if (isMFab) musicFab.setClickable(true);
        else button.setClickable(true);
    }
}
