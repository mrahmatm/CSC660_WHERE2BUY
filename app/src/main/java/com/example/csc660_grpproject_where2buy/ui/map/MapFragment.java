package com.example.csc660_grpproject_where2buy.ui.map;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
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
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.example.csc660_grpproject_where2buy.MainActivity;
import com.example.csc660_grpproject_where2buy.R;
import com.example.csc660_grpproject_where2buy.RequestsNearby;
import com.example.csc660_grpproject_where2buy.databinding.FragmentMapBinding;
import com.example.csc660_grpproject_where2buy.map.MapViewModel;
import com.example.csc660_grpproject_where2buy.ui.respond.ListViewRespond;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
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
import java.util.Map;

public class MapFragment extends Fragment implements OnMapReadyCallback {
    private static final String MAPVIEW_BUNDLE_KEY = "AIzaSyCy7mWzBosyA5FFiDb39J7c4Orp0SatpEM";
    private FragmentMapBinding binding;
    String userName, userID;
    private LatLng latLng;
    SupportMapFragment supportMapFragment;
    private FusedLocationProviderClient client;
    private MarkerOptions option;
    private final String url = "http://csc660.allprojectcs270.com/getMyRequestList.php";
    private String itemName, imageBase64, statusCode, statusMessage;
    private int requestID, responderID;
    private TextView itemNameText;
    private Bitmap capturedImageBitmap, requestImageBitmap;
    private ImageButton thumbnailImageButton, addImageButton;
    private ImageView expandedImage, zoomInIcon, capturedImage;
    private View thumbnailView;
    private RequestQueue queue;
    private View view;
    ListView lv;
    LatLng currentLatLng;

    ActivityResultLauncher<String> mPermissionResult;

    ArrayAdapter adapter;
    String requesterName, areaCenterString, requestDateString;
    //int requestID;
    ArrayList<RequestsNearby> requestsNearby;
    ArrayList<String> msg;
    ListViewRespond adapter2;


    public View onCreateView(@NonNull LayoutInflater inflater,
                                 ViewGroup container, Bundle savedInstanceState) {
        com.example.csc660_grpproject_where2buy.map.MapViewModel mapViewModel =
                new ViewModelProvider(this).get(MapViewModel.class);

        binding = FragmentMapBinding.inflate(inflater, container, false);
        supportMapFragment = (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.nearbySearchMap);
        View root = binding.getRoot();

        //final TextView textView = binding.textHome;

        // get values
        userName = MainActivity.getUserName();
        userID = MainActivity.getUserId();
        Log.d("DETECT GOOGLE ID?", "current user id: " + userID);
        lv = binding.lvMyReq;
        requestsNearby = new ArrayList<RequestsNearby>(1);
        msg = new ArrayList<String>(1);

        //textView.setText("Hello, " + userName);
        adapter = new ArrayAdapter(getContext(), android.R.layout.simple_list_item_1, msg);

        View view = inflater.inflate(R.layout.fragment_map, container, false);

        //mUserListRecyclerView = view.findViewById(R.id.user_list_recycler_view);

        mMapView = view.findViewById(R.id.nearbySearchMap);
        //getCurrentLocation();
        //initUserListRecyclerView();
        initGoogleMap(savedInstanceState);
        //refresh();
        mPermissionResult = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                new ActivityResultCallback<Boolean>() {
                    @Override
                    public void onActivityResult(Boolean result) {
                        if(result){
                            refresh();
                        }else{
                            msg.set(0, "This feature needs access to your location.\n\nPlease allow access to location services, then refresh the page.");
                            adapter.notifyDataSetChanged();
                        }
                    }
                }
        );
        client = LocationServices.getFusedLocationProviderClient(getContext());
        queue = Volley.newRequestQueue(getContext());
        refresh();
        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
    private static final String TAG = "UserListFragment";
    private MapView mMapView;

    private void refresh(){ // Get location, or do appropriate actions if needed. Called once after fragment loads and every time refresh button is pressed
        //Log.i("INFO", "refresh: ");
        msg.clear();
        msg.add(0, "Refreshing...");
        adapter.notifyDataSetChanged();
        lv.setEnabled(false); //set to disabled by default to prevent animation when clicking
        if(permissionGranted()) {
            //textView.setText("Refreshing...");
            @SuppressLint("MissingPermission") Task<Location> task = client.getLastLocation();
            task.addOnSuccessListener(new OnSuccessListener<Location>() {
                @Override
                public void onSuccess(final Location location) {
                    if (location != null) {
                        msg.set(0, "test");
                        //currentLatLng = new LatLng(location.getLatitude(), location.getLongitude());
                        sendRequest();
                    } else {
                        msg.set(0, "Unable to retrieve location.\n\nTurn on location services or try again later.");
                        adapter.notifyDataSetChanged();
                    }
                }
            });
        }else{ // If no permission, get permission
            mPermissionResult.launch(Manifest.permission.ACCESS_FINE_LOCATION);
        }

        adapter = new ArrayAdapter(getContext(), android.R.layout.simple_list_item_1, msg);
        lv.setAdapter(adapter);
        adapter.notifyDataSetChanged();
    }

    private void sendRequest(){
        //Log.e("RESPONSE", "sendRequest: entered");
        StringRequest stringRequest = new StringRequest(Request.Method.POST, url, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                //textView.setText(response);
                //Log.i("RESPONSE", response);
                try{
                    JSONObject rows = new JSONObject(response);
                    JSONObject statusJson = rows.getJSONObject("status");
                    JSONArray resultJson = rows.getJSONArray("result");
                    Log.d("REQUEST STATUS?", "status: " + statusJson);
                    Log.d("REQUEST RESULT?", "results: " + resultJson);
                    Log.d("RESULT LENGTH?", "is null?: " + resultJson.isNull(0));
                    statusCode = statusJson.getString("statusCode");
                    statusMessage = statusJson.getString("statusMessage");
                    switch (statusCode){
                        case "600":{ // if query is successful and there are results
                            RequestsNearby requestObject;
                            adapter.clear();
                            if(adapter2 != null){
                                adapter2.clear();
                            }
                            // retrieve data from json
                            for(int i = 0; i < resultJson.length(); i++){
                                Bitmap image = null;
                                JSONObject jsonObject = resultJson.getJSONObject(i);
                                requestID = jsonObject.getInt("requestID");
                                Log.d("RESULT CONTENT?", "index: " +i + " current requestID: " + requestID);
                                itemName = jsonObject.getString("itemName");
                                Log.d("RESULT CONTENT?", "index: " +i + " current itemName: " + itemName);
                                requestDateString = jsonObject.getString("requestDate");
                                Log.d("RESULT CONTENT?", "index: " +i + " current requestDate: " + requestDateString);
                                imageBase64 = jsonObject.getString("imageBase64");

                                if( !imageBase64.trim().equals("") ) {
                                    Log.i("ib64", "onResponse: imageBase64 is not null");
                                    // decoding base64 from https://stackoverflow.com/a/4837293
                                    byte[] decodedString = Base64.decode(imageBase64, Base64.DEFAULT);
                                    image = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
                                }
                                requestObject = new RequestsNearby(requestID, itemName, requestDateString, image);
                                Log.d("OBJECT?", "index: " +i + " current object: " + requestObject.toString());
                                requestsNearby.add(requestObject);
                                msg.add(i, requestsNearby.get(i).toString());
                                Log.d("ARRAY CONTENT?", "index: " +i + " content: " + msg.get(i));
                            }
                            if(getView() != null){ // only do setAdapter if getView != null to prevent crashing if user switches between fragments too quickly
                                adapter2 = new ListViewRespond(getContext(), requestsNearby, userID);
                                lv.setAdapter(adapter2);
                                lv.setEnabled(true); // Re-enable to allow clicking
                                //adapter.notifyDataSetChanged();
                            }
                            break;
                        } default:{
                            msg.set(0, statusMessage);
                            break;
                        }
                    }
                    adapter.notifyDataSetChanged();
                } catch (JSONException e) {
                    e.printStackTrace();
                    msg.add(0, "\n\nJSONException: " + e.getMessage());
                }
            }
        }, errorListener){ //POST parameters
            @Override
            protected Map<String, String> getParams(){
                Map<String, String> params = new HashMap<>();
                //params.put("responderLat", String.valueOf(currentLatLng.latitude));
                //params.put("responderLng", String.valueOf(currentLatLng.longitude));
                params.put("googleID", userID.toString());
                return params;
            }
        };

        if(stringRequest != null)
            queue.add(stringRequest);
        else
            Log.e("RESPONSE", "stringRequest is null");
    }

    private void initGoogleMap(Bundle savedInstanceState){
        // *** IMPORTANT ***
        // MapView requires that the Bundle you pass contain _ONLY_ MapView SDK
        // objects or sub-Bundles.
        Bundle mapViewBundle = null;
        if (savedInstanceState != null) {
            mapViewBundle = savedInstanceState.getBundle(MAPVIEW_BUNDLE_KEY);
        }

        mMapView.onCreate(mapViewBundle);

        mMapView.getMapAsync(this);
    }


    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        Bundle mapViewBundle = outState.getBundle(MAPVIEW_BUNDLE_KEY);
        if (mapViewBundle == null) {
            mapViewBundle = new Bundle();
            outState.putBundle(MAPVIEW_BUNDLE_KEY, mapViewBundle);
        }

        mMapView.onSaveInstanceState(mapViewBundle);
    }

    public Response.ErrorListener errorListener = new Response.ErrorListener() {
        @Override
        public void onErrorResponse(VolleyError error) {
            Log.e("ERRORS", "onErrorResponse: " + error.getMessage());
            error.printStackTrace();
            showSnackbar(-2,"An unexpected error occurred. Please try again later.");

        }
    };

    private void showSnackbar(int mode, String message){
        switch(mode){
            case 1:{ // success snackbar
                Snackbar.make(view, message, Snackbar.LENGTH_LONG).setBackgroundTint(getResources().getColor(com.google.android.libraries.places.R.color.quantum_googgreen)).show();
                break;
            } case 0:{ // info/neutral snackbar
                Snackbar.make(view, message, Snackbar.LENGTH_SHORT).show();
                break;
            } case -1:{ // warn snackbar
                Snackbar.make(view, message, Snackbar.LENGTH_LONG).setBackgroundTint(getResources().getColor(R.color.custom_orange_700)).show();
                break;
            } case -2:{ // error snackbar
                Snackbar.make(view, message, Snackbar.LENGTH_LONG).setBackgroundTint(getResources().getColor(R.color.custom_error_red)).show();
                break;
            }
        }
    }

    private boolean permissionGranted(){
        return ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    @Override
    public void onResume() {
        super.onResume();
        mMapView.onResume();
    }

    @Override
    public void onStart() {
        super.onStart();
        mMapView.onStart();
    }

    @Override
    public void onStop() {
        super.onStop();
        mMapView.onStop();
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
                    supportMapFragment.getMapAsync(new OnMapReadyCallback() {
                        @Override
                        public void onMapReady(@NonNull GoogleMap googleMap) {
                            latLng = new LatLng(location.getLatitude(), location.getLongitude());
                            //option = new MarkerOptions().position(latLng).icon(bitmapDescriptorFromVector(getContext().getApplicationContext(), R.drawable.ic_baseline_my_location_24_blue)).anchor(0.5f, 0.5f);
                            googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 12.9F), 10, null);
                            //googleMap.addMarker(option);
                            googleMap.getUiSettings().setAllGesturesEnabled(false);

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

    @Override
    public void onPause() {
        mMapView.onPause();
        super.onPause();
    }

    @Override
    public void onDestroy() {
        mMapView.onDestroy();
        super.onDestroy();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mMapView.onLowMemory();
    }
}