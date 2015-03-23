package com.nextgis.metrocell.maplib;

import android.graphics.Color;
import android.graphics.Paint;

import com.nextgis.maplib.datasource.GeoLineString;
import com.nextgis.maplib.datasource.GeoPoint;
import com.nextgis.maplib.display.GISDisplay;
import com.nextgis.maplib.display.SimpleLineStyle;

import java.util.List;

public class MetroLineStyle extends SimpleLineStyle {
    public final static int DEFAULT_COLOR = Color.DKGRAY;

    public MetroLineStyle() {
        super();
        mWidth = 4;
    }

    @Override
    public void onDraw(GeoLineString lineString, GISDisplay display) {
        Paint lnPaint = new Paint();

        lnPaint.setColor(((MetroGeoLineString) lineString).getColor());
        lnPaint.setStrokeWidth((float) (mWidth / display.getScale()));
        lnPaint.setStrokeCap(Paint.Cap.ROUND);
        lnPaint.setAntiAlias(true);

        List<GeoPoint> points = lineString.getPoints();
        for (int i = 1; i < points.size(); i++) {
            display.drawLine((float) points.get(i-1).getX(), (float) points.get(i-1).getY(),
                    (float) points.get(i).getX(), (float) points.get(i).getY(), lnPaint);
        }
    }
}
