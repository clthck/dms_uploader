package com.upwork.iurii.dms_uploader;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.HashMap;

public class DBManager {

    private static final int VERSION = 1;
    private static final String DATABASE_NAME = "app_db";

    public static DBManager instance;

    private SQLiteDatabase db;

    public static DBManager getInstance() {
        if (instance == null)
            return new DBManager();
        else return instance;
    }

    private DBManager() {
        DBHelper dbHelper = new DBHelper(MyApplication.getInstance(), DATABASE_NAME, VERSION);
        db = dbHelper.getWritableDatabase();
    }

    public int getCountForRef(String ref) {
        int result = 0;
        Cursor cursor = db.rawQuery("SELECT count FROM image_counters WHERE id = ?", new String[]{ref});
        if (cursor.moveToNext()) result = cursor.getInt(cursor.getColumnIndex("count"));
        cursor.close();
        return result;
    }

    public int increaseCountForRef(String ref) {
        int result;
        Cursor cursor = db.rawQuery("SELECT count FROM image_counters WHERE id = ?", new String[]{ref});
        if (cursor.moveToNext()) {
            int currentCount = cursor.getInt(cursor.getColumnIndex("count")) + 1;
            ContentValues contentValues = new ContentValues();
            contentValues.put("count", currentCount);
            db.update("image_counters", contentValues, "id = ?", new String[]{ref});
            result = currentCount;
        } else {
            ContentValues contentValues = new ContentValues();
            contentValues.put("id", ref);
            contentValues.put("count", 1);
            db.insert("image_counters", null, contentValues);
            result = 1;
        }
        cursor.close();
        return result;
    }

    public ArrayList<HashMap<String, Object>> getQueue() {
        ArrayList<HashMap<String, Object>> result = new ArrayList<>();
        Cursor cursor = db.rawQuery("SELECT id, fileurl, filename, status, upload_result FROM queue", null);
        while (cursor.moveToNext()) {
            HashMap<String, Object> hashMap = new HashMap<>();
            hashMap.put("id", cursor.getInt(cursor.getColumnIndex("id")));
            hashMap.put("fileurl", cursor.getString(cursor.getColumnIndex("fileurl")));
            hashMap.put("filename", cursor.getString(cursor.getColumnIndex("filename")));
            hashMap.put("status", cursor.getString(cursor.getColumnIndex("status")));
            hashMap.put("upload_result", cursor.getString(cursor.getColumnIndex("upload_result")));
            result.add(hashMap);
        }
        cursor.close();
        return result;
    }

    public void addQueueRecord(String fileurl, String filename, String status) {
        ContentValues contentValues = new ContentValues();
        contentValues.put("fileurl", fileurl);
        contentValues.put("filename", filename);
        contentValues.put("status", status);
        db.insert("queue", null, contentValues);
    }

    public void updateQueueRecordStatus(Integer id, String status, String upload_result) {
        ContentValues contentValues = new ContentValues();
        contentValues.put("status", status);
        if (upload_result != null) contentValues.put("upload_result", upload_result);
        db.update("queue", contentValues, "id = ?", new String[]{String.valueOf(id)});
    }

    public void clearQueueByStatus(String status) {
        db.delete("queue", "status = ?", new String[]{status});
    }

    public int getQueuePendingSize() {
        int result = 0;
        Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM queue WHERE upload_result is NULL", null);
        if (cursor.moveToNext()) {
            result = cursor.getInt(0);
        }
        cursor.close();
        return result;
    }

    private class DBHelper extends SQLiteOpenHelper {

        private DBHelper(Context context, String name, int version) {
            super(context, name, null, version);
        }

        @Override
        public void onCreate(SQLiteDatabase sqLiteDatabase) {
            sqLiteDatabase.execSQL("CREATE TABLE queue (id INTEGER PRIMARY KEY AUTOINCREMENT, fileurl TEXT, filename TEXT, status TEXT, upload_result TEXT)");
            sqLiteDatabase.execSQL("CREATE TABLE image_counters (id TEXT PRIMARY KEY, count INTEGER)");
        }

        @Override
        public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1) {
            sqLiteDatabase.execSQL("DROP TABLE IF EXISTS queue");
            sqLiteDatabase.execSQL("DROP TABLE IF EXISTS image_counters");
            onCreate(sqLiteDatabase);
        }
    }

}
