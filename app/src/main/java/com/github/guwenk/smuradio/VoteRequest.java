package com.github.guwenk.smuradio;

import android.os.AsyncTask;
import android.util.Log;

import java.io.IOException;
import java.net.URL;


class VoteRequest extends AsyncTask<String, Void, Void> {
    @Override
    protected Void doInBackground(String... strings) {
        try {
            Log.d("FilePath", strings[0]);
            new URL("http://" + strings[1] + "/?pass=" + strings[2] + "&action=songrequest&filename=" + strings[0]).openConnection().getInputStream(); // Отправка запроса
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }


}
