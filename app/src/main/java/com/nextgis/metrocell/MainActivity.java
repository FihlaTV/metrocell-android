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

import org.osmdroid.ResourceProxy;
import org.osmdroid.tileprovider.tilesource.OnlineTileSourceBase;
import org.osmdroid.tileprovider.tilesource.XYTileSource;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.util.ResourceProxyImpl;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.ItemizedIconOverlay;
import org.osmdroid.views.overlay.OverlayItem;

import java.io.File;
import java.util.ArrayList;


public class MainActivity extends ActionBarActivity {
    private MapView mMapView;
    private ItemizedIconOverlay<OverlayItem> mCurrentLocationOverlay;
    private float mScaledDensity;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mMapView = (MapView) findViewById(R.id.map_view);
        mScaledDensity = getResources().getDisplayMetrics().scaledDensity;

        initializeMap();

        TelephonyManager telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);

        if (telephonyManager.getPhoneType() != TelephonyManager.PHONE_TYPE_GSM)
            return;

        GsmCellLocation cellLocation = (GsmCellLocation) telephonyManager.getCellLocation();

        if (cellLocation == null)
            return;

        File dbPath = new File(getExternalFilesDir(null), SQLiteDBHelper.DB_NAME + ".sqlite");
        SQLiteDatabase db = SQLiteDatabase.openDatabase(dbPath.getPath(), null, 0);

        String selection = SQLiteDBHelper.ROW_CID + " = ? and " + SQLiteDBHelper.ROW_LAC + " = ?";
        Cursor data = db.query(SQLiteDBHelper.TABLE_POINTS, new String[] {SQLiteDBHelper.ROW_LATITUDE, SQLiteDBHelper.ROW_LONGITUDE}, selection,
                new String[] {String.valueOf(cellLocation.getCid()), String.valueOf(cellLocation.getLac())}, null, null, null);

        if (data.moveToFirst()) {
            double currentLongitude = data.getDouble(0);
            double currentLatitude = data.getDouble(1);

            OverlayItem currentLocation = new OverlayItem("1", "Current location", "Description",
                    new GeoPoint(currentLatitude, currentLongitude));
            currentLocation.setMarker(getResources().getDrawable(R.drawable.abc_ic_clear_mtrl_alpha));
            currentLocation.setMarkerHotspot(OverlayItem.HotspotPlace.CENTER);

            ArrayList<OverlayItem> list = new ArrayList<>();
            list.add(currentLocation);
            mCurrentLocationOverlay = new ItemizedIconOverlay<>(list, null, new ResourceProxyImpl(this));
            mMapView.getOverlays().add(mCurrentLocationOverlay);
        }

        data.close();
//        dbHelper.close();
    }

    private void initializeMap() {
        setHardwareAccelerationOff();

        // For bug https://github.com/osmdroid/osmdroid/issues/49
        // "Tiles are too small on high dpi devices"
        // It is from sources of TileSourceFactory
        final int newScale = (int) (256 * mScaledDensity);
        OnlineTileSourceBase mapSource = new XYTileSource(
                "Mapnik",
                ResourceProxy.string.mapnik,
                0,
                18,
                newScale,
                ".png",
                new String[]{
                        "http://a.tile.openstreetmap.org/",
                        "http://b.tile.openstreetmap.org/",
                        "http://c.tile.openstreetmap.org/"});
        mMapView.setTileSource(mapSource);

        mMapView.setMultiTouchControls(true);
        mMapView.setBuiltInZoomControls(true);
        mMapView.getController().setCenter(new GeoPoint(55.7522200, 37.6155600));
        mMapView.getController().setZoom(9);
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
