package com.example.csc660_grpproject_where2buy.ui.respond;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.example.csc660_grpproject_where2buy.databinding.FragmentRespondBinding;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class RespondFragment extends Fragment {

    private FragmentRespondBinding binding;
    LatLng currentLatLng;
    float maxDistance, distanceBetween;
    FusedLocationProviderClient client;
    TextView textView;
    Button refreshBtn;
    final String URL = "http://www.csc660.ml/getRequestDemo.php";
    String requesterName, itemName, areaCenterString, requestDateString, statusCode, statusMessage;
    int requestID;

    RequestQueue queue;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        RespondViewModel respondViewModel =
                new ViewModelProvider(this).get(RespondViewModel.class);

        binding = FragmentRespondBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        //final TextView textView = binding.textNotifications;
        //respondViewModel.getText().observe(getViewLifecycleOwner(), textView::setText);


        textView = binding.textView;
        refreshBtn = binding.refreshBtn;

        maxDistance = 200; //in km

        client = LocationServices.getFusedLocationProviderClient(getContext());

        queue = Volley.newRequestQueue(getContext());
        refreshBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                getCurrentLocation();
            }
        });
        getCurrentLocation();

        return root;

    }

    private void sendRequest(){
        Log.e("RESPONSE", "sendRequest: entered");
        StringRequest stringRequest = new StringRequest(Request.Method.POST, URL, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                //getActivity().getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
                textView.setText(response);
                Log.i("RESPONSE", response);

                try{
                    JSONArray rows = new JSONArray(response);
                    JSONObject row;

                    statusCode = rows.getJSONObject(0).getString("statusId");
                    statusMessage = rows.getJSONObject(0).getString("statusMessage");
                    if(statusCode.equals("600")){ // if query is successful
                        // retrieve data from json
                        for(int i = 1; i < rows.length(); i++){
                            row = rows.getJSONObject(i);
                            requestID = row.getInt("requestID");
                            requesterName = row.getString("requesterName");
                            itemName = row.getString("itemName");
                            areaCenterString = row.getString("areaCenter");
                            requestDateString = row.getString("requestDate");
                        }
                    }

                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }, errorListener){ //POST values
            @Override
            protected Map<String, String> getParams() throws AuthFailureError {
                Map<String, String> params = new HashMap<>();
                params.put("responderLat", String.valueOf(currentLatLng.latitude));
                params.put("responderLng", String.valueOf(currentLatLng.longitude));
                return params;
            }
        };

        if(stringRequest != null)
            queue.add(stringRequest);
        else
            Log.e("RESPONSE", "stringRequest is null");
    }

    public Response.ErrorListener errorListener = new Response.ErrorListener() {
        @Override
        public void onErrorResponse(VolleyError error) {
            Log.e("RESPONSE", "onErrorResponse: " + error.getMessage());
        }
    };

    private void getCurrentLocation(){ // Get location, or do appropriate actions if needed. Called once after fragment loads and every time refresh button is pressed
        if(permissionGranted()) {
            textView.setText("Refreshing...");
            //initialize task location
            Task<Location> task = client.getLastLocation();
            task.addOnSuccessListener(new OnSuccessListener<Location>() {
                @Override
                public void onSuccess(final Location location) {
                    if (location != null) {
                        //textView.setText("location is " + location.getLatitude() + ", " + location.getLongitude());

                        currentLatLng = new LatLng(location.getLatitude(), location.getLongitude());
                        //sync map
                    /*supportMapFragment.getMapAsync(new OnMapReadyCallback() {
                        @Override
                        public void onMapReady(GoogleMap googleMap) {
                            //initialize lat lng
                            LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
                            //create marker options
                            MarkerOptions options = new MarkerOptions().position(latLng).title("I am here");
                            //zoom map scale 15
                            googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15));
                            googleMap.addMarker(options);
                        }
                    });*/
                        //Toast.makeText(getContext(), "your latlng is " + currentLatLng.latitude + ", " + currentLatLng.latitude, Toast.LENGTH_SHORT).show();
                        sendRequest();
                    } else {
                        textView.setText("Unable to retrieve location.\n\nTurn on location services or try again later. 1");
                    }
                }
            });
        }else{
            textView.setText("To respond to other people, we need to access your location.\n\nPlease allow access to location services.");
            getPermission();
        }
    }

    private boolean permissionGranted(){
        return ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    private void getPermission(){
        ActivityCompat.requestPermissions(getActivity(),
                new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 44);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}