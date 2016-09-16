package com.upwork.iurii.dms_uploader.fragments;

import android.os.AsyncTask;
import android.util.Log;

import com.upwork.iurii.dms_uploader.DBManager;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;

public class UploadTask extends AsyncTask<Void, Object, Void> {

    private static boolean running = false;
    private static UploadTask instance;

    public UploadTask() {
        instance = this;
    }

    public static boolean isRunning() {
        return running;
    }

    public static UploadTask getRunningTask() {
        return instance;
    }

    private WeakReference<UploadTaskListener> listener;

    public void setListener(UploadTaskListener listener) {
        this.listener = new WeakReference<>(listener);
    }

    @Override
    protected Void doInBackground(Void... voids) {

        ArrayList<HashMap<String, Object>> queue = DBManager.getInstance().getQueue();
        for (HashMap<String, Object> record : queue) {
            Integer id = (Integer) record.get("id");
            String upload_result = (String) record.get("upload_result");
            if (upload_result == null) {
                try {
                    Log.e("TASK", (String) record.get("filename"));
                    publishProgress(id, "Compressing...");
                    Thread.sleep(500);
                    publishProgress(id, "Uploading...");
                    Thread.sleep(3000);
                    if (id == 3) {
                        publishProgress(id, "Error");
                    } else {
                        publishProgress(id, "Done");
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
        return null;
    }

    @Override
    protected final void onProgressUpdate(Object... values) {
        if (listener != null && listener.get() != null) {
            listener.get().onStatusChanged((Integer) values[0], (String) values[1]);
        }
    }

    @Override
    protected void onPreExecute() {
        Log.e("TASK", "onPreExecute: ");
        running = true;
        if (listener != null && listener.get() != null) {
            listener.get().onStarted();
        }
    }

    @Override
    protected void onPostExecute(Void aVoid) {
        Log.e("TASK", "onPostExecute: ");
        running = false;
        if (listener != null && listener.get() != null) {
            listener.get().onFinished();
        }
    }

    interface UploadTaskListener {
        void onStatusChanged(Integer id, String status);

        void onFinished();

        void onStarted();
    }
}
