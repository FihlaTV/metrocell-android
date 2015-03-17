package com.nextgis.metrocell;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.LightingColorFilter;
import android.graphics.Paint;
import android.graphics.PointF;

import com.nextgis.maplib.datasource.GeoPoint;
import com.nextgis.maplib.map.MapDrawable;
import com.nextgis.maplibui.MapViewOverlays;
import com.nextgis.maplibui.api.Overlay;
import com.nextgis.maplibui.api.OverlayItem;

public class CurrentCellLocationOverlay extends Overlay {
    OverlayItem mMarker;
    double mLat, mLong;
    boolean mIsVisible = false;

    public CurrentCellLocationOverlay(Context context, MapViewOverlays mapViewOverlays, GeoPoint initialPosition) {
        super(context, mapViewOverlays);

        Bitmap marker = BitmapFactory.decodeResource(context.getResources(), R.drawable.ic_location).copy(Bitmap.Config.ARGB_8888, true);
        Paint paint = new Paint();
        ColorFilter filter = new LightingColorFilter(context.getResources().getColor(R.color.accent), 1);
        paint.setColorFilter(filter);
        Canvas canvas = new Canvas(marker);
        canvas.drawBitmap(marker, 0, 0, paint);

        mLat = initialPosition.getY();
        mLong = initialPosition.getX();
        mMarker = new OverlayItem(mapViewOverlays.getMap(), initialPosition, marker);
    }

    public void setNewCellData(double longitude, double latitude) {
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
            mMarker.setCoordinates(mLong, mLat);
            drawOverlayItem(canvas, mMarker);
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
