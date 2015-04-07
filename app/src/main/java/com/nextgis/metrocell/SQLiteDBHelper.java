/*
 * Project:  Metrocell
 * Purpose:  Locating in metro by cell towers
 * Author:   Stanislav Petriakov, becomeglory@gmail.com
 * *****************************************************************************
 * Copyright (c) 2015 NextGIS, info@nextgis.com
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.nextgis.metrocell;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class SQLiteDBHelper extends SQLiteOpenHelper {
    private final static int DB_VERSION = 1;
    public final static String DB_NAME = "log_points.sqlite";

    public final static String TABLE_POINTS = "log_points";

    public final static String ROW_LATITUDE = "x";
    public final static String ROW_LONGITUDE = "y";
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
