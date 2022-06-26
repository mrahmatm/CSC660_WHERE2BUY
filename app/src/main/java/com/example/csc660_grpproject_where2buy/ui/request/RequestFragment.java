package com.example.csc660_grpproject_where2buy.ui.request;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Looper;
import android.os.PersistableBundle;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.collection.ArraySet;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.csc660_grpproject_where2buy.R;
import com.example.csc660_grpproject_where2buy.databinding.FragmentRequestBinding;
import com.example.csc660_grpproject_where2buy.ui.map.MapFragment;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionDeniedResponse;
import com.karumi.dexter.listener.PermissionGrantedResponse;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.single.PermissionListener;

import java.util.Set;

public class RequestFragment extends Fragment implements OnMapReadyCallback {

    private FragmentRequestBinding binding;

    private TextView textView2, textView3, textView6;
    private EditText editText1;
    //private MapView mapView;
    private SeekBar seekBar;
    private Button button;

    private boolean isPermissionGranted;
    private SupportMapFragment supportMapFragment;
    private FusedLocationProviderClient client;
    private LatLng latLng;
    private MarkerOptions option;
    private Circle circle;
    private CircleOptions circleOption;

    String URL = "";

    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        RequestViewModel requestViewModel = new ViewModelProvider(this).get(RequestViewModel.class);

        binding = FragmentRequestBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        textView2 = binding.textView2;
        textView3 = binding.textView3;
        textView6 = binding.textView6;
        editText1 = binding.editTextTextPersonName;
        //mapView = binding.mapView;
        seekBar = binding.seekBar;
        button = binding.button;

        supportMapFragment = (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.mapView);

        client = LocationServices.getFusedLocationProviderClient(getContext());

        getCurrentLocation();

        //supportMapFragment.getMapAsync(this);

        return root;
    }

    private void getCurrentLocation() {
        checkMyPermission();
        Task<Location> task = client.getLastLocation();
        task.addOnSuccessListener(new OnSuccessListener<Location>() {
            @Override
            public void onSuccess(Location location) {
                if (location != null) {
                    supportMapFragment.getMapAsync(new OnMapReadyCallback() {
                        @Override
                        public void onMapReady(@NonNull GoogleMap googleMap) {
                            latLng = new LatLng(location.getLatitude(), location.getLongitude());
                            option = new MarkerOptions().position(latLng).icon(bitmapDescriptorFromVector(getContext().getApplicationContext(), R.drawable.ic_baseline_my_location_24_blue)).anchor(0.5f, 0.5f);;
//                            option = new MarkerOptions().position(latLng);
                            googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 12.9F), 10, null);
                            googleMap.addMarker(option);
                            googleMap.getUiSettings().setAllGesturesEnabled(false);

                            circleOption = new CircleOptions();

                            circle = googleMap.addCircle(circleOption.center(latLng).radius(1000).fillColor(Color.argb(50, 16, 21, 115)).strokeColor(Color.rgb(16, 21, 115)).strokeWidth(2f));

                            seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                                @RequiresApi(api = Build.VERSION_CODES.O)
                                @Override
                                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                                    seekBar.setMin(10);
                                    seekBar.setMax(1000);
                                    textView6.setText(progress/10 + " KM");
                                    if (progress <= 20) {
                                        googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 12.9F), 10, null);
                                    }
                                    else if (progress > 21 && progress <= 30) {
                                        googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 12.7F), 10, null);
                                    }
                                    else if (progress > 31 && progress <= 40) {
                                        googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 12.5F), 10, null);
                                    }
                                    else if (progress > 41 && progress <= 50) {
                                        googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 12.3F), 10, null);
                                    }
                                    else if (progress > 51 && progress <= 60) {
                                        googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 12.1F), 10, null);
                                    }
                                    else if (progress > 61 && progress <= 70) {
                                        googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 11.9F), 10, null);
                                    }
                                    else if (progress > 71 && progress <= 80) {
                                        googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 11.7F), 10, null);
                                    }
                                    else if (progress > 81 && progress <= 90) {
                                        googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 11.5F), 10, null);
                                    }
                                    else if (progress > 91 && progress <= 140) {
                                        googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 11), 10, null);
                                    }
                                    else if (progress > 141 && progress <= 190) {
                                        googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 10.5F), 10, null);
                                    }
                                    else if (progress > 191 && progress <= 240) {
//                                        googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 10));
                                        googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 10), 10, null);
                                    }
                                    else if (progress > 241 && progress <= 490) {
                                        googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 9), 10, null);
                                    }
                                    else if (progress > 491) {
                                        googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 8), 10, null);
                                    }
                                    circle.remove();
                                    circle = googleMap.addCircle(circleOption.center(latLng).radius((progress*100)).fillColor(Color.argb(50, 16, 21, 115)).strokeColor(Color.rgb(16, 21, 115)).strokeWidth(2f));

                                }

                                @Override
                                public void onStartTrackingTouch(SeekBar seekBar) {

                                }

                                @Override
                                public void onStopTrackingTouch(SeekBar seekBar) {

                                }
                            });
                        }
                    });
                }
            }
        });
    }

    private void checkMyPermission() {
        Dexter.withContext(getContext()).withPermission(Manifest.permission.ACCESS_FINE_LOCATION).withListener(new PermissionListener() {
            @Override
            public void onPermissionGranted(PermissionGrantedResponse permissionGrantedResponse) {
                //Toast.makeText(getContext(), "Permission Granted", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onPermissionDenied(PermissionDeniedResponse permissionDeniedResponse) {
                Intent intent = new Intent();
                intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                Uri uri = Uri.fromParts("package", getContext().getPackageName(), "");
                intent.setData(uri);
                startActivity(intent);
            }

            @Override
            public void onPermissionRationaleShouldBeShown(PermissionRequest permissionRequest, PermissionToken permissionToken) {
                permissionToken.continuePermissionRequest();
            }
        }).check();
    }

    private BitmapDescriptor bitmapDescriptorFromVector(Context context, int vectoriID){
        Drawable vectorDrawable  = ContextCompat.getDrawable(context, vectoriID);
        vectorDrawable.setBounds(0, 0, vectorDrawable.getIntrinsicWidth(), vectorDrawable.getIntrinsicHeight());
        Bitmap bitmap=Bitmap.createBitmap(vectorDrawable.getIntrinsicWidth(), vectorDrawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);

        vectorDrawable.draw(canvas);
        return BitmapDescriptorFactory.fromBitmap(bitmap);
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {

    }
}