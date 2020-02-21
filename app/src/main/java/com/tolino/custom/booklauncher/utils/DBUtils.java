package com.tolino.custom.booklauncher.utils;

import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseErrorHandler;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Created by WorldSkills2020 on 2/18/2020.
 */

public class DBUtils extends SQLiteOpenHelper {
    private static final int SQL_VERSION = 1;
    private static final String TAG  = "DBUtils";
    private DBUtils(Context context) {
        super(context, "bookdata2", null, SQL_VERSION);
    }

    private static DBUtils mInstance = null;
    public static void init(Context ctx){
        if(null!=mInstance){mInstance.close();}
        mInstance = new DBUtils(ctx);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        onUpgrade(db,-1,SQL_VERSION);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        int curversion = oldVersion;
        while (curversion<=newVersion){
            if(curversion==1){
                //type=0: this is a book; type=1: this is a folder/bookshelf, query sub uuid for more books. the Root uuid is 0;
                db.execSQL("create table library(id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL UNIQUE,uuid text,type integer,parent_uuid text,display_name text,path text,lastopen bigint default 0)");
                db.execSQL("insert into library(uuid,type,display_name,path) values(?,?,?,?)",new Object[]{"0",1,"ヴワル魔法図書館","/mnt/sdcard/Books"});
                Log.d(TAG, "onUpgrade: now execute version "+curversion);
            }
            curversion++;
        }
    }

    public static void execSql(String sql,Object... args){
        SQLiteDatabase db = mInstance.getWritableDatabase();
        db.execSQL(sql,args);
    }

    public static int getCount(String sql,String... args){
        SQLiteDatabase db = mInstance.getWritableDatabase();
        Cursor c = db.rawQuery("select * from library where "+sql,args);
        int count = c.getCount();
        c.close();
        return count;
    }

    public static void InsertBooks(List<BookEntry> books){
        for (BookEntry b :books) {
            mInstance.execSql("insert into library(uuid,type,parent_uuid,display_name,path,lastopen) values(?,?,?,?,?,?)",
                    b.getUUID(),
                    b.getType(),
                    b.getParentUUID(),
                    b.getDisplayName(),
                    b.getPath(),
                    b.getLastOpenTime()
                    );
        }
    }

    public static Cursor rawQuery(String sql,String... args){
        SQLiteDatabase db = mInstance.getWritableDatabase();
        return db.rawQuery(sql,args);
    }

    public static List<BookEntry> queryBooks(String sql,String... args){
        Cursor c = rawQuery("select id,uuid,type,parent_uuid,display_name,path,lastopen from library where "+sql,args);
        ArrayList<BookEntry> books  = new ArrayList<>();
        if(c.moveToFirst()){
            do{
                books.add(BookEntry.readFromDB(c.getInt(0),c.getString(1),c.getInt(2),c.getString(3),c.getString(4),c.getString(5),c.getLong(6)));
            }while (c.moveToNext());
        }
        c.close();
        return books;
    }

    public static class BookEntry{
        public static final String ROOT_UUID="0";
        public static final int TYPE_BOOK = 0;
        public static final int TYPE_FOLDER = 1;
        int id=-1;
        String UUID;
        int type;
        String parentUUID;
        String displayName;
        String path;
        long lastOpenTime;

        private BookEntry(int id,String UUID, int type, String parentUUID, String displayName, String path, long lastOpenTime) {
            this.UUID = UUID;
            this.type = type;
            this.parentUUID = parentUUID;
            this.displayName = displayName;
            this.path = path;
            this.lastOpenTime = lastOpenTime;
        }

        public static BookEntry createBook(String parentUUID,String title,String path){
            return new BookEntry(-1, java.util.UUID.randomUUID().toString(),TYPE_BOOK,parentUUID,title,path,System.currentTimeMillis());
        }

        public static BookEntry createFolder(String parentUUID,String path){
            File f = new File(path);
            return new BookEntry(-1, java.util.UUID.randomUUID().toString(),TYPE_FOLDER,parentUUID,f.getName(),path,System.currentTimeMillis());
        }

        public static BookEntry readFromDB(int id,String UUID, int type, String parentUUID, String displayName, String path, long lastOpenTime){
            return new BookEntry(id,UUID, type, parentUUID,displayName, path, lastOpenTime);
        }

        //region Getter and setter

        public int getId() {
            return id;
        }

        public void setId(int id) {
            this.id = id;
        }

        public String getUUID() {
            return UUID;
        }

        public void setUUID(String UUID) {
            this.UUID = UUID;
        }

        public int getType() {
            return type;
        }

        public void setType(int type) {
            this.type = type;
        }

        public String getParentUUID() {
            return parentUUID;
        }

        public void setParentUUID(String parentUUID) {
            this.parentUUID = parentUUID;
        }

        public String getDisplayName() {
            return displayName;
        }

        public void setDisplayName(String displayName) {
            this.displayName = displayName;
        }

        public String getPath() {
            return path;
        }

        public void setPath(String path) {
            this.path = path;
        }

        public long getLastOpenTime() {
            return lastOpenTime;
        }

        public void setLastOpenTime(long lastOpenTime) {
            this.lastOpenTime = lastOpenTime;
        }

        //endregion
    }
}


