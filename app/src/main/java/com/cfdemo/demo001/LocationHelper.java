package com.cfdemo.demo001;

import android.location.Location;
import android.util.Log;

import com.huawei.hms.location.LocationResult;
import com.huawei.hms.maps.HuaweiMap;
import com.huawei.hms.maps.model.LatLng;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

import static com.cfdemo.demo001.MapHelper.setMapTarget;

public class LocationHelper {
    private static final String TAG = "CFDemo001LogTag";

    public static void onLocationCallBack(LocationResult locationResult, HuaweiMap hMap) {
        if (locationResult != null) {
            List<Location> locations = locationResult.getLocations();
            if (!locations.isEmpty()) {
                for (Location location : locations) {
                    Log.i(TAG, "onLocationResult location[Latitude,Longitude,Accuracy]:" + location.getLatitude() + "," + location.getLongitude() + "," + location.getAccuracy());
                    MapHelper.setMapTarget(hMap, new LatLng(location.getLatitude(), location.getLongitude()));
                }
            }
        }
    }


    public static void onLocationReceived(String location,  HuaweiMap hMap) {
        LatLng coordinates;
        try {
            JSONObject locationData = new JSONObject(location);
            Log.d(TAG, "location data: " + locationData.getDouble("lat") + "," + locationData.getDouble("lng"));
            coordinates = new LatLng(locationData.getDouble("lat"), locationData.getDouble("lng"));

        } catch (JSONException e) {
            e.printStackTrace();
            coordinates = new LatLng(0,0);
        }
        MapHelper.setMapTarget(hMap, coordinates);
    }
}
