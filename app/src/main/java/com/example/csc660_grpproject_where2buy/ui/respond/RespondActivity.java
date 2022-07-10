package com.example.csc660_grpproject_where2buy.ui.respond;

import androidx.activity.result.ActivityResult;
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
import androidx.core.content.FileProvider;

import android.Manifest;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
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

import com.android.volley.DefaultRetryPolicy;
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
    private View thumbnailView;

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
    private String itemName, imageBase64, statusCode, statusMessage;
    private int requestID, responderID;
    private View view;

    // misc
    private ActionBar actionBar;
    private boolean firstLaunch;
    private Uri imageUri;
    private Bitmap capturedImageBitmap, requestImageBitmap;
    private int shortAnimationDuration;

    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_respond);

        shortAnimationDuration = getResources().getInteger(android.R.integer.config_shortAnimTime);
        thumbnailView = findViewById(R.id.respondImageThumbnail);

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
        refreshBtn = findViewById(R.id.respondRefreshBtn);
        refreshBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                refreshMap();
            }
        });

        sendBtn = findViewById(R.id.sendResponseBtn);
        sendBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sendPOST();
                disableButton(sendBtn);
                refreshBtn.setClickable(false);
                map.getUiSettings().setScrollGesturesEnabled(false);
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

        //  Initialize xml vars
        thumbnailImageButton = findViewById(R.id.respondImageThumbnail);
        expandedImage = findViewById(R.id.respondImageExpanded);
        zoomInIcon = findViewById(R.id.respondZoomInIcon);

        capturedImage = findViewById(R.id.respondCapturedImage);
        addImageButton = findViewById(R.id.respondAddImage);

        // Set intent for launching camera
        ActivityResultLauncher<Intent> mResultLauncher =
            registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                new ActivityResultCallback<ActivityResult>() {
                    @Override
                    public void onActivityResult(ActivityResult result) {
                        capturedImage.setImageURI(imageUri);
                        capturedImage.setScaleType(ImageView.ScaleType.CENTER_CROP);
                        capturedImageBitmap =  ((BitmapDrawable)capturedImage.getDrawable()).getBitmap();
                    }
                }
            );

        // Onclick, launch camera and get image
        addImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
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
            Toast.makeText(this, "An error occurred.", Toast.LENGTH_LONG).show();
            finish();
        } else {
            requestID = extras.getInt("requestID");
            responderID = extras.getInt("responderID");
            getItemDetails();
            refreshMap();
        }

    }

    private void disableButton(Button button){
        button.setEnabled(false);
        button.setBackgroundColor(Color.GRAY);
    }
    private void enableButton(Button button, String color){
        button.setEnabled(true);
        button.setBackgroundColor(Color.parseColor(color));
    }

    /*      Map stuff       */

    private void refreshMap(){
        //showSnackbar(0, "Fetching location...");
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
                        case 1:
                            disableButton(sendBtn);
                            break;
                        case 2:
                        case 3:
                            disableButton(sendBtn);
                            map.getUiSettings().setScrollGesturesEnabled(false);
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
                    map.getUiSettings().setScrollGesturesEnabled(true);
                    enableButton(sendBtn, "#5556ba");
                    refreshBtn.setClickable(true);
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
        Bitmap bitmap = Bitmap.createBitmap(vectorDrawable.getIntrinsicWidth(), vectorDrawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
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
                    Log.i("getItemDetails", "onResponse: " + response);
                    JSONObject rows = new JSONObject(response);
                    JSONObject statusJson = rows.getJSONObject("status");
                    JSONArray resultJson = rows.getJSONArray("result");

                    statusCode = statusJson.getString("statusCode");
                    statusMessage = statusJson.getString("statusMessage");
                    switch (statusCode) {
                        case "600": { // if query is successful and there are results
                            // retrieve data from json
                            JSONObject jsonObject = resultJson.getJSONObject(0);
                            requestID = jsonObject.getInt("requestID");
                            itemName = jsonObject.getString("itemName");
                            imageBase64 = jsonObject.getString("imageBase64");

                            //requestDateString = resultJson.getJSONObject(0).getString("requestDate");
                            //requestObject = new RequestsNearby(requestID, itemName, requestDateString);

                            itemNameText.setText(itemName);
                            if( !imageBase64.trim().equals("") ){ // if there is an image, display it and set onclick to expand the image
                               // decoding base64 from https://stackoverflow.com/a/4837293
                               byte[] decodedString = Base64.decode(imageBase64, Base64.DEFAULT);
                               requestImageBitmap = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);

                               zoomInIcon.setVisibility(View.VISIBLE);
                               expandedImage.setImageBitmap(requestImageBitmap);
                               thumbnailImageButton.setImageBitmap(requestImageBitmap);
                               thumbnailImageButton.setScaleType(ImageView.ScaleType.CENTER_CROP);

                               //thumbnailView.setBackgroundResource(requestImageBitmap);
                               thumbnailView.setOnClickListener(new View.OnClickListener() {
                                   @Override
                                   public void onClick(View view) {
                                       zoomImageFromThumb();
                                   }
                               });
                            }
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
                    Log.e("CUSTOM", "JSONException: response is " + response);
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

    // expand image animation, from https://developer.android.com/training/animation/zoom
    private Animator currentAnimator;
    private void zoomImageFromThumb(){
        if(currentAnimator != null)
            currentAnimator.cancel();

        final Rect startBounds = new Rect();
        final Rect finalBounds = new Rect();
        final Point globalOffset = new Point();

        // The start bounds are the global visible rectangle of the thumbnail,
        // and the final bounds are the global visible rectangle of the container
        // view. Also set the container view's offset as the origin for the
        // bounds, since that's the origin for the positioning animation
        // properties (X, Y).

        thumbnailView.getGlobalVisibleRect(startBounds);
        findViewById(R.id.container)
                .getGlobalVisibleRect(finalBounds, globalOffset);
        startBounds.offset(-globalOffset.x, -globalOffset.y);
        finalBounds.offset(-globalOffset.x, -globalOffset.y);

        // Adjust the start bounds to be the same aspect ratio as the final
        // bounds using the "center crop" technique. This prevents undesirable
        // stretching during the animation. Also calculate the start scaling
        // factor (the end scaling factor is always 1.0).
        float startScale;
        if ((float) finalBounds.width() / finalBounds.height()
                > (float) startBounds.width() / startBounds.height()) {
            // Extend start bounds horizontally
            startScale = (float) startBounds.height() / finalBounds.height();
            float startWidth = startScale * finalBounds.width();
            float deltaWidth = (startWidth - startBounds.width()) / 2;
            startBounds.left -= deltaWidth;
            startBounds.right += deltaWidth;
        } else {
            // Extend start bounds vertically
            startScale = (float) startBounds.width() / finalBounds.width();
            float startHeight = startScale * finalBounds.height();
            float deltaHeight = (startHeight - startBounds.height()) / 2;
            startBounds.top -= deltaHeight;
            startBounds.bottom += deltaHeight;
        }

        // Hide the thumbnail and show the zoomed-in view. When the animation
        // begins, it will position the zoomed-in view in the place of the
        // thumbnail.
        thumbnailView.setAlpha(0f);
        expandedImage.setVisibility(View.VISIBLE);

        // Set the pivot point for SCALE_X and SCALE_Y transformations
        // to the top-left corner of the zoomed-in view (the default
        // is the center of the view).
        expandedImage.setPivotX(0f);
        expandedImage.setPivotY(0f);

        // Construct and run the parallel animation of the four translation and
        // scale properties (X, Y, SCALE_X, and SCALE_Y).
        AnimatorSet set = new AnimatorSet();
        set
                .play(ObjectAnimator.ofFloat(expandedImage, View.X,
                        startBounds.left, finalBounds.left))
                .with(ObjectAnimator.ofFloat(expandedImage, View.Y,
                        startBounds.top, finalBounds.top))
                .with(ObjectAnimator.ofFloat(expandedImage, View.SCALE_X,
                        startScale, 1f))
                .with(ObjectAnimator.ofFloat(expandedImage,
                        View.SCALE_Y, startScale, 1f));
        set.setDuration(shortAnimationDuration);
        set.setInterpolator(new DecelerateInterpolator());
        set.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                currentAnimator = null;
            }

            @Override
            public void onAnimationCancel(Animator animation) {
                currentAnimator = null;
            }
        });
        set.start();
        currentAnimator = set;

        // Upon clicking the zoomed-in image, it should zoom back down
        // to the original bounds and show the thumbnail instead of
        // the expanded image.
        final float startScaleFinal = startScale;
        expandedImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (currentAnimator != null) {
                    currentAnimator.cancel();
                }

                // Animate the four positioning/sizing properties in parallel,
                // back to their original values.
                AnimatorSet set = new AnimatorSet();
                set.play(ObjectAnimator
                                .ofFloat(expandedImage, View.X, startBounds.left))
                        .with(ObjectAnimator
                                .ofFloat(expandedImage,
                                        View.Y,startBounds.top))
                        .with(ObjectAnimator
                                .ofFloat(expandedImage,
                                        View.SCALE_X, startScaleFinal))
                        .with(ObjectAnimator
                                .ofFloat(expandedImage,
                                        View.SCALE_Y, startScaleFinal));
                set.setDuration(shortAnimationDuration);
                set.setInterpolator(new DecelerateInterpolator());
                set.addListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        thumbnailView.setAlpha(1f);
                        expandedImage.setVisibility(View.GONE);
                        currentAnimator = null;
                    }

                    @Override
                    public void onAnimationCancel(Animator animation) {
                        thumbnailView.setAlpha(1f);
                        expandedImage.setVisibility(View.GONE);
                        currentAnimator = null;
                    }
                });
                set.start();
                currentAnimator = set;
            }
        });
    }

    // convert bitmap to base64, from https://stackoverflow.com/a/38796456
    private String imageToString(Bitmap bmp){
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bmp.compress(Bitmap.CompressFormat.JPEG, 70, baos);
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
                                    // notify successful
                                    showSnackbar(1,"Response at " + storeName.getText() + " successfully recorded.");

                                    // reset inputs to default
                                    storeName.setText("");
                                    capturedImage.setImageResource(R.drawable.ic_baseline_image_not_supported_24);
                                    capturedImage.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
                                    capturedImage.setBackgroundResource(android.R.color.darker_gray);

                                    refreshMap();
                                    //Toast.makeText(RespondActivity.this, "Your response has successfully been recorded", Toast.LENGTH_SHORT).show();
                                    //finish();
                                    break;
                                }
                                default: {
                                    JSONObject resultJson = rows.getJSONObject("query");
                                    String query = resultJson.getString("queryFull");
                                    if(response.contains("Duplicate entry")){
                                        statusMessage = "You entered a duplicate entry.";
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
                            Log.e("CustomError", "sendPOST onResponse: " + e.getMessage());
                            showSnackbar(-2, "An unexpected error occurred.");
                            //Log.e("CUSTOM", "JSONException: " + e.getMessage());
                            //Log.e("CUSTOM", "PHP response: " + response);
                            //activityRespondText.setText(response + "\n\nJSONError: " + e.getMessage());
                        }
                        /*map.getUiSettings().setScrollGesturesEnabled(true);
                        enableButton(sendBtn, "#5556ba");
                        refreshBtn.setClickable(true);*/
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
                        params.put("storeLat", String.valueOf(selectedLocation.latitude));
                        params.put("storeLng", String.valueOf(selectedLocation.longitude));

                        //params.put("itemQty", itemQty.getText().toString().trim());
                        params.put("itemQty", selectedQty);

                        params.put("imageBase64", imageToString(capturedImageBitmap));
                        return params;
                    }
                };
                stringRequest.setRetryPolicy(new DefaultRetryPolicy(5000, DefaultRetryPolicy.DEFAULT_MAX_RETRIES, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
                if(inputsAreValid()){
                    queue.add(stringRequest);
                }
                enableButton(sendBtn, "#5556ba");
                refreshBtn.setClickable(true);
                map.getUiSettings().setScrollGesturesEnabled(true);
            }
            else{
                //Toast.makeText(this, "", Toast.LENGTH_SHORT).show();
                showSnackbar(-2,"Unable to get location. Please try again later.");
            }
        }else{ // if location permission is denied
            //Toast.makeText(this, "", Toast.LENGTH_SHORT).show();
            showSnackbar(-2,"Please allow location permissions.");
        }

        map.getUiSettings().setScrollGesturesEnabled(true);
        enableButton(sendBtn, "#5556ba");
        refreshBtn.setClickable(false);
    }

    private boolean inputsAreValid(){
        boolean storeNameValid = false;
        boolean imageValid = false;
        boolean proceed;
        String msg = "";

        // Check if store name is valid
        if(!storeName.getText().toString().trim().equals("")) // if input is not empty, set as valid
            storeNameValid = true;
        else
            msg += "Please enter the store name.";

        if(capturedImageBitmap != null)
            imageValid = true;
        else
            msg += " Please capture an image as proof.";

        proceed = storeNameValid && imageValid;

        if(!proceed) // if inputs are invalid, display message
            showSnackbar(-1, msg);

        return proceed;
    }

    public Response.ErrorListener errorListener = new Response.ErrorListener() {
        @Override
        public void onErrorResponse(VolleyError error) {
            Log.e("ERRORS", "onErrorResponse: " + error.getMessage());
            error.printStackTrace();
            showSnackbar(-2,"An unexpected error occurred. Please try again later.");

            map.getUiSettings().setScrollGesturesEnabled(true);
            enableButton(sendBtn, "#5556ba");
            refreshBtn.setClickable(true);
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