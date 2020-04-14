package com.onvit.chatapp.fcm;

import android.os.AsyncTask;

import java.io.InputStream;
import java.net.URL;

public class DownloadUri extends AsyncTask<String, String, InputStream> {
    @Override
    protected InputStream doInBackground(String... strings) {
        InputStream inputStream = null;
        try {
            String uri = strings[0];
            URL url = new URL(uri);
            inputStream = (InputStream) url.getContent();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return inputStream;
    }

    @Override
    protected void onPostExecute(InputStream inputStream) {

    }
}