package com.example.csc660_grpproject_where2buy.ui.respond;

import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.example.csc660_grpproject_where2buy.R;
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
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.libraries.places.api.Places;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class RespondActivity extends AppCompatActivity/* implements OnMapReadyCallback */{
    // xml vars
    private TextView itemNameText;
    private Button sendBtn, cancelBtn;
    private MapView mapView;
    private FloatingActionButton refreshBtn;

    // location vars
    private FusedLocationProviderClient client;
    private ActivityResultLauncher<String> mPermissionResult;
    private static final String MAPVIEW_BUNDLE_KEY = "MapViewBundleKey";
    private LatLng currentLatLng;
    private GoogleMap map;
    private SupportMapFragment supportMapFragment;

    // backend communication vars
    private final String URL = "http://www.csc660.ml/getRequest.php";
    private RequestQueue queue;

    // normal vars
    private String requestDateString, itemName, statusCode, statusMessage;
    private int requestID, responderID;

    //private RequestsNearby requestObject;
    @Override
    protected void onCreate(Bundle savedInstanceState){

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_respond);


        mapView = findViewById(R.id.respondMap);
        mapView.onCreate(savedInstanceState);
        //mapView.onResume();
        mapView.getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(@NonNull GoogleMap googleMap) {
                customOnMapReady(googleMap);
            }
        });

        client = LocationServices.getFusedLocationProviderClient(getApplicationContext());

        itemNameText = findViewById(R.id.respondItemName);
        sendBtn = findViewById(R.id.sendResponseBtn);
        refreshBtn = findViewById(R.id.respondRefreshBtn);
        refreshBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                refreshMap();
            }
        });


        // Get permission found on https://stackoverflow.com/a/66552678
        mPermissionResult = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                new ActivityResultCallback<Boolean>() {
                    @Override
                    public void onActivityResult(Boolean result) {
                        if(result){ // If permission is granted

                            sendBtn.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View view) {
                                    //Toast.makeText(RespondActivity.this, "lmao xd", Toast.LENGTH_SHORT).show();
                                    Intent i = new Intent(getApplicationContext(), AddResponse.class);
                                    //i.putExtra("storeID", );
                                }
                            });
                        }else{ //if permission is rejected
                            // toast or something
                            // make submit button say please enable location
                            Toast.makeText(RespondActivity.this, "Please enable location services, then refresh the map", Toast.LENGTH_SHORT).show();
                            sendBtn.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View view) {
                                    Toast.makeText(RespondActivity.this, "Please enable location services, then refresh the map", Toast.LENGTH_SHORT).show();
                                }
                            });
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


        queue = Volley.newRequestQueue(getApplicationContext());


        Bundle extras = getIntent().getExtras();
        if (extras == null) {
            Toast.makeText(this, "An error occurred.", Toast.LENGTH_SHORT).show();
            finish();
        } else {
            requestID = extras.getInt("requestID");
            responderID = extras.getInt("responderID");

            getCurrentLocation();
            //Toast.makeText(this, "requestID " + requestID, Toast.LENGTH_SHORT).show();
            sendPOST();
        }
    }

    private void refreshMap() {
        Toast.makeText(this, "Getting location...", Toast.LENGTH_SHORT).show();
        getCurrentLocation();
    }


    private void getCurrentLocation(){
        if(permissionGranted()){
            @SuppressLint("MissingPermission") Task<Location> task = client.getLastLocation();
            task.addOnSuccessListener(new OnSuccessListener<Location>() {
                @Override
                public void onSuccess(Location location) {
                    if(location != null){
                        map.clear();
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
                        Toast.makeText(RespondActivity.this, "Unable to retrieve location.", Toast.LENGTH_SHORT).show();
                        mapView.setEnabled(false);
                        mapView.setClickable(false);
                    }
                }
            });
        }else{
            getPermission();
        }
    }

    private void sendPOST() {
        StringRequest stringRequest = new StringRequest(Request.Method.POST, URL, new Response.Listener<String>() {
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
            }
        }, errorListener) { //POST parameters
            @Nullable
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                params.put("responderID", String.valueOf(responderID));
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
            Log.e("ERRORS", "onErrorResponse: " + error.getMessage());
        }
    };


    private boolean permissionGranted() {
        return ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }
    private void getPermission() {
        mPermissionResult.launch(Manifest.permission.ACCESS_FINE_LOCATION);
    }
    /*@Override*/
    public void customOnMapReady(@NonNull GoogleMap googleMap) {
        map = googleMap;

        if(currentLatLng != null){
            //Toast.makeText(this, "Marker added", Toast.LENGTH_SHORT).show();
            //Toast.makeText(this, "lat/lng : " + currentLatLng.latitude + " " + currentLatLng.longitude, Toast.LENGTH_SHORT).show();
            map.addMarker(new MarkerOptions()
                    .position(currentLatLng)
                    .title("You are here")
                    .icon(bitmapDescriptorFromVector(getApplicationContext(), R.drawable.ic_baseline_person_pin_circle_24))
            );
            googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 17));
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
    protected void onResume() {
        mapView.onResume(); // Override to update mapView onResume
        super.onResume();
    }
}