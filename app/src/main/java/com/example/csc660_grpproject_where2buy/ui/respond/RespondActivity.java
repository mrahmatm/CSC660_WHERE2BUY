package com.example.csc660_grpproject_where2buy.ui.respond;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContract;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.example.csc660_grpproject_where2buy.BuildConfig;
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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class RespondActivity extends AppCompatActivity{
    // xml vars
    private TextView itemNameText;
    private Button sendBtn, cancelBtn;
    private MapView mapView;
    private FloatingActionButton refreshBtn, toMarkerBtn;
    private Toolbar toolbar;
    private Switch toggleSwitch;
    private EditText storeName, itemQty;
    private Spinner qtySelect;
    private String selectedQty;
    private ImageButton thumbnailImageButton, addImageButton;
    private ImageView expandedImage, zoomInIcon, capturedImage;

    // location vars
    private FusedLocationProviderClient client;
    private ActivityResultLauncher<String> mPermissionResult, cameraPerms;
    private LatLng currentLatLng, selectedLocation;
    private GoogleMap map;
    //private Marker selectedLocation;
    private MarkerOptions markerOptions;
    private Marker location;

    // backend communication vars
    private final String getItemDetailsURL = "http://csc660.allprojectcs270.com/getRequest.php";
    private final String addResponseURL = "http://csc660.allprojectcs270.com/addRespond.php";
    private RequestQueue queue;

    // normal vars
    private String itemName, statusCode, statusMessage;
    private int requestID, responderID;
    private View view;

    // misc
    private ActionBar actionBar;
    private boolean firstLaunch;
    private Uri imageUri;
    private Bitmap imageBitmap;

    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_respond);

        firstLaunch = true;

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

        cameraPerms = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(),
            new ActivityResultCallback<Boolean>() {
                @Override
                public void onActivityResult(Boolean result) {
                    if(!result){ //if permission is rejected
                        // toast or something
                        // make submit button say please enable location
                        //Toast.makeText(getApplicationContext(), "Please enable location services, then refresh the map", Toast.LENGTH_SHORT).show();
                        //showSnackbar(0, "Please enable location services, then refresh the map");
                        //Log.i("CUSTOM", "onClick: denied clicked");
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

        /*toggleSwitch = findViewById(R.id.respondSwitch);
        toggleSwitch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(toggleSwitch.isChecked()){
                    if(selectedLocation == null){
                        showSnackbar(-1,"Please select a location on the map.");
                    }
                }
            }
        });*/

        //  Initialize xml vars
        thumbnailImageButton = findViewById(R.id.respondImageThumbnail);
        expandedImage = findViewById(R.id.respondImageExpanded);
        zoomInIcon = findViewById(R.id.respondZoomInIcon);

        capturedImage = findViewById(R.id.respondCapturedImage);
        addImageButton = findViewById(R.id.respondAddImage);


        //imageUri = FileProvider.getUriForFile(this, BuildConfig.APPLICATION_ID + ".provider", file);
        ActivityResultLauncher<Intent> mResultLauncher =
            registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                new ActivityResultCallback<ActivityResult>() {
                    @Override
                    public void onActivityResult(ActivityResult result) {
                        capturedImage.setImageURI(imageUri);
                        capturedImage.setScaleType(ImageView.ScaleType.CENTER_CROP);
                    }
                }
            );

        addImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent captureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                if(ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED){
                    File file = null;
                    try {
                        file = File.createTempFile(UUID.randomUUID().toString(), ".jpg", getCacheDir());
                    } catch (IOException e) {
                        Log.e("FILE", e.getMessage());
                    }
                    imageUri = FileProvider.getUriForFile(getApplicationContext(), "com.example.csc660_grpproject_where2buy.FileProvider", file);
                    Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                    cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
                    mResultLauncher.launch(cameraIntent);
                    //startActivityForResult(captureIntent, 1);
                }else{
                    cameraPerms.launch(Manifest.permission.CAMERA);
                }

            }
        });

        // Initialize stuff for POST
        storeName = findViewById(R.id.respondStoreNameText);
        //itemQty = findViewById(R.id.respondItemQtyText);
        //itemQty.setFilters(new InputFilter[]{new InputFilterMinMax("1", "999")});

        qtySelect = findViewById(R.id.respondQtySpinner);
        String[] qtyOptions = {"Few", "Many"};
        ArrayAdapter adapter = new ArrayAdapter(this, android.R.layout.simple_spinner_item, qtyOptions);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        qtySelect.setAdapter(adapter);
        qtySelect.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                selectedQty = qtyOptions[i];
                //Toast.makeText(RespondActivity.this, "Selected: " + selectedQty, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });

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
            MarkerOptions options = new MarkerOptions()
                    .position(currentLatLng)
                    .title("Drag to change position")
                    .icon(bitmapDescriptorFromVector(getApplicationContext(), R.drawable.ic_baseline_location_on_24));
            location = map.addMarker(options);
            //map.animateCamera(CameraUpdateFactory.newLatLng(currentLatLng));
            if(firstLaunch){
                map.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng,17));
                firstLaunch = false;
            }
            else
                map.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 17));
            //map.animateCamera(CameraUpdateFactory.newCameraPosition(new CameraPosition(currentLatLng, 17, 0, 0)));

            map.setOnCameraMoveStartedListener(new GoogleMap.OnCameraMoveStartedListener() {
                @Override
                public void onCameraMoveStarted(int i) {
                    switch(i){
                        case 1: case 2: case 3:
                            sendBtn.setEnabled(false);
                            sendBtn.setBackgroundColor(Color.GRAY);
                            break;
                    }
                }
            });

            map.setOnCameraMoveListener(new GoogleMap.OnCameraMoveListener() {
                @Override
                public void onCameraMove() {
                    location.setPosition(map.getCameraPosition().target);
                }
            });

            map.setOnCameraIdleListener(new GoogleMap.OnCameraIdleListener() {
                @Override
                public void onCameraIdle() {
                    selectedLocation = map.getCameraPosition().target;
                    sendBtn.setEnabled(true);
                    sendBtn.setBackgroundColor(Color.parseColor("#5556ba"));
                }
            });
            // if user previously has selected a location, place the marker back on the map
            /*if(selectedLocation != null){
                selectedLocation = map.addMarker(markerOptions);
            }

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
            });*/


            // Make it so that dragging marker also saves it in the app, so that when refreshing it still stays
            /*map.setOnMarkerDragListener(new GoogleMap.OnMarkerDragListener() {
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
            });*/
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

                            /** TO-DO: get image from database, allow responder to also capture image and upload */
                            /*if image exists{
                                imageButton.setImageBitmap(); // set thumbnail image
                                expandedImage.setImageBitmap(); // set full size image
                                zoomInIcon.setVisibility(View.VISIBLE);
                            }*/
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


    // from https://stackoverflow.com/a/38796456
    private String getStringImage(Bitmap bmp){
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bmp.compress(Bitmap.CompressFormat.JPEG, 100, baos);
        byte[] imageBytes = baos.toByteArray();
        String encodedImage = Base64.encodeToString(imageBytes, Base64.DEFAULT);
        return encodedImage;
    }

    private void sendPOST(){ // Send request to server
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
                                    //showSnackbar(1,"Your response has successfully been sent.");
                                    Toast.makeText(RespondActivity.this, "Your response has successfully been recorded", Toast.LENGTH_SHORT).show();
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
                        //params.put("storeName", storeName.getText().toString().trim());

                        //Stored in db currently: request id, responder user id, store name, lat, lng, item count
                        params.put("storeLat", String.valueOf(selectedLocation.latitude));
                        params.put("storeLng", String.valueOf(selectedLocation.longitude));

                        //params.put("itemQty", itemQty.getText().toString().trim());
                        params.put("itemQty", selectedQty);
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
        boolean qtyValid = false;
        boolean proceed;
        String msg = "";

        // Check if store name is valid
        if(!storeName.getText().toString().trim().equals("")) // if input is not empty, set as valid
            storeNameValid = true;
        else
            msg = "Please enter the store name.";

        /*// Check if location is valid
        if (!itemQty.getText().toString().trim().equals(""))  // if custom location marker exists, set as valid
            qtyValid = true;
        else                        // if custom location marker does not exist, set warning message
            msg += "\nPlease select a location on the map.";

        proceed = storeNameValid && qtyValid; // proceed will only be true if both inputs are valid*/
        proceed = storeNameValid;

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