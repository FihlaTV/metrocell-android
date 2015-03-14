package com.nextgis.metrocell;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class SQLiteDBHelper extends SQLiteOpenHelper {
    private final static int DB_VERSION = 1;
    public final static String DB_NAME = "log_points";

    public final static String TABLE_POINTS = "log_points";

    public final static String ROW_LATITUDE = "xcoord";
    public final static String ROW_LONGITUDE = "ycoord";
    public final static String ROW_LAC = "lac";
    public final static String ROW_CID = "cid";
    public final static String ROW_PSC = "psc";

    public SQLiteDBHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
//        if(android.os.Build.VERSION.SDK_INT >= 17){
//            DB_PATH = context.getApplicationInfo().dataDir + "/databases/";
//        }
//        else
//        {
//            DB_PATH = "/data/data/" + context.getPackageName() + "/databases/";
//        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }
}
