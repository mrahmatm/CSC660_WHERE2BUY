package com.example.csc660_grpproject_where2buy.ui.map;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentManager;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;

import com.example.csc660_grpproject_where2buy.R;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;

public class ViewRespondLocation extends AppCompatActivity {
    private static final String MAPVIEW_BUNDLE_KEY = "AIzaSyCy7mWzBosyA5FFiDb39J7c4Orp0SatpEM";
    SupportMapFragment mapFragment;
    private FusedLocationProviderClient client;
    GoogleMap mMap;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_respond_location2);
        FragmentManager myFragmentManager = getSupportFragmentManager();
        mapFragment = (SupportMapFragment) myFragmentManager.findFragmentById(R.id.respondLocationMap);
        client = LocationServices.getFusedLocationProviderClient(this);
        //getCurrentLocation();

        Bundle bundle = getIntent().getExtras();
        String val = bundle.getString("position");
        Log.d("?test map", "onCreate, location: "+val);

        mapFragment.getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(@NonNull GoogleMap googleMap) {
                mMap = googleMap;
                //Log.d("gcl?current location: ", currentLocation.toString());
                //currentLocation= new LatLng(location.getLatitude(), location.getLongitude());
                //option = new MarkerOptions().position(currentLocation).icon(bitmapDescriptorFromVector(getContext().getApplicationContext(), R.drawable.ic_baseline_my_location_24_blue)).anchor(0.5f, 0.5f);;
//                            option = new MarkerOptions().position(latLng);
                //googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLocation, 12.9F), 10, null);
                //googleMap.addMarker(option);
                googleMap.getUiSettings().setAllGesturesEnabled(true);

            }
        });


   }

}