package com.github.guwenk.smuradio;

import android.widget.ImageButton;

import be.rijckaert.tim.animatedvector.FloatingMusicActionButton;


class ButtonTimeout extends Thread{
    private FloatingMusicActionButton musicFab;
    private int millis;
    private ImageButton imageButton;
    private boolean isMFab;

    ButtonTimeout(FloatingMusicActionButton musicFab, int millis){
        this.musicFab = musicFab;
        this.millis = millis;
        isMFab = true;
    }
    ButtonTimeout(ImageButton button, int millis){
        this.imageButton = button;
        this.millis = millis;
        isMFab = false;
    }

    @Override
    public void run() {
        if (isMFab) musicFab.setClickable(false);
        else imageButton.setClickable(false);
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        if (isMFab) musicFab.setClickable(true);
        else imageButton.setClickable(true);
    }
}
