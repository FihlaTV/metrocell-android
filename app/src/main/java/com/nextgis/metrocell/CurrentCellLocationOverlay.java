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

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.LightingColorFilter;
import android.graphics.Paint;
import android.graphics.PointF;

import com.nextgis.maplib.datasource.GeoLineString;
import com.nextgis.maplib.datasource.GeoPoint;
import com.nextgis.maplib.map.MapDrawable;
import com.nextgis.maplib.util.GeoConstants;
import com.nextgis.maplibui.MapViewOverlays;
import com.nextgis.maplibui.api.Overlay;
import com.nextgis.maplibui.api.OverlayItem;

import java.util.List;

public class CurrentCellLocationOverlay extends Overlay {
    OverlayItem mMarker;
    double mLat, mLong;
    boolean mIsVisible = false;
    List<GeoPoint> mPoints;
    Paint mPaint;
    int mWidth = 6;

    public CurrentCellLocationOverlay(Context context, MapViewOverlays mapViewOverlays, GeoPoint initialPosition) {
        super(context, mapViewOverlays);

        Bitmap marker = BitmapFactory.decodeResource(context.getResources(), R.drawable.ic_location).copy(Bitmap.Config.ARGB_8888, true);

        mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
//        int color = context.getResources().getColor(R.color.pink);
        int color = Color.MAGENTA;
//        ColorFilter filter = new LightingColorFilter(color, 1);
//        mPaint.setColorFilter(filter);
        mPaint.setColor(color);
//        Canvas canvas = new Canvas(marker);
//        canvas.drawBitmap(marker, 0, 0, mPaint);
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setStrokeCap(Paint.Cap.ROUND);

        mLat = initialPosition.getY();
        mLong = initialPosition.getX();
        mMarker = new OverlayItem(mapViewOverlays.getMap(), initialPosition, marker);
    }

    public void setNewCellLine(GeoLineString line) {
        mPoints = line.getPoints();
        line.project(GeoConstants.CRS_WEB_MERCATOR);
        mMapViewOverlays.postInvalidate();
    }

    public void setNewCellPoint(double longitude, double latitude) {
        mLong = longitude;
        mLat = latitude;
        mMapViewOverlays.postInvalidate();
    }

    public void setVisibility(boolean visibility) {
        mIsVisible = visibility;
    }

    @Override
    public void draw(Canvas canvas, MapDrawable mapDrawable) {
        if (mIsVisible) {
            mPaint.setStrokeWidth((float) (mWidth));
            GeoPoint x0 = mapDrawable.mapToScreen(mPoints.get(0)), x1;

            for (int i = 1; i < mPoints.size(); i++) {
                x1 = mapDrawable.mapToScreen(mPoints.get(i - 1));
                canvas.drawLine((float) x0.getX(), (float) x0.getY(), (float) x1.getX(), (float) x1.getY(), mPaint);
                x0 = x1;
            }

//            mMarker.setCoordinates(mLong, mLat);
//            drawOverlayItem(canvas, mMarker);
        }
    }

    @Override
    public void drawOnPanning(Canvas canvas, PointF currentMouseOffset) {
        if (mIsVisible) {
            drawOnPanning(canvas, currentMouseOffset, mMarker);
        }
    }

    @Override
    public void drawOnZooming(Canvas canvas, PointF currentFocusLocation, float scale) {
        if (mIsVisible) {
            drawOnZooming(canvas, currentFocusLocation, scale, mMarker, false);
        }
    }
}
