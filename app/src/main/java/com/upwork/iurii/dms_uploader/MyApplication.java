package com.upwork.iurii.dms_uploader;

import android.app.Application;

import java.lang.ref.WeakReference;

public class MyApplication extends Application {

    private static MyApplication instance;
    private WeakReference<MainActivity> mainActivity;

    public static MyApplication getInstance() {
        return instance;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
    }

    public void setMainActivity(MainActivity mainActivity) {
        this.mainActivity = new WeakReference<>(mainActivity);
    }

    public WeakReference<MainActivity> getMainActivity() {
        return mainActivity;
    }
}