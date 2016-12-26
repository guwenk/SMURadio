package com.github.guwenk.smuradio;

import android.os.AsyncTask;
import android.util.Log;

import java.io.IOException;
import java.net.URL;


class VoteRequest extends AsyncTask<String, Void, Void> {
    @Override
    protected Void doInBackground(String... filename) {
        try {
            Log.d("FilePath", filename[0]);
            new URL("http://192.168.1.69:9001/?pass=yHZDVtGwCC&action=songrequest&filename=" + filename[0]).openConnection().getInputStream(); // Отправка запроса
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
}
