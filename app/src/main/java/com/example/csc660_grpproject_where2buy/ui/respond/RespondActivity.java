package com.example.csc660_grpproject_where2buy.ui.respond;

import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.example.csc660_grpproject_where2buy.MainActivity;
import com.example.csc660_grpproject_where2buy.R;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class RespondActivity extends AppCompatActivity/* implements OnMapReadyCallback */{
    // xml vars
    private TextView title, itemNameText;
    private Button sendBtn, cancelBtn;
    private MapView mapView;
    private FloatingActionButton refreshBtn, toMarkerBtn;
    private Toolbar toolbar;
    private Switch toggleSwitch;
    private EditText storeName;

    // location vars
    private FusedLocationProviderClient client;
    private ActivityResultLauncher<String> mPermissionResult;
    private LatLng currentLatLng;
    private GoogleMap map;
    private Marker selectedLocation;
    private MarkerOptions markerOptions;

    // backend communication vars
    private final String getItemDetailsURL = "http://www.csc660.ml/getRequest.php";
    private final String addResponseURL = "http://www.csc660.ml/addRespond.php";
    private RequestQueue queue;

    // normal vars
    private String requestDateString, itemName, statusCode, statusMessage;
    private int requestID, responderID;
    private View view;

    ActionBar actionBar;

    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_respond);

        // Setup action bar
        view = getWindow().findViewById(android.R.id.content);
        toolbar = findViewById(R.id.respondToolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });
        actionBar = getSupportActionBar();
        actionBar.setHomeAsUpIndicator(R.drawable.ic_baseline_arrow_back_24);
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setTitle("Report Available");


        // Setup map
        mapView = findViewById(R.id.respondMap);
        mapView.onCreate(savedInstanceState);
        mapView.getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(@NonNull GoogleMap googleMap) {
                customOnMapReady(googleMap);
            }
        });
        selectedLocation = null;

        client = LocationServices.getFusedLocationProviderClient(getApplicationContext());

        /*title = findViewById(R.id.respondActivityTitle);
        //title.setTextSize(16);
        title.setText("Select the place where this item is in stock");*/

        itemNameText = findViewById(R.id.respondItemName);

        // Setup buttons
        sendBtn = findViewById(R.id.sendResponseBtn);
        sendBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sendPOST();
            }
        });

        refreshBtn = findViewById(R.id.respondRefreshBtn);
        refreshBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                refreshMap();
            }
        });

        toMarkerBtn = findViewById(R.id.goToMarkerBtn);
        toMarkerBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(selectedLocation != null){
                    map.animateCamera(CameraUpdateFactory.newLatLngZoom(selectedLocation.getPosition(), 17));
                }
            }
        });

        // Get permission prompt with callback found on https://stackoverflow.com/a/66552678
        mPermissionResult = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                new ActivityResultCallback<Boolean>() {
                    @Override
                    public void onActivityResult(Boolean result) {
                        if(!result){ //if permission is rejected
                            // toast or something
                            // make submit button say please enable location
                            //Toast.makeText(getApplicationContext(), "Please enable location services, then refresh the map", Toast.LENGTH_SHORT).show();
                            Snackbar.make(view, "Please enable location services, then refresh the map", Snackbar.LENGTH_SHORT).show();
                            Log.i("CUSTOM", "onClick: denied clicked");
                        }
                    }
                }
        );
        cancelBtn = findViewById(R.id.respondBackBtn);

        cancelBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });

        toggleSwitch = findViewById(R.id.respondSwitch);

        // Initialize stuff for POST
        storeName = findViewById(R.id.respondStoreNameText);
        queue = Volley.newRequestQueue(getApplicationContext());
        Bundle extras = getIntent().getExtras();
        if (extras == null) {
            //Toast.makeText(this, "An error occurred.", Toast.LENGTH_SHORT).show();
            Snackbar.make(view, "An error occurred.", Snackbar.LENGTH_LONG).show();
            finish();
        } else {
            requestID = extras.getInt("requestID");
            responderID = extras.getInt("responderID");
            refreshMap();
            getItemDetails();
        }
    }

    private void refreshMap(){
        Snackbar.make(view, "Fetching location...", Snackbar.LENGTH_SHORT).show();
        if(permissionGranted()){
            @SuppressLint("MissingPermission") Task<Location> task = client.getLastLocation();
            task.addOnSuccessListener(new OnSuccessListener<Location>() {
                @Override
                public void onSuccess(Location location) {
                    if(location != null){
                        map.clear(); // clear all markers
                        mapView.setEnabled(true);
                        mapView.setClickable(true);
                        //Toast.makeText(RespondActivity.this, "getCurrentLocation Location isnt null", Toast.LENGTH_SHORT).show();
                        currentLatLng = new LatLng(location.getLatitude(), location.getLongitude());

                        mapView.getMapAsync(new OnMapReadyCallback() {
                            @Override
                            public void onMapReady(@NonNull GoogleMap googleMap) {
                                customOnMapReady(googleMap);
                            }
                        });
                    }else{
                        Snackbar.make(view, "Unable to retrieve location.", Snackbar.LENGTH_LONG).setBackgroundTint(getResources().getColor(R.color.custom_error_red)).show();
                        //Toast.makeText(RespondActivity.this, "Unable to retrieve location.", Toast.LENGTH_SHORT).show();
                        mapView.setEnabled(false);
                        mapView.setClickable(false);
                    }
                }
            });
        }else{
            getPermission();
        }
    }

    private void sendPOST(){
        if(permissionGranted()){
            if(currentLatLng != null) {
                //Toast.makeText(this, "dummy POST", Toast.LENGTH_SHORT).show();
                StringRequest stringRequest = new StringRequest(Request.Method.POST, addResponseURL, new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        //Toast.makeText(RespondActivity.this, "we POSTed!!!!", Toast.LENGTH_SHORT).show();

                        try {
                            JSONObject rows = new JSONObject(response);
                            JSONObject statusJson = rows.getJSONObject("status");

                            /*
                            // Debug stuff
                            JSONObject resultJson = rows.getJSONObject("query");
                           */
                            statusCode = statusJson.getString("statusCode");
                            statusMessage = statusJson.getString("statusMessage");
                            switch (statusCode) {
                                case "600": { // if query is successful
                                    // retrieve data from json
                                    //itemName = resultJson.getJSONObject(0).getString("itemName");
                                    //requestDateString = resultJson.getJSONObject(0).getString("requestDate");

                                    /*
                                    // Debug stuff
                                    String query = resultJson.getString("queryFull");
                                    Log.i("CUSTOM", "code 600");
                                    Log.i("CUSTOM", query);
                                    */

                                    Snackbar.make(view, "Your response has successfully been sent.", Snackbar.LENGTH_SHORT).setBackgroundTint(getResources().getColor(com.google.android.libraries.places.R.color.quantum_googgreen)).show();
                                    break;
                                }
                                default: {
                                    Log.e("CUSTOM", "something happened");
                                    break;
                                }
                            }
                            // activityRespondText.setText(requestObject.toString());
                        } catch (JSONException e) {
                            e.printStackTrace();
                            Log.e("CUSTOM", "JSONException: " + e.getMessage());
                            //activityRespondText.setText(response + "\n\nJSONError: " + e.getMessage());
                        }

                    }
                }, errorListener){
                    @Nullable
                    @Override
                    protected Map<String, String> getParams() { //POST values
                        Map<String, String> params = new HashMap<>();
                        params.put("requestID", String.valueOf(requestID));
                        params.put("responderID", String.valueOf(MainActivity.getUserId()));
                        params.put("storeName", storeName.getText().toString().trim());

                        //Stored in db currently: request id, responder user id, store name, lat, lng, item count
                        if(toggleSwitch.isChecked()){ // is checked means use selected location
                            params.put("storeLat", String.valueOf(markerOptions.getPosition().latitude));
                            params.put("storeLng", String.valueOf(markerOptions.getPosition().longitude));

                        }else{ // not checked means use current location
                            params.put("storeLat", String.valueOf(currentLatLng.latitude));
                            params.put("storeLng", String.valueOf(currentLatLng.longitude));
                        }
                        return params;
                    }
                };
                if(inputsAreValid()){
                    queue.add(stringRequest);
                }
            }
            else{
                //Toast.makeText(this, "", Toast.LENGTH_SHORT).show();
                Snackbar.make(view, "Unable to get location. Please try again later.", Snackbar.LENGTH_LONG).setBackgroundTint(getResources().getColor(R.color.custom_error_red)).show();
            }
        }else{ // if location permission is denied
            //Toast.makeText(this, "", Toast.LENGTH_SHORT).show();
            Snackbar.make(view, "Please allow location permissions.", Snackbar.LENGTH_LONG).setBackgroundTint(getResources().getColor(R.color.custom_error_red)).show();
        }
    }

    private boolean inputsAreValid(){
        boolean storeNameValid = false;
        boolean markerOptionValid = false;
        boolean proceed;
        String msg = "";

        // Check if store name is valid
        if(!storeName.getText().toString().trim().equals("")) // if input is not empty, set as valid
            storeNameValid = true;
        else
            msg = "Please enter the store name.";

        // Check if location is valid
        if(toggleSwitch.isChecked()) {// if using custom location
            if (markerOptions != null)  // if custom location marker exists, set as valid
                markerOptionValid = true;
            else                        // if custom location marker does not exist, set warning message
                msg += "Please select a location on the map.";
        }else {                       // if using current location
            if (currentLatLng != null)  // if user location can be retrieved, set as valid
                markerOptionValid = true;
            else                        // if user location can't be retrieved, set warning message
                msg += "Unable to get location. Please turn on location services or try again later.";
        }
        proceed = storeNameValid && markerOptionValid; // proceed will only be true if both inputs are valid

        if(!proceed) // if !proceed, display message
            Snackbar.make(view, msg, Snackbar.LENGTH_LONG).setBackgroundTint(getResources().getColor(R.color.custom_orange_700)).show();

        return proceed;
    }

    private void getItemDetails() {
        StringRequest stringRequest = new StringRequest(Request.Method.POST, getItemDetailsURL, new Response.Listener<String>() {
        @Override
        public void onResponse(String response) {
            try {
                JSONObject rows = new JSONObject(response);
                JSONObject statusJson = rows.getJSONObject("status");
                JSONArray resultJson = rows.getJSONArray("result");

                statusCode = statusJson.getString("statusCode");
                statusMessage = statusJson.getString("statusMessage");
                switch (statusCode) {
                    case "600": { // if query is successful and there are results
                        // retrieve data from json
                        requestID = resultJson.getJSONObject(0).getInt("requestID");
                        itemName = resultJson.getJSONObject(0).getString("itemName");
                        requestDateString = resultJson.getJSONObject(0).getString("requestDate");

                        //requestObject = new RequestsNearby(requestID, itemName, requestDateString);

                        itemNameText.setText(itemName);
                        Log.i("CUSTOM", "code 600");
                        break;
                    }
                    default: {
                        Log.i("CUSTOM", "code something else");
                        break;
                    }
                }
                // activityRespondText.setText(requestObject.toString());
            } catch (JSONException e) {
                e.printStackTrace();
                Log.e("CUSTOM", "JSONException: " + e.getMessage());
                //activityRespondText.setText(response + "\n\nJSONError: " + e.getMessage());
            }
        }}, errorListener) { //POST parameters
            @Nullable
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                params.put("requestID", String.valueOf(requestID));
                return params;
            }
        };
        if (stringRequest != null) {
            queue.add(stringRequest);
        } else {
            Log.e("ERRORS", "stringRequest is null");
        }
    }

    public Response.ErrorListener errorListener = new Response.ErrorListener() {
        @Override
        public void onErrorResponse(VolleyError error) {
            Log.e("ERRORS", "onErrorResponse: " + error.getLocalizedMessage());
            Snackbar.make(view, "An unexpected error occurred.", Snackbar.LENGTH_LONG).setBackgroundTint(getResources().getColor(R.color.custom_error_red)).show();
        }
    };

    private boolean permissionGranted() {
        return ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }
    private void getPermission() {
        mPermissionResult.launch(Manifest.permission.ACCESS_FINE_LOCATION);
    }

    public void customOnMapReady(@NonNull GoogleMap googleMap) {
        map = googleMap;
        map.getUiSettings().setMapToolbarEnabled(false);
        if(currentLatLng != null){
            //Toast.makeText(this, "Marker added", Toast.LENGTH_SHORT).show();
            //Toast.makeText(this, "lat/lng : " + currentLatLng.latitude + " " + currentLatLng.longitude, Toast.LENGTH_SHORT).show();
            map.addMarker(new MarkerOptions()
                    .position(currentLatLng)
                    .title("You are here")
                    .icon(bitmapDescriptorFromVector(getApplicationContext(), R.drawable.ic_baseline_person_pin_circle_24))
            );
            // if user previously has selected a location, letak balik the marker on the map
            if(selectedLocation != null){
                selectedLocation = map.addMarker(markerOptions);
            }
            googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 17));

            googleMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
                @Override
                public void onMapClick(@NonNull LatLng latLng) {
                    // Only add a new marker if user has not previously set a marker
                    if(selectedLocation == null){
                        markerOptions = new MarkerOptions()
                                .position(latLng)
                                .title("Selected Location")
                                .icon(bitmapDescriptorFromVector(getApplicationContext(), R.drawable.ic_baseline_location_on_24))
                                .draggable(true);
                        selectedLocation = map.addMarker(markerOptions);
                        toMarkerBtn.setVisibility(View.VISIBLE); // when marker is added, set to visible
                    }else{ // If there is already a marker, change the position
                        selectedLocation.setPosition(latLng);
                        markerOptions.position(latLng);
                    }
                    //Toast.makeText(RespondActivity.this, "you clicked on lat lng " + latLng.latitude + " " + latLng.longitude, Toast.LENGTH_SHORT).show();
                }
            });

            // Make it so that dragging marker also saves it in the app, so that when refreshing it still stays
            googleMap.setOnMarkerDragListener(new GoogleMap.OnMarkerDragListener() {
                @Override
                public void onMarkerDrag(@NonNull Marker marker) {

                }

                @Override
                public void onMarkerDragEnd(@NonNull Marker marker) {
                    markerOptions.position(marker.getPosition());
                }

                @Override
                public void onMarkerDragStart(@NonNull Marker marker) {

                }
            });
        }else{
            //Toast.makeText(this, "currentLatLng is null", Toast.LENGTH_SHORT).show();
        }
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
    protected void onResume() { // Override to also update mapView on resume
        mapView.onResume(); // without this here, mapView will only update when it is tapped instead of constantly updating
        super.onResume();
    }
}