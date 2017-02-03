package com.github.guwenk.smuradio;

import android.content.res.Resources;
import android.os.AsyncTask;
import android.util.Log;

import java.io.IOException;
import java.net.URL;


class VoteRequest extends AsyncTask<String, Void, Void> {
    @Override
    protected Void doInBackground(String... filename) {
        try {
            Log.d("FilePath", filename[0]);
            new URL("http://" + Resources.getSystem().getString(R.string.request_address)
                    + "/?pass=" + Resources.getSystem().getString(R.string.request_pass)
                    + "&action=songrequest&filename=" + filename[0]).openConnection().getInputStream(); // Отправка запроса
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }


}
