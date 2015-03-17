package com.nextgis.metrocell;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import com.nextgis.maplib.api.ILayer;
import com.nextgis.maplib.map.LayerFactory;
import com.nextgis.maplib.map.LayerGroup;
import com.nextgis.maplib.map.LocalTMSLayer;
import com.nextgis.maplib.map.NGWRasterLayer;
import com.nextgis.maplib.map.NGWVectorLayer;
import com.nextgis.maplib.map.RemoteTMSLayer;
import com.nextgis.maplib.map.TrackLayer;
import com.nextgis.maplib.map.VectorLayer;
import com.nextgis.maplib.util.FileUtil;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;

import static com.nextgis.maplib.util.Constants.CONFIG;
import static com.nextgis.maplib.util.Constants.JSON_TYPE_KEY;
import static com.nextgis.maplib.util.Constants.LAYERTYPE_GROUP;
import static com.nextgis.maplib.util.Constants.LAYERTYPE_LOCAL_TMS;
import static com.nextgis.maplib.util.Constants.LAYERTYPE_LOCAL_VECTOR;
import static com.nextgis.maplib.util.Constants.LAYERTYPE_NGW_RASTER;
import static com.nextgis.maplib.util.Constants.LAYERTYPE_NGW_VECTOR;
import static com.nextgis.maplib.util.Constants.LAYERTYPE_REMOTE_TMS;
import static com.nextgis.maplib.util.Constants.LAYERTYPE_TRACKS;
import static com.nextgis.maplib.util.Constants.TAG;

public class MetroLayerFactory extends LayerFactory {
    @Override
    public ILayer createLayer(
            Context context,
            File path)
    {
        File config_file = new File(path, CONFIG);
        ILayer layer = null;

        try {
            String sData = FileUtil.readFromFile(config_file);
            JSONObject rootObject = new JSONObject(sData);
            int nType = rootObject.getInt(JSON_TYPE_KEY);

            switch (nType) {
                case LAYERTYPE_REMOTE_TMS:
                    layer = new RemoteTMSLayer(context, path);
                    break;
                case LAYERTYPE_LOCAL_VECTOR:
                    layer = new MetroVectorLayer(context, path);
                    break;
                case LAYERTYPE_GROUP:
                    layer = new LayerGroup(context, path, this);
                    break;
                case LAYERTYPE_NGW_RASTER:
                case LAYERTYPE_NGW_VECTOR:
                case LAYERTYPE_LOCAL_TMS:
                case LAYERTYPE_TRACKS:
                    break;
            }
        } catch (IOException | JSONException e) {
            Log.d(TAG, e.getLocalizedMessage());
        }

        return layer;
    }

    @Override
    public void createNewRemoteTMSLayer(Context context, LayerGroup groupLayer) {

    }

    @Override
    public void createNewNGWLayer(Context context, LayerGroup groupLayer) {

    }

    @Override
    public void createNewLocalTMSLayer(Context context, LayerGroup groupLayer, Uri uri) {

    }

    @Override
    public void createNewVectorLayer(Context context, LayerGroup groupLayer, Uri uri) {

    }

    @Override
    public void createNewVectorLayerWithForm(Context context, LayerGroup groupLayer, Uri uri) {

    }
}
