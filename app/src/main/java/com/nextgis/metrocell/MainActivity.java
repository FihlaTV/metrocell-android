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

import android.annotation.TargetApi;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBarActivity;
import android.telephony.CellLocation;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.telephony.gsm.GsmCellLocation;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;

import com.nextgis.maplib.datasource.GeoLineString;
import com.nextgis.maplib.datasource.GeoPoint;
import com.nextgis.maplib.util.GeoConstants;
import com.nextgis.maplibui.MapViewOverlays;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;


public class MainActivity extends ActionBarActivity {
    private static final String KEY_PREF_APP_FIRST_RUN = "is_first_run";

    private MapViewOverlays mMapView;
    CurrentCellLocationOverlay mCurrentCellLocationOverlay;
    TelephonyManager mTelephonyManager;
    CellListener mCellListener;
    private boolean mIsInterfaceLoaded = false;
//    private float mScaledDensity;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mTelephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        mCellListener = new CellListener();

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        if (sharedPreferences.getBoolean(KEY_PREF_APP_FIRST_RUN, true)) {
            new FirstRunTask(this).execute();
            return;
        }

        loadInterface();
    }

    private void loadInterface() {
        mMapView = new MapViewOverlays(this, ((GISApplication) getApplication()).getMap());

        ((FrameLayout) findViewById(R.id.map_view)).addView(mMapView, 0, new FrameLayout.LayoutParams(
                RelativeLayout.LayoutParams.MATCH_PARENT,
                RelativeLayout.LayoutParams.MATCH_PARENT));
//        mScaledDensity = getResources().getDisplayMetrics().scaledDensity;

        initializeMap();

        mCurrentCellLocationOverlay = new CurrentCellLocationOverlay(this, mMapView, new GeoPoint(0, 0));
        mMapView.addOverlay(mCurrentCellLocationOverlay);

        mIsInterfaceLoaded = true;
    }

    private boolean checkOrCreateDatabase(File path) {
        if (!path.exists() || !path.isFile()) {
            try {
                InputStream input = getAssets().open(SQLiteDBHelper.DB_NAME);
                OutputStream output = new FileOutputStream(path);

                byte[] buffer = new byte[1024];
                int length;
                while ((length = input.read(buffer)) > 0) {
                    output.write(buffer, 0, length);
                }

                output.flush();
                output.close();
                input.close();

                return true;
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }
        }

        return true;
    }

    private void initializeMap() {
        setHardwareAccelerationOff();

        GeoPoint center = new GeoPoint(37.6155600, 55.7522200);
        center.setCRS(GeoConstants.CRS_WGS84);
        center.project(GeoConstants.CRS_WEB_MERCATOR);
        mMapView.setZoomAndCenter(12, center);
//        mMapView.getMap().setLimits(new GeoEnvelope(55.4843,55.8976,37.9193,37.2272), Constants.MAP_LIMITS_XY);
//        mMapView.getMap().setLimits(new GeoEnvelope(56.1272,55.3041,36.8893,38.4109), Constants.MAP_LIMITS_XY);
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    private void setHardwareAccelerationOff() {
        // Turn off hardware acceleration here, or in manifest
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
            mMapView.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
    }

    @Override
    protected void onPause() {
        mTelephonyManager.listen(mCellListener, PhoneStateListener.LISTEN_NONE);
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mTelephonyManager.listen(mCellListener, PhoneStateListener.LISTEN_CELL_LOCATION);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private class FirstRunTask extends AsyncTask<Context, Void, Void> {
        private ProgressDialog mProgressDialog;
        private Context mContext;

        public FirstRunTask(Context context) {
            mContext = context;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            mProgressDialog = new ProgressDialog(mContext);
            mProgressDialog.setIndeterminate(true);
            mProgressDialog.setCancelable(false);
            mProgressDialog.setMessage(getString(R.string.first_run));
            mProgressDialog.setTitle(getString(R.string.first_run_title));
            mProgressDialog.show();
        }

        @Override
        protected Void doInBackground(Context... params) {
            ((GISApplication) getApplication()).onFirstRun();

            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);

            SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(mContext);
            SharedPreferences.Editor edit = sharedPreferences.edit();
            edit.putBoolean(KEY_PREF_APP_FIRST_RUN, false);
            edit.commit();

            loadInterface();
            mProgressDialog.dismiss();
        }
    }

    private class CellListener extends PhoneStateListener {
        @Override
        public void onCellLocationChanged(CellLocation location) {
            if (mTelephonyManager.getPhoneType() != TelephonyManager.PHONE_TYPE_GSM || !mIsInterfaceLoaded)
                return;

            GsmCellLocation cellLocation = (GsmCellLocation) mTelephonyManager.getCellLocation();
            File dbPath = new File(getExternalFilesDir(null), SQLiteDBHelper.DB_NAME);

            if (!isCellLocationValid(cellLocation) || !checkOrCreateDatabase(dbPath))
                return;

            SQLiteDatabase db = SQLiteDatabase.openDatabase(dbPath.getPath(), null, 0);
            String selection = SQLiteDBHelper.ROW_CID + " = ? and " + SQLiteDBHelper.ROW_LAC + " = ?";
            Cursor data = db.query(SQLiteDBHelper.TABLE_POINTS, new String[] {SQLiteDBHelper.ROW_LATITUDE, SQLiteDBHelper.ROW_LONGITUDE}, selection,
                    new String[] {String.valueOf(cellLocation.getCid()), String.valueOf(cellLocation.getLac())}, null, null, null);
//                    new String[] {String.valueOf(382), String.valueOf(770)}, null, null, null);

            if (data.moveToFirst()) {
                GeoLineString geoPosition = new GeoLineString();
                geoPosition.setCRS(GeoConstants.CRS_WGS84);
                geoPosition.add(new GeoPoint(data.getDouble(0), data.getDouble(1)));

                while (data.moveToNext()) {
                    geoPosition.add(new GeoPoint(data.getDouble(0), data.getDouble(1)));
                }

//                geoPosition.project(GeoConstants.CRS_WEB_MERCATOR);

//                double currentLongitude = data.getDouble(0);
//                double currentLatitude = data.getDouble(1);

                mCurrentCellLocationOverlay.setVisibility(true);
//                mCurrentCellLocationOverlay.setNewCellPoint(currentLongitude, currentLatitude);
                mCurrentCellLocationOverlay.setNewCellLine(geoPosition);
            } else
                mCurrentCellLocationOverlay.setVisibility(false);

            data.close();
            db.close();
        }

        private boolean isCellLocationValid(GsmCellLocation cellLocation) {
            return cellLocation != null && (cellLocation.getLac() > 0 && cellLocation.getCid() > 0);
        }
    }
}
