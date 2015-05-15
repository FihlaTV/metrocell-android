/*
 * Project:  Metrocell
 * Purpose:  Locating in metro by cell towers
 * Author:	 Stanislav Petriakov, becomeglory@gmail.com
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

import android.app.Application;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.widget.Toast;

import com.nextgis.maplib.api.IGISApplication;
import com.nextgis.maplib.api.ILayer;
import com.nextgis.maplib.location.GpsEventSource;
import com.nextgis.maplib.map.MapDrawable;
import com.nextgis.maplib.map.RemoteTMSLayer;
import com.nextgis.maplib.util.FileUtil;
import com.nextgis.maplib.util.GeoConstants;
import com.nextgis.metrocell.maplib.MetroLayerFactory;
import com.nextgis.metrocell.maplib.MetroVectorLayer;
import com.nextgis.metrocell.util.Constants;

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
    private static final String LAYER_LINES_NAME = "Metro lines";

    private MapDrawable mMap;
    private GpsEventSource mGpsEventSource;
    private SharedPreferences mSharedPreferences;

    @Override
    public void onCreate() {
        super.onCreate();

        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        updateApplicationStructure();

        mGpsEventSource = new GpsEventSource(this);
        getMap();
    }

    public void onFirstRun() {
        String layerName = getString(R.string.osm);
        String layerURL = getString(R.string.osm_url);
//        RemoteTMSLayerUI layer = new RemoteTMSLayerUI(getApplicationContext(), mMap.createLayerStorage());
        RemoteTMSLayer layer = new RemoteTMSLayer(getApplicationContext(), mMap.createLayerStorage());
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
                layer.setName(LAYER_LINES_NAME);
                layer.setVisible(true);

                JSONObject geoJSONObject = new JSONObject(responseStrBuilder.toString());
                String errorMessage = layer.createFromGeoJSON(geoJSONObject);
                layer.reloadCache();

                if (TextUtils.isEmpty(errorMessage)) {
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

    public File getDBPath() {
        if (getExternalFilesDir(null) != null)
            return new File(getExternalFilesDir(null), SQLiteDBHelper.DB_NAME);
        else
            return null;
    }

    @Override
    public void showSettings() {
        Intent preferences = new Intent(this, PreferencesActivity.class);
        preferences.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(preferences);
    }

    private void updateApplicationStructure() {
        try {
            int currentVersionCode = getPackageManager().getPackageInfo(getPackageName(), 0).versionCode;
            int savedVersionCode = mSharedPreferences.getInt(Constants.PREF_APP_VERSION, 0);

            switch (savedVersionCode) {
                case 0:
                    FileUtil.deleteRecursive(getDBPath());
                    break;
                case 2:
                case 3:
                    mSharedPreferences.edit().remove(Constants.PREF_APP_SAVED_MAILS).commit();
                    FileUtil.deleteRecursive(getDBPath());

                    ILayer metroLines = getMap().getLayerByName(LAYER_LINES_NAME);

                    if (metroLines != null)
                        getMap().moveLayer(0, metroLines);
                    else
                        createMetroLinesLayer();
                default:
                    break;
            }

            if (savedVersionCode < currentVersionCode) {
                mSharedPreferences.edit().putInt(Constants.PREF_APP_VERSION, currentVersionCode).commit();
            }
        } catch (PackageManager.NameNotFoundException ignored) {
        }
    }
}
