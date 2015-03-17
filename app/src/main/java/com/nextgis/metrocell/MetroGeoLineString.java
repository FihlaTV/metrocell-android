package com.nextgis.metrocell;

import android.graphics.Color;

import com.nextgis.maplib.datasource.GeoLineString;
import com.nextgis.maplib.datasource.GeoPoint;

import java.util.ArrayList;

public class MetroGeoLineString extends GeoLineString {
    private int mColor = MetroLineStyle.DEFAULT_COLOR;

//    public MetroGeoLineString()
//    {
//        mPoints = new ArrayList<>();
//    }

    public MetroGeoLineString(GeoLineString geoLineString, int color)
    {
        mPoints = new ArrayList<>();
        for(GeoPoint point : geoLineString.getPoints()){
            mPoints.add((GeoPoint) point.copy());
        }

        mColor = color;
    }

    public void setColor(int color) {
         mColor = color;
    }
    public int getColor() {
        return mColor;
    }
}
