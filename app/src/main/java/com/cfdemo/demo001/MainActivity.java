package com.cfdemo.demo001;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentSender;

import android.os.Bundle;
import com.huawei.agconnect.config.AGConnectServicesConfig;
import com.huawei.hmf.tasks.OnFailureListener;
import com.huawei.hmf.tasks.OnSuccessListener;
import com.huawei.hms.aaid.HmsInstanceId;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import androidx.core.app.NotificationManagerCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;

import com.huawei.hms.ads.AdParam;
import com.huawei.hms.ads.BannerAdSize;
import com.huawei.hms.ads.HwAds;
import com.huawei.hms.ads.banner.BannerView;
import com.huawei.hms.common.ApiException;
import com.huawei.hms.common.ResolvableApiException;
import com.huawei.hms.hmsscankit.ScanUtil;
import com.huawei.hms.location.FusedLocationProviderClient;
import com.huawei.hms.location.LocationCallback;
import com.huawei.hms.location.LocationRequest;
import com.huawei.hms.location.LocationResult;
import com.huawei.hms.location.LocationServices;
import com.huawei.hms.location.LocationSettingsRequest;
import com.huawei.hms.location.LocationSettingsResponse;
import com.huawei.hms.location.LocationSettingsStatusCodes;
import com.huawei.hms.location.SettingsClient;
import com.huawei.hms.maps.HuaweiMap;
import com.huawei.hms.maps.MapView;
import com.huawei.hms.maps.OnMapReadyCallback;
import com.huawei.hms.ml.scan.HmsScan;
import com.huawei.hms.ml.scan.HmsScanAnalyzerOptions;

import org.json.JSONException;
import org.json.JSONObject;

import io.branch.referral.Branch;
import io.branch.referral.BranchError;

import static com.cfdemo.demo001.LocationHelper.onLocationCallBack;
import static com.cfdemo.demo001.LocationHelper.onLocationReceived;
import static com.cfdemo.demo001.PermissionHelper.checkPermissions;
import static com.cfdemo.demo001.PermissionHelper.onPermissionGranted;
import static com.cfdemo.demo001.UtilHelper.showText;
import static com.cfdemo.demo001.UtilHelper.stringVerify;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback {
    protected String pushToken;
    private static final String TAG = "CFDemo001LogTag";
    private HuaweiMap hMap;
    private MapView mMapView;
    private static final String MAPVIEW_BUNDLE_KEY = "MapViewBundleKey";
    private FusedLocationProviderClient fusedLocationProviderClient;
    private LocationRequest mLocationRequest;
    private static final int REQUEST_CODE_SCAN = 000567; // change this code to fit your need

    private LocationCallback mLocationCallback = new LocationCallback() {
        @Override
        public void onLocationResult(LocationResult locationResult) {
            onLocationCallBack(locationResult, hMap);
        }
    };
    private Activity activityReference = this;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        checkPermissions(this);
        HwAds.init(this);

        initMapView(savedInstanceState);
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
        getToken();

        LocalBroadcastManager.getInstance(this).registerReceiver(messageListener,
                new IntentFilter("hmsMessageService"));
        Log.d(TAG, "Notification Enabled: " + NotificationManagerCompat.from(this).areNotificationsEnabled());
        String initialText =  NotificationManagerCompat.from(this).areNotificationsEnabled() ? "Waiting for push token..." : "Please enable notification for this app";
        showText(this, initialText);

        loadBannerAd();
    }

    @Override
    protected void onStart() {
        super.onStart();
        mMapView.onStart();
        Branch.getInstance().initSession(branchReferralInitListener, this.getIntent().getData(), this);
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
        removeLocationUpdatesWithCallback();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        //receive result after your activity finished scanning
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_OK || data == null) {
            return;
        }
        // Obtain the return value of HmsScan from the value returned by the onActivityResult method by using ScanUtil.RESULT as the key value.
        if (requestCode == REQUEST_CODE_SCAN) {
            Object obj = data.getParcelableExtra(ScanUtil.RESULT);
            if (obj instanceof HmsScan) {
                if (!TextUtils.isEmpty(((HmsScan) obj).getOriginalValue())) {
                    try {
                        JSONObject codeData = new JSONObject(((HmsScan) obj).getOriginalValue());
                        handleData(codeData);
                    } catch (Exception e) {
                        Log.d(TAG, ""+e);
                    }
                }
                return;
            }
        }
    }


    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String[] permissions, int[] grantResults) {
        Log.d(TAG, "onRequestPermissionsResult " + requestCode);
        onPermissionGranted(requestCode, permissions, grantResults);
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
                    handleData(notificationData);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }
    };

    private Branch.BranchReferralInitListener branchReferralInitListener = new Branch.BranchReferralInitListener() {
        @Override
        public void onInitFinished(JSONObject linkProperties, BranchError error) {
            // do stuff with deep link data (nav to page, display content, etc)
            if (error == null) {
                Log.i(TAG, "BRANCH SDK Success " + linkProperties.toString());
                handleData(linkProperties);
            } else {
                Log.i(TAG, "BRANCH SDK Error " + error.getMessage());
            }
        }
    };

    private void initMapView(Bundle savedInstanceState) {
        mMapView = findViewById(R.id.mapView);
        Bundle mapViewBundle = null;
        if (savedInstanceState != null) {
            mapViewBundle = savedInstanceState.getBundle(MAPVIEW_BUNDLE_KEY);
        }
        mMapView.onCreate(mapViewBundle);
        //get map instance
        mMapView.getMapAsync(this);
    }

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

    private void onTokenReceived(String token) {
        showText(this,"Token: " + token);
        requestLocationUpdatesWithCallback();
    }

    private void handleData (JSONObject data) {
        try {
            String message = data.has("message") ? data.getString("message") : null;
            if (stringVerify(message)) {
                onMessageReceived(message);
            }
            String location = data.has("location") ? data.getString("location") : null;
            if (stringVerify(location)) {
                onLocationReceived(location, hMap);
            } else {
                Boolean myLocation = data.has("my_location") ? data.getBoolean("my_location") : null;
                if (myLocation) {
                    Log.d(TAG, "Set map to my location");
                    requestLocationUpdatesWithCallback();
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void onMessageReceived(String message) {
        showText(this,"Message: " + message);
    }

    private void requestLocationUpdatesWithCallback () {
        SettingsClient settingsClient = LocationServices.getSettingsClient(this);
        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder();
        mLocationRequest = new LocationRequest();
        builder.addLocationRequest(mLocationRequest);
        LocationSettingsRequest locationSettingsRequest = builder.build();
        //check Location Settings
        settingsClient.checkLocationSettings(locationSettingsRequest)
                .addOnSuccessListener(new OnSuccessListener<LocationSettingsResponse>() {
                    @Override
                    public void onSuccess(LocationSettingsResponse locationSettingsResponse) {
                        //Have permissions， send requests
                        fusedLocationProviderClient
                                .requestLocationUpdates(mLocationRequest, mLocationCallback, Looper.getMainLooper())
                                .addOnSuccessListener(new OnSuccessListener<Void>() {
                                    @Override
                                    public void onSuccess(Void aVoid) {
                                        Log.d(TAG, "Request Location Success");
                                        //Interface call successfully processed
                                    }
                                });
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(Exception e) {
                        //Settings do not meet targeting criteria
                        Log.d(TAG, "Request location failed");
                        showText(activityReference,"Location Request failed");
                        int statusCode = ((ApiException) e).getStatusCode();
                        switch (statusCode) {
                            case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                                try {
                                    ResolvableApiException rae = (ResolvableApiException) e;
                                    //Calling startResolutionForResult can pop up a window to prompt the user to open the corresponding permissions
                                    rae.startResolutionForResult(MainActivity.this, 0);
                                } catch (IntentSender.SendIntentException sie) {
                                    //…
                                }
                                break;
                        }
                    }
                });
    }

    private void removeLocationUpdatesWithCallback() {
        try {
            fusedLocationProviderClient.removeLocationUpdates(mLocationCallback)
                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            Log.i(TAG, "removeLocationUpdatesWithCallback onSuccess");
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(Exception e) {
                            Log.e(TAG, "removeLocationUpdatesWithCallback onFailure:" + e.getMessage());
                        }
                    });
        } catch (Exception e) {
            Log.e(TAG, "removeLocationUpdatesWithCallback exception:" + e.getMessage());
        }
    }

    private void loadBannerAd () {
        BannerView bannerView = new BannerView(this);
        // "testw6vs28auh3" is a dedicated test ad slot ID.
        bannerView.setAdId("testw6vs28auh3");
        bannerView.setBannerAdSize(BannerAdSize.BANNER_SIZE_320_50);
        FrameLayout adFrameLayout = findViewById(R.id.ad_frame);
        adFrameLayout.addView(bannerView);
        AdParam adParam = new AdParam.Builder().build();
        bannerView.loadAd(adParam);
    }

    public void onScanCode(View view) {
        ScanUtil.startScan(MainActivity.this, REQUEST_CODE_SCAN, new HmsScanAnalyzerOptions.Creator().setHmsScanTypes(HmsScan.ALL_SCAN_TYPE).create());
    }
}
