package com.nextgis.metrocell;

import android.annotation.TargetApi;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.telephony.TelephonyManager;
import android.telephony.gsm.GsmCellLocation;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;

import com.nextgis.maplib.datasource.GeoEnvelope;
import com.nextgis.maplib.datasource.GeoPoint;
import com.nextgis.maplib.util.Constants;
import com.nextgis.maplib.util.GeoConstants;
import com.nextgis.maplibui.MapViewOverlays;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;


public class MainActivity extends ActionBarActivity {
    private MapViewOverlays mMapView;
    CurrentCellLocationOverlay mCurrentCellLocationOverlay;
//    private float mScaledDensity;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mMapView = new MapViewOverlays(this, ((GISApplication) getApplication()).getMap());

        ((FrameLayout) findViewById(R.id.map_view)).addView(mMapView, 0, new FrameLayout.LayoutParams(
                RelativeLayout.LayoutParams.MATCH_PARENT,
                RelativeLayout.LayoutParams.MATCH_PARENT));
//        mScaledDensity = getResources().getDisplayMetrics().scaledDensity;

        initializeMap();

        TelephonyManager telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);

        if (telephonyManager.getPhoneType() != TelephonyManager.PHONE_TYPE_GSM)
            return;

        GsmCellLocation cellLocation = (GsmCellLocation) telephonyManager.getCellLocation();

        if (cellLocation == null)
            return;

        File dbPath = new File(getExternalFilesDir(null), SQLiteDBHelper.DB_NAME);

        if (!checkOrCreateDatabase(dbPath))
            return;

        SQLiteDatabase db = SQLiteDatabase.openDatabase(dbPath.getPath(), null, 0);

        String selection = SQLiteDBHelper.ROW_CID + " = ? and " + SQLiteDBHelper.ROW_LAC + " = ?";
        Cursor data = db.query(SQLiteDBHelper.TABLE_POINTS, new String[] {SQLiteDBHelper.ROW_LATITUDE, SQLiteDBHelper.ROW_LONGITUDE}, selection,
                new String[] {String.valueOf(cellLocation.getCid()), String.valueOf(cellLocation.getLac())}, null, null, null);

        if (data.moveToFirst()) {
            double currentLongitude = data.getDouble(0);
            double currentLatitude = data.getDouble(1);

            mCurrentCellLocationOverlay =
                    new CurrentCellLocationOverlay(this, mMapView, new GeoPoint(currentLongitude, currentLatitude));

            mMapView.addOverlay(mCurrentCellLocationOverlay);
        }

        data.close();
        db.close();
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
}
