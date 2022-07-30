package com.example.csc660_grpproject_where2buy.ui.map;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.example.csc660_grpproject_where2buy.MainActivity;
import com.example.csc660_grpproject_where2buy.R;
import com.example.csc660_grpproject_where2buy.RequestsNearby;
import com.example.csc660_grpproject_where2buy.databinding.FragmentMapBinding;
import com.example.csc660_grpproject_where2buy.databinding.FragmentRequestBinding;
import com.example.csc660_grpproject_where2buy.map.MapViewModel;
import com.example.csc660_grpproject_where2buy.ui.respond.ListViewRespond;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.snackbar.Snackbar;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionDeniedResponse;
import com.karumi.dexter.listener.PermissionGrantedResponse;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.single.PermissionListener;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MapFragment extends Fragment implements OnMapReadyCallback {
    private static final String MAPVIEW_BUNDLE_KEY = "AIzaSyCy7mWzBosyA5FFiDb39J7c4Orp0SatpEM";

    private FragmentMapBinding binding;
    private View root;
    private SupportMapFragment supportMapFragment;
    private FusedLocationProviderClient client;
    private String userID;

    LatLng latLng;
    private MarkerOptions option, newMarker;
    LatLng currentLocation;

    GoogleMap mMap;
    private Marker currentMarker;

    JSONArray resultArray;


    public View onCreateView(@NonNull LayoutInflater inflater,
                                 ViewGroup container, Bundle savedInstanceState) {
        com.example.csc660_grpproject_where2buy.map.MapViewModel mapViewModel =
                new ViewModelProvider(this).get(MapViewModel.class);

        binding = FragmentMapBinding.inflate(inflater, container, false);
        userID = MainActivity.getUserId();
        root = binding.getRoot();
        supportMapFragment = (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.nearbySearchMap);
        client = LocationServices.getFusedLocationProviderClient(getContext());
        getCurrentLocation();
        //Log.d("?current location: ", latLng.toString());
        mQueue = Volley.newRequestQueue(getContext());
        //putMarkers();
        return root;
    }


    @Override
    public void onMapReady(GoogleMap map) {
        if (ActivityCompat.checkSelfPermission(getActivity(), Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(getActivity(), Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            return;
        }
        map.setMyLocationEnabled(true);
    }

    private void getCurrentLocation() {
        checkMyPermission();
        Task<Location> task = client.getLastLocation();
        task.addOnSuccessListener(new OnSuccessListener<Location>() {
            @Override
            public void onSuccess(Location location) {
                if (location != null) {
                    currentLocation = new LatLng(location.getLatitude(), location.getLongitude());
                    jsonParse();

                    //putMarkers();
                    supportMapFragment.getMapAsync(new OnMapReadyCallback() {
                        @Override
                        public void onMapReady(@NonNull GoogleMap googleMap) {
                            mMap = googleMap;
                            Log.d("gcl?current location: ", currentLocation.toString());
                            //currentLocation= new LatLng(location.getLatitude(), location.getLongitude());
                            option = new MarkerOptions().position(currentLocation).icon(bitmapDescriptorFromVector(getContext().getApplicationContext(), R.drawable.ic_baseline_my_location_24_blue)).anchor(0.5f, 0.5f);;
//                            option = new MarkerOptions().position(latLng);
                            googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLocation, 12.9F), 10, null);
                            googleMap.addMarker(option);
                            googleMap.getUiSettings().setAllGesturesEnabled(true);

                        }
                    });
                }

            }
        });
        //return  latLng;
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

    private TextView mTextViewResult;
    private RequestQueue mQueue;

    private void jsonParse() {
       // Log.d("?current location: ", latLng.toString());
        String url = "http://csc660.allprojectcs270.com/getRequestLocation.php";

        StringRequest request = new StringRequest(Request.Method.POST, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        try {
                            JSONObject rows = new JSONObject(response);
                            JSONObject statusJson = rows.getJSONObject("status");
                            String statusCode = statusJson.getString("statusCode");
                            Log.d("?RESULT STATUS CODE: ", statusCode);

                            JSONArray jsonArray = rows.getJSONArray("result");
                            resultArray = jsonArray;
                            //putMarkers(jsonArray);
                            if (jsonArray.length() <= 0){
                                Log.d("?RESULT STATUS: ", "THERE'S NO RESULT");
                                showSnackbar(0, "There are no requests nearby");
                            }
                            for (int i = 0; i < jsonArray.length(); i++) {
                                JSONObject request = jsonArray.getJSONObject(i);

                                String requestID = request.getString("requestID");
                                String displayName = request.getString("requesterName");
                                String itemName = request.getString("itemName");
                                //String distanceFromCenter = request.getString("distance_in_km");
                                String requestDate = request.getString("requestDate");
                                String areaCenterLat = request.getString("areaCenterLat");
                                String areaCenterLng = request.getString("areaCenterLng");
                                String imageBase64 = request.getString("imageBase64");


                                //mTextViewResult.append(firstName + ", " + String.valueOf(age) + ", " + mail + "\n\n");
                                Log.d("?resultofFetch: ", requestID+", "+displayName);
                            }
                            putMarkers();
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                error.printStackTrace();
            }
        }){
            @Override
            protected Map<String, String> getParams(){
                Map<String, String> params = new HashMap<>();

                Log.d("jreq?current location: ", currentLocation.toString());
                params.put("responderLat", String.valueOf(currentLocation.latitude));
                params.put("responderLng", String.valueOf(currentLocation.longitude));
                return params;
            }
        };

        if(request != null)
            mQueue.add(request);
        else
            Log.e("?RESPONSE", "stringRequest is null");

        //putMarkers();

    }

    private void putMarkers(){
        //jsonParse();
        int n = 0;
        LatLng currentPosition;
        String currentLat, currentLng;
        while(n < resultArray.length()){
            try {
                currentLat = resultArray.getJSONObject(n).getString("areaCenterLat");
                currentLng= resultArray.getJSONObject(n).getString("areaCenterLng");
                currentPosition = new LatLng(Double.parseDouble(currentLat), Double.parseDouble(currentLng));
                newMarker = new MarkerOptions().position(currentPosition).snippet(resultArray.getJSONObject(n).getString("requestID")).icon(bitmapDescriptorFromVector(getContext().getApplicationContext(), R.drawable.where2buy_icon_noshadow_tiny)).anchor(0.5f, 0.5f);
                //newMarker;
            } catch (JSONException e) {
                e.printStackTrace();
            }
            currentMarker = mMap.addMarker(newMarker);
            n++;
        }
        mMap.addMarker(newMarker);
        mMap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(@NonNull Marker marker) {
                Intent intent = new Intent(getContext(), ViewRequest.class);

                String currentSnippet = marker.getSnippet();
                Log.d("?test", "?snippet dalam event: "+ currentSnippet);
                intent.putExtra("id", currentSnippet);
                intent.putExtra("currentUser", userID);

                startActivity(intent);
                return false;
            }
        });
    }

    /*
    @Override
    public Boolean onMarkerClick(final Marker marker){
        showSnackbar(0, "clicked!");

        return false;
    }*/

    private void showSnackbar(int mode, String message){
        switch(mode){
            case 1:{ // success snackbar
                Snackbar.make(root, message, Snackbar.LENGTH_LONG).setBackgroundTint(getResources().getColor(com.google.android.libraries.places.R.color.quantum_googgreen)).show();
                break;
            } case 0:{ // info/neutral snackbar
                Snackbar.make(root, message, Snackbar.LENGTH_SHORT).show();
                break;
            } case -1:{ // warn snackbar
                Snackbar.make(root, message, Snackbar.LENGTH_LONG).setBackgroundTint(getResources().getColor(R.color.custom_orange_700)).show();
                break;
            } case -2:{ // error snackbar
                Snackbar.make(root, message, Snackbar.LENGTH_LONG).setBackgroundTint(getResources().getColor(R.color.custom_error_red)).show();
                break;
            }
        }
    }
}