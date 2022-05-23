package com.example.csc660_grpproject_where2buy.ui.respond;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
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
import com.example.csc660_grpproject_where2buy.MainActivity;
import com.example.csc660_grpproject_where2buy.RequestsNearby;
import com.example.csc660_grpproject_where2buy.databinding.FragmentRespondBinding;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import com.example.csc660_grpproject_where2buy.R;

public class RespondFragment extends Fragment {
    private FragmentRespondBinding binding;
    LatLng currentLatLng;
    float maxDistance, distanceBetween;
    FusedLocationProviderClient client;

    ActivityResultLauncher<String> mPermissionResult;

    //logged in user data
    private String userID;

    TextView textView;
    FloatingActionButton refreshBtn;
    ListView lv;
    ArrayAdapter adapter;

    String requesterName, itemName, areaCenterString, requestDateString, statusCode, statusMessage;
    int requestID;
    ArrayList<RequestsNearby> requestsNearby;
    ArrayList<String> msg;
    ListViewRespond adapter2;

    final String URL = "http://www.csc660.ml/getRequestList.php";
    RequestQueue queue;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        RespondViewModel respondViewModel =
                new ViewModelProvider(this).get(RespondViewModel.class);

        binding = FragmentRespondBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        //final TextView textView = binding.textNotifications;
        //respondViewModel.getText().observe(getViewLifecycleOwner(), textView::setText);

        userID = MainActivity.getUserId();

        textView = binding.textView;
        refreshBtn = binding.refreshBtn;
        lv = binding.lv;
        requestsNearby = new ArrayList<RequestsNearby>(1);

        msg = new ArrayList<String>(1);
        adapter = new ArrayAdapter(getContext(), android.R.layout.simple_list_item_1, msg);
        //lv.setAdapter(adapter);
        //adapter.notifyDataSetChanged();

        textView.setText("Requests near you");
        textView.setAllCaps(true);
        textView.setTextSize(20);

        // Get permission found on https://stackoverflow.com/a/66552678
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
        refreshBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                refresh();
            }
        });
        refresh();

        return root;

    }

    private void refresh(){ // Get location, or do appropriate actions if needed. Called once after fragment loads and every time refresh button is pressed
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
                        currentLatLng = new LatLng(location.getLatitude(), location.getLongitude());
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
        StringRequest stringRequest = new StringRequest(Request.Method.POST, URL, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                //textView.setText(response);
                //Log.i("RESPONSE", response);
                try{
                    JSONObject rows = new JSONObject(response);
                    JSONObject statusJson = rows.getJSONObject("status");
                    JSONArray resultJson = rows.getJSONArray("result");

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
                                requestID = resultJson.getJSONObject(i).getInt("requestID");
                                itemName = resultJson.getJSONObject(i).getString("itemName");
                                requestDateString = resultJson.getJSONObject(i).getString("requestDate");

                                requestObject = new RequestsNearby(requestID, itemName, requestDateString);

                                requestsNearby.add(requestObject);
                                msg.add(i, requestsNearby.get(i).toString());

                                //ImageButton imageButton =   R.id.respondButton;
                                //imageButton.setOnClickListener();

                            }
                            if(getView() != null){ // only do setAdapter if getView != null to prevent crashing if user switches between fragments too quickly
                                adapter2 = new ListViewRespond(getContext(), requestsNearby, userID);
                                lv.setAdapter(adapter2);
                                lv.setEnabled(true); // Re-enable to allow clicking
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
                    msg.add(0, msg.get(0) + "\n\nJSONException: " + e.getMessage());
                }
            }
        }, errorListener){ //POST parameters
            @Override
            protected Map<String, String> getParams(){
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

    private boolean permissionGranted(){
        return ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

}