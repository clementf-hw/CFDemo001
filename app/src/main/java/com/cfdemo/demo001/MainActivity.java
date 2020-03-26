package com.cfdemo.demo001;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import com.huawei.agconnect.config.AGConnectServicesConfig;
import com.huawei.hms.aaid.HmsInstanceId;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.huawei.hms.maps.CameraUpdate;
import com.huawei.hms.maps.CameraUpdateFactory;
import com.huawei.hms.maps.HuaweiMap;
import com.huawei.hms.maps.MapView;
import com.huawei.hms.maps.OnMapReadyCallback;
import com.huawei.hms.maps.model.LatLng;

import org.json.JSONException;
import org.json.JSONObject;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback {
    protected String pushToken;
    private static final String TAG = "CFDemo001LogTag";
    private static final int REQUEST_CODE = 316;
    private DemoHmsMessageService messageService;
    private HuaweiMap hMap;
    private MapView mMapView;
    private static final String[] RUNTIME_PERMISSIONS = {
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.INTERNET
    };
    private static final String MAPVIEW_BUNDLE_KEY = "MapViewBundleKey";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (!hasPermissions(this, RUNTIME_PERMISSIONS)) {
            ActivityCompat.requestPermissions(this, RUNTIME_PERMISSIONS, REQUEST_CODE);
        }
        Log.d(TAG, "Notification Enabled: " + NotificationManagerCompat.from(this).areNotificationsEnabled());
        //get mapview instance
        mMapView = findViewById(R.id.mapView);
        Bundle mapViewBundle = null;
        if (savedInstanceState != null) {
            mapViewBundle = savedInstanceState.getBundle(MAPVIEW_BUNDLE_KEY);
        }
        mMapView.onCreate(mapViewBundle);
        //get map instance
        mMapView.getMapAsync(this);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getToken();
        LocalBroadcastManager.getInstance(this).registerReceiver(messageListener,
                new IntentFilter("hmsMessageService"));
        String initialText =  NotificationManagerCompat.from(this).areNotificationsEnabled() ? "HMS Demo" : "Please enable notification for this app";
        showText(initialText);
    }

    @Override
    protected void onStart() {
        super.onStart();
        mMapView.onStart();
    }

    @Override
    protected void onStop() {
        super.onStop();
        mMapView.onStop();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mMapView.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mMapView.onResume();
    }

    @Override
    protected void onDestroy() {
        // Unregister since the activity is about to be closed.
        super.onDestroy();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(messageListener);
        mMapView.onDestroy();
    }

    private void getToken() {
        Log.d(TAG, "get token: begin");

        // get token
        new Thread() {
            @Override
            public void run() {
                try {
                    String appId = AGConnectServicesConfig.fromContext(MainActivity.this).getString("client/app_id");
                    pushToken = HmsInstanceId.getInstance(MainActivity.this).getToken(appId, "HCM");
                    if(!TextUtils.isEmpty(pushToken)) {
                        Log.d(TAG, "get token:" + pushToken);
                        onTokenReceived(pushToken);
                    }
                } catch (Exception e) {
                    Log.d(TAG,"getToken failed, " + e);
                }
            }
        }.start();
    }

    private boolean stringVerify (String input) {
        if (input == null || TextUtils.isEmpty(input)){
            return false;
        }
        return true;
    }

    private BroadcastReceiver messageListener = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent ) {
            String token = intent.getStringExtra("token");
            String data = intent.getStringExtra("data");
            if (stringVerify(token)) {
                Log.d(TAG, token);
                onTokenReceived(token);
            }
            if (stringVerify(data)) {
                Log.d(TAG, data);
                try {
                    JSONObject notificationData = new JSONObject(data);
                    String message = notificationData.has("message") ? notificationData.getString("message") : null;
                    if (stringVerify(message)) {
                        onMessageReceived(message);
                    }
                    String location = notificationData.has("location") ? notificationData.getString("location") : null;
                    if (stringVerify(location)) {
                        onLocationReceived(location);
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }
    };

    @Override
    public void onMapReady(HuaweiMap huaweiMap) {
        Log.d(TAG, "onMapReady: ");
        hMap = huaweiMap;
        // Specify whether to enable the compass.
        hMap.getUiSettings().setCompassEnabled(true);
        // Enable the my-location layer.
        hMap.setMyLocationEnabled(true);
        // Enable the function of displaying the my-location icon.
        hMap.getUiSettings().setMyLocationButtonEnabled(true);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String[] permissions, int[] grantResults) {
        Log.d(TAG, "onRequestPermissionsResult " + requestCode);
        switch (requestCode) {
            case REQUEST_CODE: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.d(TAG, "Permission Granted");
                    // permission was granted, yay! Do the
                    // contacts-related task you need to do.
                } else {
                    Log.d(TAG, "Permission Not Granted");
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                }
                return;
            }
            // other 'case' lines to check for other
            // permissions this app might request.
        }
    }

    private static boolean hasPermissions(Context context, String... permissions) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && permissions != null) {
            for (String permission : permissions) {
                if (ActivityCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                    return false;
                }
            }
        }
        return true;
    }

    private void showText(final String input) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                View txtLogView = findViewById(R.id.txt_display);
                if (txtLogView instanceof TextView) {
                    ((TextView) txtLogView).setText(input);
                    Toast.makeText(MainActivity.this, pushToken, Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void onTokenReceived(String token) {
        showText("Token: " + token);
    }

    private void onMessageReceived(String message) {
        showText("Message: " + message);
    }

    private void onLocationReceived(String location) {
        LatLng coordinates;
        try {
            JSONObject locationData = new JSONObject(location);
            Log.d(TAG, "location data: " + locationData.getDouble("lat") + "," + locationData.getDouble("lng"));
            coordinates = new LatLng(locationData.getDouble("lat"), locationData.getDouble("lng"));

        } catch (JSONException e) {
            e.printStackTrace();
            coordinates = new LatLng(0,0);
        }
        setMapTarget(coordinates);
    }

    private void setMapTarget(LatLng coordinates) {
        CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLng(coordinates);
        hMap.animateCamera(cameraUpdate);
    }
}
