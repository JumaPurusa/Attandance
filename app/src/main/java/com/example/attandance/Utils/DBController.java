package com.example.attandance.Utils;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.example.attandance.Models.User;

import java.util.HashMap;

public class DBController extends SQLiteOpenHelper {

    public DBController(Context context){
        super(context, "attandance.db", null, 1);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {

        String query;
        query = "CREATE TABLE user (id INTEGER PRIMARY KEY AUTOINCREMENT, username VARCHAR, templateData TEXT, templateLength INTEGER)";
        db.execSQL(query);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        String query = "DROP TABLE IF EXISTS user";
        db.execSQL(query);
        onCreate(db);
    }

    private boolean isRecordExistInDatabase(String tableName, String field, String value) {
        String query = "SELECT * FROM " + tableName + " WHERE " + field + " = '" + value + "'";
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.rawQuery(query, null);
        if (c.moveToFirst()) {
            //Record exist
            c.close();
            return true;
        }
        //Record available
        c.close();
        return false;
    }

    public boolean addUser(String username, String templateData, int templateLength){

        // check if record exist in database by title number
        if(isRecordExistInDatabase("user", "username", username)){
            return false;
        }else{

            SQLiteDatabase db = this.getWritableDatabase();
            ContentValues contentValues = new ContentValues();

            contentValues.put("username", username);
            contentValues.put("templateData", templateData);
            contentValues.put("templateLength", templateLength);

            db.insert("user", null, contentValues);
            db.close();

            return true;
        }
    }

    /*
     * this method will give us all the users stored in sqlite
     * */
    public Cursor getUsers(){
        SQLiteDatabase db = this.getReadableDatabase();
        String sql = "SELECT * FROM user";
        Cursor c = db.rawQuery(sql, null);
        return c;
    }

    public Cursor getUser(String field, String value){

        SQLiteDatabase db = this.getReadableDatabase();
        String query = "SELECT * FROM user  WHERE " + field + " = '" + value + "'";
        Cursor c = db.rawQuery(query, null);
        return c;
    }

    public String getSyncStatus(){
        String msg = null;
        if(this.dbSyncCount() == 0){
            msg = "SQLite and Remote MySQL DBs are in Sync!";
        }else{
            msg = "DB Sync needed\n";
        }
        return msg;
    }

    /**
     * Get SQLite records that are yet to be Synced
     * @return
     */
    public int dbSyncCount(){
        int count = 0;
        String selectQuery = "SELECT  * FROM user";
        SQLiteDatabase database = this.getWritableDatabase();
        Cursor cursor = database.rawQuery(selectQuery, null);
        count = cursor.getCount();
        database.close();
        return count;
    }
}
