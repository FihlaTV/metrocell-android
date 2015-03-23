package com.nextgis.metrocell.maplib;

import com.nextgis.maplib.display.SimpleFeatureRenderer;
import com.nextgis.maplib.display.SimpleMarkerStyle;
import com.nextgis.maplib.display.SimplePolygonStyle;
import com.nextgis.maplib.display.Style;
import com.nextgis.maplib.map.Layer;

import org.json.JSONException;
import org.json.JSONObject;

import static com.nextgis.maplib.util.Constants.JSON_NAME_KEY;

public class MetroFeatureRenderer extends SimpleFeatureRenderer {
    public MetroFeatureRenderer(Layer layer, Style style) {
        super(layer, style);
    }

    @Override
    public void fromJSON(JSONObject jsonObject)
            throws JSONException
    {
        JSONObject styleJsonObject = jsonObject.getJSONObject(JSON_STYLE_KEY);
        String styleName = styleJsonObject.getString(JSON_NAME_KEY);
        switch (styleName)
        {
            case "SimpleMarkerStyle":
                mStyle = new SimpleMarkerStyle();
                mStyle.fromJSON(styleJsonObject);
                break;
            case "SimpleLineStyle":
                mStyle = new MetroLineStyle();
                mStyle.fromJSON(styleJsonObject);
                break;
            case "SimplePolygonStyle":
                mStyle = new SimplePolygonStyle();
                mStyle.fromJSON(styleJsonObject);
                break;
        }
    }
}
