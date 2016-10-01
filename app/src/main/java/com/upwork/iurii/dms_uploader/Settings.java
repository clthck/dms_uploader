package com.upwork.iurii.dms_uploader;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public class Settings implements SharedPreferences.OnSharedPreferenceChangeListener {

    private static Settings instance;
    private SharedPreferences sharedPreferences;
    private MyApplication application;

    private Settings() {
        instance = this;
        application = MyApplication.getInstance();
        PreferenceManager.setDefaultValues(application, R.xml.settings, false);
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(application);
        sharedPreferences.registerOnSharedPreferenceChangeListener(this);
    }

    public static Settings getInstance() {
        if (instance == null) return new Settings();
        else return instance;
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String s) {
        Pref pref = Pref.valueOf(s);
        switch (pref) {
            case image_quality:
                break;
            case api_url:
                break;
            case doc_type:
                break;
            case device_id:
                if (application.getMainActivity() != null && application.getMainActivity().get() != null && application.getMainActivity().get().getNavDeviceIDText() != null)
                    application.getMainActivity().get().getNavDeviceIDText().setText((String) getPref(Pref.device_id));
                break;
        }
    }

    public Object getPref(Pref pref) {
        Class type = pref.getType();
        String key = pref.name();
        Object defaultVal = pref.getDefaultVal();

        if (type == String.class) {
            return sharedPreferences.getString(key, (String) defaultVal);
        } else if (type == Boolean.class) {
            return sharedPreferences.getBoolean(key, (Boolean) defaultVal);
        } else if (type == Integer.class) {
            return sharedPreferences.getInt(key, (Integer) defaultVal);
        } else if (type == Long.class) {
            return sharedPreferences.getLong(key, (Long) defaultVal);
        } else if (type == Float.class) {
            return sharedPreferences.getFloat(key, (Float) defaultVal);
        }

        return null;
    }

    public void setPref(Pref pref, Object value) {
        String key = pref.name();
        if (value.getClass() != pref.getType()) return;

        SharedPreferences.Editor editor = sharedPreferences.edit();
        if (value instanceof String) {
            editor.putString(key, (String) value);
        } else if (value instanceof Boolean) {
            editor.putBoolean(key, (Boolean) value);
        } else if (value instanceof Integer) {
            editor.putInt(key, (Integer) value);
        } else if (value instanceof Long) {
            editor.putLong(key, (Long) value);
        } else if (value instanceof Float) {
            editor.putFloat(key, (Float) value);
        }
        editor.apply();
    }

    public enum Pref {
        image_quality(Integer.class, 70),
        api_url(String.class, "http://"),
        api_username(String.class, ""),
        api_password(String.class, ""),
        doc_type(String.class, ""),
        device_id(String.class, ""),
        test_web_service(String.class, "");

        private Class type;
        private Object defaultVal;

        Pref(Class type, Object defaultVal) {
            this.type = type;
            this.defaultVal = defaultVal;
        }

        public Class getType() {
            return type;
        }

        public Object getDefaultVal() {
            return defaultVal;
        }
    }

}
