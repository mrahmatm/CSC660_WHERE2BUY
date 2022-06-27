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

public class RespondActivity extends AppCompatActivity{
    // xml vars
    private TextView itemNameText;
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
    private final String getItemDetailsURL = "http://csc660.allprojectcs270.com/getRequest.php";
    private final String addResponseURL = "http://csc660.allprojectcs270.com/addRespond.php";
    private RequestQueue queue;

    // normal vars
    private String itemName, statusCode, statusMessage;
    private int requestID, responderID;
    private View view;

    private ActionBar actionBar;

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
                        showSnackbar(0, "Please enable location services, then refresh the map");
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
        toggleSwitch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(toggleSwitch.isChecked()){
                    if(selectedLocation == null){
                        showSnackbar(-1,"Please select a location on the map.");
                    }
                }
            }
        });

        // Initialize stuff for POST
        storeName = findViewById(R.id.respondStoreNameText);
        queue = Volley.newRequestQueue(getApplicationContext());
        Bundle extras = getIntent().getExtras();
        if (extras == null) {
            //Toast.makeText(this, "An error occurred.", Toast.LENGTH_SHORT).show();
            showSnackbar(-2,"An error occurred.");
            finish();
        } else {
            requestID = extras.getInt("requestID");
            responderID = extras.getInt("responderID");
            refreshMap();
            getItemDetails();
        }

    }

    /*      Map stuff       */

    private void refreshMap(){
        showSnackbar(0, "Fetching location...");
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
                        showSnackbar(-2,"Unable to retrieve location.");
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

    public void customOnMapReady(@NonNull GoogleMap googleMap) {
        map = googleMap;
        map.getUiSettings().setMapToolbarEnabled(false);
        if(currentLatLng != null){
            //Toast.makeText(this, "Marker added", Toast.LENGTH_SHORT).show();
            //Toast.makeText(this, "lat/lng : " + currentLatLng.latitude + " " + currentLatLng.longitude, Toast.LENGTH_SHORT).show();
            map.addMarker(new MarkerOptions()
                    .position(currentLatLng)
                    .title("Current Location")
                    .snippet("You are here.")
                    .icon(bitmapDescriptorFromVector(getApplicationContext(), R.drawable.ic_baseline_person_pin_circle_24))
            );
            // if user previously has selected a location, place the marker back on the map
            if(selectedLocation != null){
                selectedLocation = map.addMarker(markerOptions);
            }
            map.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 17));
            map.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
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
            map.setOnMarkerDragListener(new GoogleMap.OnMarkerDragListener() {
                @Override
                public void onMarkerDrag(@NonNull Marker marker) {

                }

                @Override
                public void onMarkerDragEnd(@NonNull Marker marker) {
                    markerOptions.position(marker.getPosition()); // Save new position after user drags the marker
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


    /*      Shortcut method because I'm too lazy to type in full lol        */
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

    /*      Backend communication methods       */

    private void getItemDetails() { // Get details of selected request
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
                            //requestDateString = resultJson.getJSONObject(0).getString("requestDate");

                            //requestObject = new RequestsNearby(requestID, itemName, requestDateString);

                            itemNameText.setText(itemName);
                            Log.i("CUSTOM", "code 600");
                            break;
                        }
                        default: {
                            Log.e("CUSTOM", "Get details POSTed. Failed to get item.");
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

    private void sendPOST(){ // Send response to server
        if(permissionGranted()){
            if(currentLatLng != null) {
                //Toast.makeText(this, "dummy POST", Toast.LENGTH_SHORT).show();
                StringRequest stringRequest = new StringRequest(Request.Method.POST, addResponseURL, new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        try {
                            JSONObject rows = new JSONObject(response);
                            JSONObject statusJson = rows.getJSONObject("status");

                            statusCode = statusJson.getString("statusCode");
                            //statusMessage = statusJson.getString("statusMessage");
                            switch (statusCode) {
                                case "600": { // if query is successful
                                    showSnackbar(1,"Your response has successfully been sent.");
                                    finish();
                                    break;
                                }
                                default: {
                                    JSONObject resultJson = rows.getJSONObject("query");
                                    String query = resultJson.getString("queryFull");
                                    if(response.contains("Duplicate entry")){
                                        statusMessage = "You have already responded to this request.";
                                    }else{
                                        Log.e("RESPOND ERROR", "onResponse: " + response);
                                        statusMessage = "An unexpected error occurred.";
                                    }
                                    showSnackbar(-2,statusMessage);
                                    //Log.e("CUSTOM", query);
                                    //Log.e("CUSTOM", response);
                                    break;
                                }
                            }
                            // activityRespondText.setText(requestObject.toString());
                        } catch (JSONException e) {
                            e.printStackTrace();
                            showSnackbar(-2, "An unexpected error occurred.");
                            //Log.e("CUSTOM", "JSONException: " + e.getMessage());
                            //Log.e("CUSTOM", "PHP response: " + response);
                            //activityRespondText.setText(response + "\n\nJSONError: " + e.getMessage());
                        }
                    }
                }, errorListener){
                    @Nullable
                    @Override
                    protected Map<String, String> getParams() { //POST values
                        Map<String, String> params = new HashMap<>();
                        params.put("requestID", String.valueOf(requestID));
                        params.put("responderID", MainActivity.getUserId());
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
                showSnackbar(-2,"Unable to get location. Please try again later.");
            }
        }else{ // if location permission is denied
            //Toast.makeText(this, "", Toast.LENGTH_SHORT).show();
            showSnackbar(-2,"Please allow location permissions.");
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

        if(!proceed) // if inputs are invalid, display message
            showSnackbar(-1, msg);

        return proceed;
    }

    public Response.ErrorListener errorListener = new Response.ErrorListener() {
        @Override
        public void onErrorResponse(VolleyError error) {
            Log.e("ERRORS", "onErrorResponse: " + error.getLocalizedMessage());
            showSnackbar(-2,"An unexpected error occurred.");
        }
    };

    /*      Permission shortcut methods      */
    private boolean permissionGranted() {
        return ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    private void getPermission() {
        mPermissionResult.launch(Manifest.permission.ACCESS_FINE_LOCATION);
    }
}