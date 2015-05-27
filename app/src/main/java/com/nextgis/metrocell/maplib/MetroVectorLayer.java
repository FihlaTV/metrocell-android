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

package com.nextgis.metrocell.maplib;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.graphics.Color;

import com.nextgis.maplib.datasource.Feature;
import com.nextgis.maplib.datasource.Field;
import com.nextgis.maplib.datasource.GeoGeometry;
import com.nextgis.maplib.datasource.GeoGeometryFactory;
import com.nextgis.maplib.datasource.GeoLineString;
import com.nextgis.maplib.display.SimpleFeatureRenderer;
import com.nextgis.maplib.display.SimpleMarkerStyle;
import com.nextgis.maplib.display.SimplePolygonStyle;
import com.nextgis.maplib.map.MapBase;
import com.nextgis.maplib.map.MapContentProviderHelper;
import com.nextgis.maplib.map.VectorLayer;
import com.nextgis.maplib.util.Constants;
import com.nextgis.maplib.util.VectorCacheItem;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import static com.nextgis.maplib.util.Constants.NOT_FOUND;
import static com.nextgis.maplib.util.GeoConstants.CRS_WEB_MERCATOR;
import static com.nextgis.maplib.util.GeoConstants.CRS_WGS84;
import static com.nextgis.maplib.util.GeoConstants.FTDateTime;
import static com.nextgis.maplib.util.GeoConstants.FTInteger;
import static com.nextgis.maplib.util.GeoConstants.FTReal;
import static com.nextgis.maplib.util.GeoConstants.FTString;
import static com.nextgis.maplib.util.GeoConstants.GEOJSON_CRS;
import static com.nextgis.maplib.util.GeoConstants.GEOJSON_GEOMETRY;
import static com.nextgis.maplib.util.GeoConstants.GEOJSON_ID;
import static com.nextgis.maplib.util.GeoConstants.GEOJSON_NAME;
import static com.nextgis.maplib.util.GeoConstants.GEOJSON_PROPERTIES;
import static com.nextgis.maplib.util.GeoConstants.GEOJSON_TYPE;
import static com.nextgis.maplib.util.GeoConstants.GEOJSON_TYPE_FEATURES;
import static com.nextgis.maplib.util.GeoConstants.GTLineString;
import static com.nextgis.maplib.util.GeoConstants.GTMultiLineString;
import static com.nextgis.maplib.util.GeoConstants.GTMultiPoint;
import static com.nextgis.maplib.util.GeoConstants.GTMultiPolygon;
import static com.nextgis.maplib.util.GeoConstants.GTNone;
import static com.nextgis.maplib.util.GeoConstants.GTPoint;
import static com.nextgis.maplib.util.GeoConstants.GTPolygon;

public class MetroVectorLayer extends VectorLayer {
    private final static String FIELD_COLOR = "color";

    public MetroVectorLayer(Context context, File path) {
        super(context, path);
    }

    @Override
    public String createFromGeoJSON(JSONObject geoJSONObject) {
        try {
            //check crs
            boolean isWGS84 = true; //if no crs tag - WGS84 CRS
            if (geoJSONObject.has(GEOJSON_CRS)) {
                JSONObject crsJSONObject = geoJSONObject.getJSONObject(GEOJSON_CRS);
                //the link is unsupported yet.
                if (!crsJSONObject.getString(GEOJSON_TYPE).equals(GEOJSON_NAME)) {
                    return mContext.getString(com.nextgis.maplib.R.string.error_crs_unsupported);
                }
                JSONObject crsPropertiesJSONObject =
                        crsJSONObject.getJSONObject(GEOJSON_PROPERTIES);
                String crsName = crsPropertiesJSONObject.getString(GEOJSON_NAME);
                switch (crsName) {
                    case "urn:ogc:def:crs:OGC:1.3:CRS84":  // WGS84
                        isWGS84 = true;
                        break;
                    case "urn:ogc:def:crs:EPSG::3857":
                    case "EPSG:3857":  //Web Mercator
                        isWGS84 = false;
                        break;
                    default:
                        return mContext.getString(com.nextgis.maplib.R.string.error_crs_unsupported);
                }
            }

            //load contents to memory and reproject if needed
            JSONArray geoJSONFeatures = geoJSONObject.getJSONArray(GEOJSON_TYPE_FEATURES);
            if (0 == geoJSONFeatures.length()) {
                return mContext.getString(com.nextgis.maplib.R.string.error_empty_dataset);
            }

            List<Feature> features = new ArrayList<>();
            List<Field> fields = new ArrayList<>();

            int geometryType = GTNone;
            for (int i = 0; i < geoJSONFeatures.length(); i++) {
                JSONObject jsonFeature = geoJSONFeatures.getJSONObject(i);
                //get geometry
                JSONObject jsonGeometry = jsonFeature.getJSONObject(GEOJSON_GEOMETRY);
                GeoGeometry geometry = GeoGeometryFactory.fromJson(jsonGeometry);

                if (geometry instanceof GeoLineString)
                    geometry = new MetroGeoLineString((GeoLineString) geometry, MetroLineStyle.DEFAULT_COLOR);

                if (geometryType == GTNone) {
                    geometryType = geometry.getType();
                } else if (geometryType != geometry.getType()) {
                    //skip different geometry type
                    continue;
                }

                //reproject if needed
                if (isWGS84) {
                    geometry.setCRS(CRS_WGS84);
                    geometry.project(CRS_WEB_MERCATOR);
                } else {
                    geometry.setCRS(CRS_WEB_MERCATOR);
                }

                int nId = i;
                if (jsonFeature.has(GEOJSON_ID)) {
                    nId = jsonFeature.getInt(GEOJSON_ID);
                }
                Feature feature = new Feature(nId, fields); // ID == i
                feature.setGeometry(geometry);

                //normalize attributes
                JSONObject jsonAttributes = jsonFeature.getJSONObject(GEOJSON_PROPERTIES);
                Iterator<String> iter = jsonAttributes.keys();
                while (iter.hasNext()) {
                    String key = iter.next();
                    Object value = jsonAttributes.get(key);
                    int nType = NOT_FOUND;
                    //check type
                    if (value instanceof Integer || value instanceof Long) {
                        nType = FTInteger;
                    } else if (value instanceof Double || value instanceof Float) {
                        nType = FTReal;
                    } else if (value instanceof Date) {
                        nType = FTDateTime;
                    } else if (value instanceof String) {
                        nType = FTString;
                    } else if (value instanceof JSONObject) {
                        nType = NOT_FOUND;
                        //the some list - need to check it type FTIntegerList, FTRealList, FTStringList
                    }

                    if (nType != NOT_FOUND) {
                        int fieldIndex = NOT_FOUND;
                        for (int j = 0; j < fields.size(); j++) {
                            if (fields.get(j).getName().equals(key)) {
                                fieldIndex = j;
                                break;
                            }
                        }
                        if (fieldIndex == NOT_FOUND) { //add new field
                            Field field = new Field(nType, key, null);
                            fieldIndex = fields.size();
                            fields.add(field);
                        }
                        feature.setFieldValue(fieldIndex, value);
                    }
                }
                features.add(feature);
            }

            return initialize(fields, features, NOT_FOUND);
        } catch (JSONException | SQLiteException e) {
            e.printStackTrace();
            return e.getLocalizedMessage();
        }
    }

    @Override
    public void reloadCache() throws SQLiteException {
        //load vector cache
        MapContentProviderHelper map = (MapContentProviderHelper) MapBase.getInstance();
        SQLiteDatabase db = map.getDatabase(false);
        String[] columns = new String[]{Constants.FIELD_ID, Constants.FIELD_GEOM, FIELD_COLOR};
        Cursor cursor = db.query(mPath.getName(), columns, null, null, null, null, null);

        if (null != cursor) {
            if (cursor.moveToFirst()) {
                do {
                    try {
                        GeoGeometry geoGeometry = GeoGeometryFactory.fromBlob(cursor.getBlob(1));
                        if (null != geoGeometry) {
                            if (geoGeometry instanceof GeoLineString) {
                                int color = MetroLineStyle.DEFAULT_COLOR;

                                try {
                                    color = Color.parseColor(cursor.getString(2));
                                } catch (IllegalArgumentException ignored) {

                                }

                                geoGeometry = new MetroGeoLineString((GeoLineString) geoGeometry, color);
                            }

                            int nId = cursor.getInt(0);
                            mExtents.merge(geoGeometry.getEnvelope());
                            mVectorCacheItems.add(new VectorCacheItem(geoGeometry, nId));
                        }
                    } catch (IOException | ClassNotFoundException e) {
                        e.printStackTrace();
                    }
                } while (cursor.moveToNext());
            }

            cursor.close();
        }
    }

    @Override
    protected void setDefaultRenderer() {
        switch (mGeometryType) {
            case GTPoint:
            case GTMultiPoint:
                mRenderer = new SimpleFeatureRenderer(this, new SimpleMarkerStyle(Color.RED, Color.BLACK, 6, SimpleMarkerStyle.MarkerStyleCircle));
                break;
            case GTLineString:
            case GTMultiLineString:
                mRenderer = new MetroFeatureRenderer(this, new MetroLineStyle());
                break;
            case GTPolygon:
            case GTMultiPolygon:
                mRenderer = new SimpleFeatureRenderer(this, new SimplePolygonStyle(Color.MAGENTA));
                break;
            default:
                mRenderer = null;
        }
    }

    @Override
    protected void setRenderer(JSONObject jsonObject) throws JSONException {
        setDefaultRenderer();
    }

}
