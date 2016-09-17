package com.upwork.iurii.dms_uploader;

import android.os.AsyncTask;
import android.util.Log;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;

public class UploadTask extends AsyncTask<Void, Object, Void> {

    private static boolean running = false;
    private static UploadTask instance;

    private DBManager db;

    public UploadTask() {
        instance = this;
        db = DBManager.getInstance();
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

        ArrayList<HashMap<String, Object>> queue = db.getQueue();
        for (HashMap<String, Object> record : queue) {
            Integer id = (Integer) record.get("id");
            String status = (String) record.get("status");
            if (!status.equals("Done")) {
                try {
                    Log.e("TASK", (String) record.get("filename"));

                    publishProgress(id, "Compressing...");
                    db.updateQueueRecordStatus(id, "Compressing...", null);
                    Thread.sleep(500);

                    publishProgress(id, "Uploading...");
                    db.updateQueueRecordStatus(id, "Uploading...", null);
                    Thread.sleep(3000);

                    if (id == 3) {
                        publishProgress(id, "Error", "4343");
                        db.updateQueueRecordStatus(id, "Error", "4343");
                    } else {
                        publishProgress(id, "Done", "55545");
                        db.updateQueueRecordStatus(id, "Done", "55545");
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
            if (values.length == 2) {
                listener.get().onStatusChanged((Integer) values[0], (String) values[1], "");
            } else if (values.length == 3) {
                listener.get().onStatusChanged((Integer) values[0], (String) values[1], (String) values[2]);
            }
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

    public interface UploadTaskListener {
        void onStatusChanged(Integer id, String status, String uploadResult);

        void onFinished();

        void onStarted();
    }
}
