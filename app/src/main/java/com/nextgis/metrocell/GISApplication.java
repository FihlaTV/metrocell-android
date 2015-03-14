package com.nextgis.metrocell;

import android.app.Application;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.preference.PreferenceManager;
import android.widget.Toast;

import com.nextgis.maplib.api.IGISApplication;
import com.nextgis.maplib.location.GpsEventSource;
import com.nextgis.maplib.map.MapDrawable;
import com.nextgis.maplib.util.GeoConstants;
import com.nextgis.maplibui.mapui.LayerFactoryUI;
import com.nextgis.maplibui.mapui.RemoteTMSLayerUI;

import java.io.File;

public class GISApplication extends Application implements IGISApplication {
    private static final String KEY_PREF_APP_FIRST_RUN = "is_first_run";
    private static final String AUTHORITY = "com.nextgis.metrocell";
    private static final String MAP_NAME = "default";
    private static final String MAP_EXT = ".ngm";

    private MapDrawable mMap;
    private GpsEventSource mGpsEventSource;

    @Override
    public void onCreate() {
        super.onCreate();

        mGpsEventSource = new GpsEventSource(this);

        getMap();
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        if (sharedPreferences.getBoolean(KEY_PREF_APP_FIRST_RUN, true)) {
            onFirstRun();
            SharedPreferences.Editor edit = sharedPreferences.edit();
            edit.putBoolean(KEY_PREF_APP_FIRST_RUN, false);
            edit.commit();
        }
    }

    private void onFirstRun() {
        String layerName = getString(R.string.osm);
        String layerURL = getString(R.string.osm_url);
        RemoteTMSLayerUI layer = new RemoteTMSLayerUI(getApplicationContext(), mMap.createLayerStorage());
        layer.setName(layerName);
        layer.setURL(layerURL);
        layer.setTMSType(GeoConstants.TMSTYPE_OSM);
        layer.setVisible(true);

        mMap.addLayer(layer);
        mMap.moveLayer(0, layer);
        mMap.save();
    }

    @Override
    public MapDrawable getMap() {
        if (null != mMap)
            return mMap;

        File defaultPath = getExternalFilesDir(null);

        if (defaultPath == null) {
            Toast.makeText(this, "External storage not available", Toast.LENGTH_SHORT).show();
            return null;
        }

        File mapFullPath = new File(defaultPath.getPath(), MAP_NAME + MAP_EXT);

        final Bitmap bkBitmap = BitmapFactory.decodeResource(getResources(), com.nextgis.maplibui.R.drawable.bk_tile);
        mMap = new MapDrawable(bkBitmap, this, mapFullPath, new LayerFactoryUI());
        mMap.setName(MAP_NAME);
        mMap.load();

        return mMap;
    }

    @Override
    public String getAuthority() {
        return AUTHORITY;
    }

    @Override
    public GpsEventSource getGpsEventSource() {
        return mGpsEventSource;
    }

    @Override
    public void showSettings() {

    }
}
