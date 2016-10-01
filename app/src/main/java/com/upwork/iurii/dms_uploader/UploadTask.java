package com.upwork.iurii.dms_uploader;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;

public class UploadTask extends AsyncTask<Void, Object, Void> {

    private static boolean running = false;
    private static UploadTask instance;
    private final String docType;
    private final String deviceID;
    private String url;
    private Integer imgQuality;
    private ArrayList<HashMap<String, Object>> queue;
    private String username;
    private String password;

    private DBManager db;
    private WeakReference<UploadTaskListener> listener;

    public UploadTask(ArrayList<HashMap<String, Object>> queue) {
        username = (String) Settings.getInstance().getPref(Settings.Pref.api_username);
        password = (String) Settings.getInstance().getPref(Settings.Pref.api_password);
        url = (String) Settings.getInstance().getPref(Settings.Pref.api_url);
        docType = (String) Settings.getInstance().getPref(Settings.Pref.doc_type);
        deviceID = (String) Settings.getInstance().getPref(Settings.Pref.device_id);
        imgQuality = (Integer) Settings.getInstance().getPref(Settings.Pref.image_quality);
        this.queue = queue;
        instance = this;
        db = DBManager.getInstance();
    }

    public static boolean isRunning() {
        return running;
    }

    public static UploadTask getRunningTask() {
        return instance;
    }

    public void setListener(UploadTaskListener listener) {
        this.listener = new WeakReference<>(listener);
    }

    @Override
    protected Void doInBackground(Void... voids) {
        for (HashMap<String, Object> record : queue) {
            db.updateRecordStatus((Integer) record.get("id"), RecordStatus.QUEUED, null);
            publishProgress(record.get("id"), RecordStatus.QUEUED, null);
        }
        for (HashMap<String, Object> record : queue) {
            Integer id = (Integer) record.get("id");
            String fileurl = (String) record.get("fileurl");
            String ref = (String) record.get("ref");

            Log.e("TASK", (String) record.get("filename"));

            publishProgress(id, "Compressing...");
            Bitmap bmp = BitmapFactory.decodeFile(Uri.parse(fileurl).getPath());
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            bmp.compress(Bitmap.CompressFormat.JPEG, imgQuality, bos);

            publishProgress(id, "Uploading...");
            HashMap<String, Object> map = new HashMap<>();
            map.put("deviceid", deviceID);
            map.put("doctype", docType);
            map.put("reference", ref);
            map.put("image", HttpRequest.Base64.encodeBytes(bos.toByteArray()));

            try {
                String text;
                String requestToSend = Utils_JSON.mapToJson(map).toString();
                HttpRequest request = HttpRequest.post(url).basic(username, password).send(requestToSend);
                String response = request.body();
                JSONObject responseJson = new JSONObject(response);
                int code = request.code();
                switch (code) {
                    case 200:
                        String docno = responseJson.getString("docno");
                        publishProgress(id, RecordStatus.DONE, docno);
                        db.updateRecordStatus(id, RecordStatus.DONE, docno);
                        break;
                    case 500:
                        text = String.format("%s: %s.", responseJson.getString("errorno"), responseJson.getString("errortext"));
                        publishProgress(id, RecordStatus.ERROR, text);
                        db.updateRecordStatus(id, RecordStatus.ERROR, text);
                    default:
                        text = String.format("Got %s code. 200 or 500 expected.", String.valueOf(code));
                        publishProgress(id, RecordStatus.ERROR, text);
                        db.updateRecordStatus(id, RecordStatus.ERROR, text);
                }
            } catch (Exception e) {
                e.printStackTrace();
                String text = String.format("%s Tap to retry", e.getMessage());
                publishProgress(id, RecordStatus.ERROR, text);
                db.updateRecordStatus(id, RecordStatus.ERROR, text);
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
