package com.example.mover_f;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import android.Manifest;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.inputmethodservice.InputMethodService;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import com.example.mover_f.R;
import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.firebase.geofire.GeoQuery;
import com.firebase.geofire.GeoQueryEventListener;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;


import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

public class customerMapsActivity extends AppCompatActivity implements OnMapReadyCallback{
    private static final int PERMISSION_REQUEST_CODE = 9001;
    private static final int PLAY_SERVICES_ERROR_CODE = 9002;
    private static final int GPS_REQUEST_CODE = 9003;
    boolean mLocationPermissionGranted;
    private GoogleMap mGoogleMap;
    private Button mBtnLocate;
    private EditText mSearchAddress;
   // private Button mCurrent_Location;
    double bottomBoundary = 28.546315;
    double leftBoundary = 77.054215;
    double topBoundary = 28.733739;
    double rightBoundary = 77.479120;
    public static final int DEFAULT_ZOOM = 17;
    private FusedLocationProviderClient mLocationClient;
    private LocationCallback mLocationCallback;
    private LatLng pickupLocation;
    private List<Polyline> polylines = null;
    private Button requestRide;
    private long backPressedTime;
    private Toast backToast;
    Marker mDriverMarker = null;
    private Boolean requestBol = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_customer_maps);
        mSearchAddress = findViewById(R.id.et_address);
        mBtnLocate = findViewById(R.id.btn_locate);
        mBtnLocate.setOnClickListener(this::geoLocate);



        requestRide = findViewById(R.id.requestRide);
        requestRide.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (requestBol){
                    requestBol = false;
                    geoQuery.removeAllListeners();
                    driverLocationRef.removeEventListener(driverLocationRefListener);
                    if (driverFoundID  != null){
                        DatabaseReference driverRef = FirebaseDatabase.getInstance().getReference().child("Users").child("Drivers").child(driverFoundID);
                        driverRef.setValue(true);
                        driverFoundID = null;
                    }
                    driverFound = false;
                    radius = 1;
                    String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();

                    DatabaseReference ref = FirebaseDatabase.getInstance().getReference("customerRequest");
                    GeoFire geoFire = new GeoFire(ref);
                    geoFire.removeLocation(userId);

                    if(mMarker != null){
                        mMarker.remove();
                    }
                    requestRide.setText("LOOK FOR MOVERS");


                }else{  requestBol = true;
                if (ActivityCompat.checkSelfPermission(customerMapsActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(customerMapsActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

                    return;
                }
                mLocationClient.getLastLocation().addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Location location = task.getResult();


                        //showMarker(location.getLatitude(), location.getLongitude());
                        gotoLocation(location.getLatitude(), location.getLongitude());
                        pickupLocation = new LatLng(location.getLatitude(), location.getLongitude());
                        Log.d("MAP_DEBUG", "pickup Location " + pickupLocation);
                        if (mDriverMarker != null) {
                            mDriverMarker.remove();
                        }
                        mDriverMarker = mGoogleMap.addMarker(new MarkerOptions().position(pickupLocation).title("You are Here"));
                        getCurrentLocation();
                        getLocationUpdate();
                        getClosestDriver();




                    }
                });
            }}

        });



        SupportMapFragment mapFragment =
                (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map_fragment);
        mapFragment.getMapAsync(this);


        initGoogleMap();

        mLocationClient = new FusedLocationProviderClient(this);
        mLocationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null) {
                    return;
                }
                Location location = locationResult.getLastLocation();
                //Toast.makeText(DriverMapsActivity.this, "LocationResult: Location is " + location.getLatitude() + "," + location.getLongitude(), Toast.LENGTH_SHORT).show();
                Log.d("MAP_DEBUG", "onLocationResult: Location is " + location.getLatitude() + " ," + location.getLongitude());

                //Send to Database
                String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
                DatabaseReference ref = FirebaseDatabase.getInstance().getReference("customerRequest");

                GeoFire geoFire = new GeoFire(ref);
                geoFire.setLocation(userId, new GeoLocation(location.getLatitude(), location.getLongitude()));




            }
        };


    }

    // FIND NEAREST DRIVERS

    private int radius = 1;
    private Boolean driverFound = false;
    private String driverFoundID;
    private GeoQuery geoQuery;
    private void getClosestDriver(){
        DatabaseReference driverLocation = FirebaseDatabase.getInstance().getReference().child("driversAvailable");

        GeoFire geoFire = new GeoFire(driverLocation);
        geoQuery = geoFire.queryAtLocation(new GeoLocation(pickupLocation.latitude, pickupLocation.longitude), radius);
        geoQuery.removeAllListeners();

        geoQuery.addGeoQueryEventListener(new GeoQueryEventListener() {
            @Override
            public void onKeyEntered(String key, GeoLocation location) {
                if (!driverFound && requestBol){
                    driverFound = true;
                    driverFoundID = key;

                    DatabaseReference driverRef = FirebaseDatabase.getInstance().getReference().child("Users").child("Drivers").child(driverFoundID);
                    String customerId = FirebaseAuth.getInstance().getCurrentUser().getUid();
                    HashMap map = new HashMap();
                    map.put("customerRideId", customerId);
                    driverRef.updateChildren(map);

                    getDriverLocation();
                    requestRide.setText("Looking for Driver Location....");

                }
            }

            @Override
            public void onKeyExited(String key) {

            }

            @Override
            public void onKeyMoved(String key, GeoLocation location) {

            }

            @Override
            public void onGeoQueryReady() {
                if (!driverFound)
                {
                    radius++;
                    getClosestDriver();
                }
            }

            @Override
            public void onGeoQueryError(DatabaseError error) {

            }
        });
    }
    Marker mMarker;
    DatabaseReference driverLocationRef;
    private ValueEventListener driverLocationRefListener;
    private void getDriverLocation(){
        driverLocationRef = FirebaseDatabase.getInstance().getReference().child("driversWorking").child(driverFoundID).child("l");
       driverLocationRefListener= driverLocationRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if(dataSnapshot.exists() && requestBol){
                    List<Object> map = (List<Object>) dataSnapshot.getValue();
                    double locationLat = 0;
                    double locationLng = 0;
                    requestRide.setText("Driver Found");
                    if(map.get(0) != null){
                        locationLat = Double.parseDouble(map.get(0).toString());
                    }
                    if(map.get(1) != null){
                        locationLng = Double.parseDouble(map.get(1).toString());
                    }
                    LatLng driverLatLng = new LatLng(locationLat,locationLng);
                    if(mMarker != null){
                        mMarker.remove();
                    }
                    Location loc1 = new Location("");
                    loc1.setLatitude(pickupLocation.latitude);
                    loc1.setLongitude(pickupLocation.longitude);

                    Location loc2 = new Location("");
                    loc2.setLatitude(driverLatLng.latitude);
                    loc2.setLongitude(driverLatLng.longitude);

                    float distance = loc1.distanceTo(loc2);

                    if (distance<100){
                        requestRide.setText("Driver is HERE");
                    }else{
                        requestRide.setText("Driver Found" + " "+ distance +"m Away");
                    }


                    if(mMarker != null){
                        mMarker.remove();
                    }
                    mMarker =mGoogleMap.addMarker(new MarkerOptions().position(driverLatLng).title("your driver"));


                }

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
            }
        });

    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mLocationCallback != null) {
            mLocationClient.removeLocationUpdates(mLocationCallback);
        }
    }

    private void getLocationUpdate() {

        LocationRequest locationRequest = LocationRequest.create();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequest.setInterval(5000);
        locationRequest.setFastestInterval(2000);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            return;
        }
        mLocationClient.requestLocationUpdates(locationRequest, mLocationCallback, null);



    }

    @Override
    public void onBackPressed() {

        if (backPressedTime + 2000 > System.currentTimeMillis()) {
            backToast.cancel();
            super.onBackPressed();
            return;
        } else {
            backToast = Toast.makeText(getBaseContext(), "Press back again to exit", Toast.LENGTH_SHORT);
            backToast.show();
        }
        backPressedTime = System.currentTimeMillis();
    }

    @Override
    protected void onStop() {
        super.onStop();

        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("customerRequest");

        GeoFire geoFire = new GeoFire(ref);
        geoFire.removeLocation(userId);
    }

    private void getCurrentLocation() {

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            return;
        }
        mLocationClient.getLastLocation().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Location location = task.getResult();
                //showMarker(location.getLatitude(), location.getLongitude());
                if(mDriverMarker != null){
                    mDriverMarker.remove();
                }
                mDriverMarker = mGoogleMap.addMarker(new MarkerOptions().position(new LatLng(location.getLatitude(), location.getLongitude())).title("You are Here"));
                gotoLocation(location.getLatitude(), location.getLongitude());
                pickupLocation= new LatLng(location.getLatitude(), location.getLongitude());
                Log.d("MAP_DEBUG", "pickup Location " + pickupLocation);


            }

        });


    }

    private void geoLocate(View view) {
        hideSoftKeyboard(view);

        String locationName = mSearchAddress.getText().toString();

        Geocoder geocoder = new Geocoder(this, Locale.getDefault());

        try {
            List<Address> addressList = geocoder.getFromLocationName(locationName, 3);

            if (addressList.size() > 0) {
                Address address = addressList.get(0);

                gotoLocation(address.getLatitude(), address.getLongitude());

                showMarker(address.getLatitude(), address.getLongitude());

                // Toast.makeText(this, address.getLocality(), Toast.LENGTH_SHORT).show();

                Log.d("MAP_DEBUG", "geoLocate: Locality: " + address.getLocality());
            }

            for (Address address : addressList) {
                Log.d("MAP_DEBUG", "geoLocate: Address: " + address.getAddressLine(address.getMaxAddressLineIndex()));
            }


        } catch (IOException e) {


        }


    }


    private void showMarker(double lat, double lng) {
        MarkerOptions markerOptions = new MarkerOptions();
        markerOptions.position(new LatLng(lat, lng));
        mGoogleMap.addMarker(markerOptions);
    }

    private void gotoLocation(double lat, double lng) {

        LatLng latLng = new LatLng(lat, lng);

        CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(latLng, DEFAULT_ZOOM);

        mGoogleMap.moveCamera(cameraUpdate);
        mGoogleMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);


    }


    private void hideSoftKeyboard(View view) {
        InputMethodManager imm = (InputMethodManager) getSystemService
                (INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }




    private void initGoogleMap() {

        if (isServicesOk()) {
            if (isGPSEnabled()) {
                if (checkLocationPermission()) {
                    //Toast.makeText(this, "Ready to Map", Toast.LENGTH_SHORT).show();

                    SupportMapFragment supportMapFragment = (SupportMapFragment) getSupportFragmentManager()
                            .findFragmentById(R.id.map_fragment);

                    supportMapFragment.getMapAsync(this);
                } else {
                    requestLocationPermission();
                }
            }
        }
    }

    private boolean checkLocationPermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED;
    }

    private void requestLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSION_REQUEST_CODE);
            }
        }
    }

    private boolean isServicesOk() {

        GoogleApiAvailability googleApi = GoogleApiAvailability.getInstance();

        int result = googleApi.isGooglePlayServicesAvailable(this);

        if (result == ConnectionResult.SUCCESS) {
            return true;
        } else if (googleApi.isUserResolvableError(result)) {
            Dialog dialog = googleApi.getErrorDialog(this, result, PLAY_SERVICES_ERROR_CODE, task ->
                    Toast.makeText(this, "Dialog is cancelled by User", Toast.LENGTH_SHORT).show());
            dialog.show();
        } else {
            Toast.makeText(this, "Play services are required by this application", Toast.LENGTH_SHORT).show();
        }
        return false;
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == PERMISSION_REQUEST_CODE && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            mLocationPermissionGranted = true;
            Toast.makeText(this, "Permission granted", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Permission not granted", Toast.LENGTH_SHORT).show();
        }
    }

    private boolean isGPSEnabled() {

        LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

        boolean providerEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);

        if (providerEnabled) {
            return true;
        } else {

            AlertDialog alertDialog = new AlertDialog.Builder(this)
                    .setTitle("GPS Permissions")
                    .setMessage("GPS is required for this app to work. Please enable GPS.")
                    .setPositiveButton("Yes", ((dialogInterface, i) -> {
                        Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                        startActivityForResult(intent, GPS_REQUEST_CODE);
                    }))
                    .setCancelable(false)
                    .show();




        }

        return false;
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == GPS_REQUEST_CODE) {

            LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

            boolean providerEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);

            if (providerEnabled) {
                Toast.makeText(this, "GPS is enabled", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "GPS not enabled. Unable to show user location", Toast.LENGTH_SHORT).show();
            }
        }
    }


    @Override
    public void onMapReady(GoogleMap googleMap) {
        Log.d("MAP DEBUG", "onMapReady: Map is Showing");

        mGoogleMap = googleMap;
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            return;
        }

        mLocationClient.getLastLocation().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Location location = task.getResult();

                gotoLocation(location.getLatitude(), location.getLongitude());



            }

        });



    }


}





