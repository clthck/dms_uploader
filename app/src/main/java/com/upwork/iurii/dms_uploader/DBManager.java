package com.upwork.iurii.dms_uploader;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.HashMap;

public class DBManager {

    private static final int VERSION = 3;
    private static final String DATABASE_NAME = "app_db";

    private static DBManager instance;

    private SQLiteDatabase db;

    private DBManager() {
        instance = this;
        DBHelper dbHelper = new DBHelper(MyApplication.getInstance(), DATABASE_NAME, VERSION);
        db = dbHelper.getWritableDatabase();
    }

    public static DBManager getInstance() {
        if (instance == null)
            return new DBManager();
        else return instance;
    }

    public int addNewRecord(String fileurl, String filename, String ref) {
        ContentValues contentValues = new ContentValues();
        contentValues.put("fileurl", fileurl);
        contentValues.put("filename", filename);
        contentValues.put("ref", ref);
        contentValues.put("status", RecordStatus.NEW);
        return (int) db.insert("records", null, contentValues);
    }

    public void deleteRecordById(int id) {
        db.delete("records", "id = ?", new String[]{String.valueOf(id)});
    }

    public HashMap<String, Object> getRecordById(int id) {
        HashMap<String, Object> result = null;
        Cursor cursor = db.rawQuery("SELECT * FROM records WHERE id = ?", new String[]{String.valueOf(id)});
        if (cursor.moveToNext()) {
            result = generateRecordObject(cursor);
        }
        cursor.close();
        return result;
    }

    public void updateRecordStatus(Integer id, String status, String upload_result) {
        ContentValues contentValues = new ContentValues();
        contentValues.put("status", status);
        if (upload_result != null) contentValues.put("upload_result", upload_result);
        db.update("records", contentValues, "id = ?", new String[]{String.valueOf(id)});
    }

    public ArrayList<String> clearQueueByStatusDone() {
        ArrayList<String> result = new ArrayList<>();
        Cursor cursor = db.rawQuery("SELECT fileurl FROM records WHERE status = ?", new String[]{RecordStatus.DONE});
        while (cursor.moveToNext()) {
            result.add(cursor.getString(0));
        }
        cursor.close();
        db.delete("records", "status = ?", new String[]{RecordStatus.DONE});
        return result;
    }

    public ArrayList<String> clearQueueByStatusError() {
        ArrayList<String> result = new ArrayList<>();
        Cursor cursor = db.rawQuery("SELECT fileurl FROM records WHERE status = ?", new String[]{RecordStatus.ERROR});
        while (cursor.moveToNext()) {
            result.add(cursor.getString(0));
        }
        cursor.close();
        db.delete("records", "status = ?", new String[]{RecordStatus.ERROR});
        return result;
    }

    public ArrayList<HashMap<String, Object>> getQueuePendingRecords() {
        ArrayList<HashMap<String, Object>> result = new ArrayList<>();
        Cursor cursor = db.rawQuery("SELECT * FROM records WHERE status != ? AND status != ?", new String[]{RecordStatus.DONE, RecordStatus.NEW});
        while (cursor.moveToNext()) {
            result.add(generateRecordObject(cursor));
        }
        cursor.close();
        return result;
    }

    public ArrayList<HashMap<String, Object>> getQueueRecords() {
        ArrayList<HashMap<String, Object>> result = new ArrayList<>();
        Cursor cursor = db.rawQuery("SELECT * FROM records WHERE status != ?", new String[]{RecordStatus.NEW});
        while (cursor.moveToNext()) {
            result.add(generateRecordObject(cursor));
        }
        cursor.close();
        return result;
    }

    public ArrayList<HashMap<String, Object>> getQueueErroredRecords() {
        ArrayList<HashMap<String, Object>> result = new ArrayList<>();
        Cursor cursor = db.rawQuery("SELECT * FROM records WHERE status = ?", new String[]{RecordStatus.ERROR});
        while (cursor.moveToNext()) {
            result.add(generateRecordObject(cursor));
        }
        cursor.close();
        return result;
    }

    public ArrayList<HashMap<String, Object>> getNewRecordsByRef(String ref) {
        ArrayList<HashMap<String, Object>> result = new ArrayList<>();
        Cursor cursor = db.rawQuery("SELECT * FROM records WHERE status = ? AND ref = ?", new String[]{RecordStatus.NEW, ref});
        while (cursor.moveToNext()) {
            result.add(generateRecordObject(cursor));
        }
        cursor.close();
        return result;
    }

    private HashMap<String, Object> generateRecordObject(Cursor cursor) {
        HashMap<String, Object> hashMap = new HashMap<>();
        hashMap.put("id", cursor.getInt(cursor.getColumnIndex("id")));
        hashMap.put("fileurl", cursor.getString(cursor.getColumnIndex("fileurl")));
        hashMap.put("filename", cursor.getString(cursor.getColumnIndex("filename")));
        hashMap.put("ref", cursor.getString(cursor.getColumnIndex("ref")));
        hashMap.put("status", cursor.getString(cursor.getColumnIndex("status")));
        hashMap.put("upload_result", cursor.getString(cursor.getColumnIndex("upload_result")));
        return hashMap;
    }

    private class DBHelper extends SQLiteOpenHelper {

        private DBHelper(Context context, String name, int version) {
            super(context, name, null, version);
        }

        @Override
        public void onCreate(SQLiteDatabase sqLiteDatabase) {
            sqLiteDatabase.execSQL("CREATE TABLE records (id INTEGER PRIMARY KEY AUTOINCREMENT, fileurl TEXT, filename TEXT, ref TEXT, status TEXT, upload_result TEXT)");
        }

        @Override
        public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1) {
            sqLiteDatabase.execSQL("DROP TABLE IF EXISTS records");
            onCreate(sqLiteDatabase);
        }
    }

}
