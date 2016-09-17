package com.upwork.iurii.dms_uploader;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;

public class UploadTask extends AsyncTask<Void, Object, Void> {

    private static boolean running = false;
    private static UploadTask instance;
    private String url;
    private final String docType;
    private final String deviceID;
    private Integer imgQuality;

    private DBManager db;

    public UploadTask(String url, String docType, String deviceID, Integer imgQuality) {
        this.url = url;
        this.docType = docType;
        this.deviceID = deviceID;
        this.imgQuality = imgQuality;
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
            String fileurl = (String) record.get("fileurl");
            String ref = (String) record.get("ref");
            String status = (String) record.get("status");

            if (!status.equals("Done")) {
                Log.e("TASK", (String) record.get("filename"));

                publishProgress(id, "Compressing...");
                db.updateQueueRecordStatus(id, "Compressing...", null);
                Bitmap bmp = BitmapFactory.decodeFile(Uri.parse(fileurl).getPath());
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                bmp.compress(Bitmap.CompressFormat.JPEG, imgQuality, bos);

                publishProgress(id, "Uploading...");
                db.updateQueueRecordStatus(id, "Uploading...", null);
                HashMap<String, Object> map = new HashMap<>();
                map.put("deviceid", deviceID);
                map.put("doctype", docType);
                map.put("reference", ref);
                map.put("image", HttpRequest.Base64.encodeBytes(bos.toByteArray()));

                try {
                    HttpRequest request = HttpRequest.post(url).acceptJson().send(Utils_JSON.mapToJson(map).toString());
                    String response = request.body();
                    JSONObject responseJson = new JSONObject(response);
                    switch (request.code()) {
                        case 200:
                            String docno = responseJson.getString("docno");
                            publishProgress(id, "Done", docno);
                            db.updateQueueRecordStatus(id, "Done", docno);
                            break;
                        case 500:
                            String text = String.format("%s: %s", responseJson.getString("errorno"), responseJson.getString("errortext"));
                            publishProgress(id, "Error", text);
                            db.updateQueueRecordStatus(id, "Error", text);
                            break;
                    }
                } catch (JSONException | HttpRequest.HttpRequestException e) {
                    e.printStackTrace();
                    publishProgress(id, "Error", "API error");
                    db.updateQueueRecordStatus(id, "Error", "API error");
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
