package com.cfdemo.demo001;

import com.huawei.hms.maps.CameraUpdate;
import com.huawei.hms.maps.CameraUpdateFactory;
import com.huawei.hms.maps.HuaweiMap;
import com.huawei.hms.maps.model.LatLng;


public class MapHelper {

    public static void setMapTarget(HuaweiMap hMap, LatLng coordinates) {
        CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLng(coordinates);
        hMap.animateCamera(cameraUpdate);
    }
}
