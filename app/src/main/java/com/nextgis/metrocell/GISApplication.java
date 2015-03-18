package com.nextgis.metrocell;

import android.app.Application;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.text.TextUtils;
import android.widget.Toast;

import com.nextgis.maplib.api.IGISApplication;
import com.nextgis.maplib.location.GpsEventSource;
import com.nextgis.maplib.map.MapDrawable;
import com.nextgis.maplib.util.GeoConstants;
import com.nextgis.maplibui.mapui.RemoteTMSLayerUI;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class GISApplication extends Application implements IGISApplication {
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
    }

    public void onFirstRun() {
        String layerName = getString(R.string.osm);
        String layerURL = getString(R.string.osm_url);
        RemoteTMSLayerUI layer = new RemoteTMSLayerUI(getApplicationContext(), mMap.createLayerStorage());
        layer.setName(layerName);
        layer.setURL(layerURL);
        layer.setTMSType(GeoConstants.TMSTYPE_OSM);
        layer.setVisible(true);

        mMap.addLayer(layer);
        mMap.save();
        createMetroLinesLayer();
    }

    private void createMetroLinesLayer() {
        try {
            InputStream inputStream = getAssets().open("lines.geojson");

            if (inputStream.available() > 0) {
                //read all geojson
                BufferedReader streamReader = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"));
                StringBuilder responseStrBuilder = new StringBuilder();
                String inputStr;

                while ((inputStr = streamReader.readLine()) != null) {
                    responseStrBuilder.append(inputStr);
                }

                MetroVectorLayer layer = new MetroVectorLayer(mMap.getContext(), mMap.createLayerStorage());
                layer.setName("Metro lines");
                layer.setVisible(true);

                JSONObject geoJSONObject = new JSONObject(responseStrBuilder.toString());
                String errorMessage = layer.createFromGeoJSON(geoJSONObject);
                layer.reloadCache();

                if(TextUtils.isEmpty(errorMessage)) {
                    mMap.addLayer(layer);
                    mMap.save();
                }
            }
        } catch (JSONException | IOException | SecurityException e) {
            e.printStackTrace();
        }
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
        mMap = new MapDrawable(bkBitmap, this, mapFullPath, new MetroLayerFactory());
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
