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
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.PorterDuff;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;

import com.melnykov.fab.FloatingActionButton;
import com.nextgis.maplib.datasource.GeoLineString;
import com.nextgis.maplib.datasource.GeoPoint;
import com.nextgis.maplib.util.GeoConstants;
import com.nextgis.maplibui.MapViewOverlays;
import com.nextgis.maplibui.util.SettingsConstantsUI;
import com.nextgis.metrocell.util.Constants;
import com.nextgis.metrocell.util.ConstantsSecured;
import com.nineoldandroids.view.ViewHelper;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;


public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    enum STATUS {STATUS_SEARCHING, STATUS_NOT_FOUND, STATUS_FOUND}

    private MapViewOverlays mMapView;
    CurrentCellLocationOverlay mCurrentCellLocationOverlay;

    private CellEngine mCellEngine;
    TelephonyManager mTelephonyManager;
//    CellListener mCellListener;

    private boolean mIsInterfaceLoaded = false;
    private SharedPreferences mSharedPreferences;

    private ImageView mImageViewStatus;
    private ProgressBar mProgressStatus;
    private FloatingActionButton mFAB;
//    private float mScaledDensity;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mFAB = (FloatingActionButton) findViewById(R.id.fab);
        mFAB.setOnClickListener(this);
        mProgressStatus = (ProgressBar) findViewById(R.id.pb_status);
        mProgressStatus.getIndeterminateDrawable().setColorFilter(getResources().getColor(R.color.pink), PorterDuff.Mode.SRC_IN);
        mImageViewStatus = (ImageView) findViewById(R.id.iv_status);
        ViewHelper.setAlpha(mImageViewStatus, 0.8f);

        mCellEngine = new CellEngine(this);
        mTelephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
//        mCellListener = new CellListener();

        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        mSharedPreferences.edit().remove(Constants.PREF_APP_SAVED_MAILS).commit();

        if (mSharedPreferences.getBoolean(Constants.PREF_APP_FIRST_RUN, true)) {
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

    private boolean checkOrCreateDatabase() {
        File dbPath = ((GISApplication) getApplication()).getDBPath();

        if (dbPath != null && (!dbPath.exists() || !dbPath.isFile())) {
            try {
                InputStream input = getAssets().open(SQLiteDBHelper.DB_NAME);
                OutputStream output = new FileOutputStream(dbPath);

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
//        mTelephonyManager.listen(mCellListener, PhoneStateListener.LISTEN_NONE);
        mCellEngine.onPause();
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mCellEngine.onResume();

        if (mSharedPreferences.getBoolean(SettingsConstantsUI.KEY_PREF_KEEPSCREENON, true))
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        else
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

//        mTelephonyManager.listen(mCellListener, PhoneStateListener.LISTEN_CELL_LOCATION);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_settings:
                ((GISApplication) getApplication()).showSettings();
                return true;
            case R.id.action_report:
                sendReport();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.fab:
                findCellLocation();
                break;
        }
    }

    private void findCellLocation() {
        setStatus(STATUS.STATUS_SEARCHING);

//            if (mTelephonyManager.getPhoneType() != TelephonyManager.PHONE_TYPE_GSM || !mIsInterfaceLoaded)
        if (!mIsInterfaceLoaded || !checkOrCreateDatabase())
            return;

        FindLocationInDB finder = new FindLocationInDB();
        finder.execute();
    }

    private void setStatus(STATUS status) {
        switch (status) {
            case STATUS_SEARCHING:
                mProgressStatus.setVisibility(View.VISIBLE);
                mImageViewStatus.setVisibility(View.GONE);
                break;
            case STATUS_NOT_FOUND:
                mProgressStatus.setVisibility(View.GONE);
                mImageViewStatus.setVisibility(View.VISIBLE);
                mImageViewStatus.setImageResource(R.drawable.ic_error_red_24dp);
                break;
            case STATUS_FOUND:
                mProgressStatus.setVisibility(View.GONE);
                mImageViewStatus.setVisibility(View.VISIBLE);
                mImageViewStatus.setImageResource(R.drawable.ic_success_green_24dp);
                break;
        }
    }

    private void sendReport() {
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("plain/text");
        intent.putExtra(Intent.EXTRA_EMAIL, ConstantsSecured.EMAIL_TO);
        intent.putExtra(Intent.EXTRA_SUBJECT, "Metrocell report");
        intent.putExtra(Intent.EXTRA_TEXT, mSharedPreferences.getString(Constants.PREF_APP_SAVED_MAILS, "No last known data"));

        startActivity(Intent.createChooser(intent, getString(R.string.action_report)));

//        Reporter reporter = new Reporter(this);
//        reporter.execute(data);
    }

    private class FindLocationInDB extends AsyncTask<Void, Void, Boolean> {
        private GeoPoint mCurrentPoint;
        private GeoLineString mGeoPosition;

        @Override
        protected Boolean doInBackground(Void... params) {
            boolean result = false;
            mCurrentCellLocationOverlay.setVisibility(false);
            mGeoPosition = new GeoLineString();
            mGeoPosition.setCRS(GeoConstants.CRS_WGS84);

            ArrayList<CellEngine.GSMInfo> gsmInfoArray = mCellEngine.getGSMInfoArray();
            CellEngine.GSMInfo activeCell = null;

            SQLiteDatabase db = SQLiteDatabase.openDatabase(((GISApplication) getApplication()).getDBPath().getPath(), null, 0);
            ArrayList<String> args = new ArrayList<>();
            String selection, where, payload = String.format("select %s, %s from %s where %s = ? and %s = ?",
                    SQLiteDBHelper.ROW_SEG_BEGIN, SQLiteDBHelper.ROW_SEG_END, SQLiteDBHelper.TABLE_POINTS, SQLiteDBHelper.ROW_LAC, SQLiteDBHelper.ROW_CID);

            selection = payload;
            where = "\r\n";

            for (CellEngine.GSMInfo gsmInfo : gsmInfoArray) {
                if (args.size() > 0)
                    selection += " intersect " + payload;

                args.add(gsmInfo.getLac() + "");
                args.add(gsmInfo.getCid() + "");

                String active = gsmInfo.isActive() ? "1" : gsmInfoArray.get(0).getMcc() + "-" + gsmInfoArray.get(0).getMnc() + "-"
                        + gsmInfoArray.get(0).getLac() + "-" + gsmInfoArray.get(0).getCid();
                where += "\r\n" + CellEngine.getItem(gsmInfo, active, "", "", "");

                if (gsmInfo.isActive()) {
                    activeCell = gsmInfo;
                }
            }

            mSharedPreferences.edit().putString(Constants.PREF_APP_SAVED_MAILS, where).commit();

            if (activeCell == null || activeCell.getLac() == -1 || activeCell.getCid() == -1 ||
                    activeCell.getLac() == 2147483647 || activeCell.getCid() == 2147483647)
                return false;

            Cursor item, data = db.rawQuery(selection, args.toArray(new String[args.size()]));
            Log.d(Constants.TAG, "sql intersections query: " + selection);
            Log.d(Constants.TAG, "sql intersections query args: " + TextUtils.join(",", args));

            if (data.moveToFirst()) {
                Log.d(Constants.TAG, "found bts intersections");
                ArrayList<MetroSegment> segmentsIds = new ArrayList<>();

                do {
                    segmentsIds.add(new MetroSegment(data.getInt(0), data.getInt(1)));
                } while (data.moveToNext());

                data.close();

                for (MetroSegment segment : segmentsIds) {
                    selection = "select max(mins), min(maxs) from (select max(ration) as maxs, min(ration) as mins from " + SQLiteDBHelper.TABLE_POINTS;
                    where = String.format(" where %s = ? and %s = ? and (", SQLiteDBHelper.ROW_SEG_BEGIN, SQLiteDBHelper.ROW_SEG_END);
                    payload = String.format("(%s = ? and %s = ?)", SQLiteDBHelper.ROW_LAC, SQLiteDBHelper.ROW_CID);
                    args.clear();
                    args.add(segment.getBeginSeg() + "");
                    args.add(segment.getEndSeg() + "");

                    for (CellEngine.GSMInfo gsmInfo : gsmInfoArray) {
                        if (args.size() >= 4)
                            where += " or ";

                        where += payload;
                        args.add(gsmInfo.getLac() + "");
                        args.add(gsmInfo.getCid() + "");
                    }

                    where += ")";
                    selection += where + " group by lac, cid)";

                    data = db.rawQuery(selection, args.toArray(new String[args.size()]));

                    if (data.moveToFirst()) {
                        Log.d(Constants.TAG, "segment " + segment.getBeginSeg() + "-" + segment.getEndSeg() +
                                " min: " + data.getString(0) + " max: " + data.getString(1));

                        args.add(data.getString(0));
                        args.add(data.getString(1));
                        selection = String.format("select %s, %s from %s%s and %s between ? and ?",
                                SQLiteDBHelper.ROW_LATITUDE, SQLiteDBHelper.ROW_LONGITUDE, SQLiteDBHelper.TABLE_POINTS, where, SQLiteDBHelper.ROW_RATIO);
                        item = db.rawQuery(selection, args.toArray(new String[args.size()]));

                        if (item.moveToFirst()) {
                            do {
                                mCurrentPoint = new GeoPoint(item.getDouble(0), item.getDouble(1));
                                mGeoPosition.add(mCurrentPoint);
                            } while (item.moveToNext());

                            result = true;
                        }

                        item.close();
                    }

                    data.close();
                }
            } else
                Log.d(Constants.TAG, "no bts intersections");

            if (mGeoPosition.getPointCount() == 0) {
                selection = SQLiteDBHelper.ROW_CID + " = ? and " + SQLiteDBHelper.ROW_LAC + " = ?";
                // and " + SQLiteDBHelper.ROW_RATIO + " between 0.1 and 0.9";
                data = db.query(SQLiteDBHelper.TABLE_POINTS, new String[]{SQLiteDBHelper.ROW_LATITUDE, SQLiteDBHelper.ROW_LONGITUDE},
                        selection, new String[]{activeCell.getCid() + "", activeCell.getLac() + ""}, null, null, null);

                if (data.moveToFirst()) {
                    Log.d(Constants.TAG, "found active bts only lac: " + activeCell.getLac() + " cid: " + activeCell.getCid());

                    do {
                        mCurrentPoint = new GeoPoint(data.getDouble(0), data.getDouble(1));
                        mGeoPosition.add(mCurrentPoint);
                    } while (data.moveToNext());

                    result = true;
                }

                data.close();
            }

            data.close();
            db.close();

            return result;
        }

        @Override
        protected void onPostExecute(Boolean result) {
            super.onPostExecute(result);

            if (result != null && result) {
                mCurrentCellLocationOverlay.setVisibility(true);
                mCurrentCellLocationOverlay.setNewCellLine(mGeoPosition);
                setStatus(STATUS.STATUS_FOUND);

                if (mCurrentPoint != null)
                    mMapView.panTo(mCurrentPoint);
            } else {
                setStatus(STATUS.STATUS_NOT_FOUND);
            }
        }
    }

    private class MetroSegment {
        public int getBeginSeg() {
            return mBeginSeg;
        }

        public int getEndSeg() {
            return mEndSeg;
        }

        private int mBeginSeg = -1, mEndSeg = -1;

        public MetroSegment(int beginSeg, int endSeg) {
            mBeginSeg = beginSeg;
            mEndSeg = endSeg;
        }
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
            edit.putBoolean(Constants.PREF_APP_FIRST_RUN, false);
            edit.commit();

            loadInterface();
            mProgressDialog.dismiss();
        }
    }
}
