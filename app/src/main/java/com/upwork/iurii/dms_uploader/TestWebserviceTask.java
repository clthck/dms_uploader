package com.upwork.iurii.dms_uploader;

import android.os.AsyncTask;

import java.lang.ref.WeakReference;
import java.util.HashMap;

public class TestWebserviceTask extends AsyncTask<Void, Void, Boolean> {

    private String url;
    private WeakReference<TestResultListener> listener;
    private String username;
    private String password;

    public TestWebserviceTask(TestResultListener listener) {
        url = (String) Settings.getInstance().getPref(Settings.Pref.api_url);
        ;
        this.listener = new WeakReference<>(listener);
        username = (String) Settings.getInstance().getPref(Settings.Pref.api_username);
        password = (String) Settings.getInstance().getPref(Settings.Pref.api_password);
    }

    @Override
    protected Boolean doInBackground(Void... voids) {

        HashMap<String, Object> map = new HashMap<>();
        map.put("deviceid", "");
        map.put("doctype", "");
        map.put("reference", "");
        map.put("image", "");
        try {
            if (HttpRequest.post(url).basic(username, password).send(Utils_JSON.mapToJson(map).toString()).code() == 200)
                return true;
        } catch (HttpRequest.HttpRequestException ignored) {
        }
        return false;
    }

    @Override
    protected void onPostExecute(Boolean result) {
        if (listener != null && listener.get() != null) {
            listener.get().onTestResult(result);
        }
    }

    public interface TestResultListener {
        void onTestResult(Boolean isOk);
    }
}
