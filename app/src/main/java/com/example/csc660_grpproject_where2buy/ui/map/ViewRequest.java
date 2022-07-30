package com.example.csc660_grpproject_where2buy.ui.map;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.example.csc660_grpproject_where2buy.R;
import com.example.csc660_grpproject_where2buy.ui.respond.RespondActivity;
import com.google.android.gms.maps.model.LatLng;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class ViewRequest extends AppCompatActivity {
    TextView txtName, txtRequesterName, txtRespondPlaceHolder;
    ImageView displayImage;
    String currentRequest, currentRequester;
    private String userID;
    JSONArray resultArray;
    ArrayList<Listing> listing = new ArrayList<>();
    ListView lv;
    Button btnRespond;

    ListingAdapter adapter;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_request);

        Bundle bundle = getIntent().getExtras();
        String val = bundle.getString("id");
        userID = bundle.getString("currentUser");
        currentRequest = val;

        Log.d("?new activity", "onCreate: "+val);
        txtName = (TextView) findViewById(R.id.displayReqName);
        txtRequesterName = (TextView) findViewById(R.id.displayRequester);
        txtRespondPlaceHolder = (TextView)findViewById(R.id.respondPlaceHolder);
        displayImage = (ImageView)findViewById(R.id.displayImage) ;
        lv = (ListView)findViewById(R.id.displayRespondList) ;

        txtName.setText(val);
        mQueue = Volley.newRequestQueue(this);
        getRequestInfo();
        adapter = new ListingAdapter(this, R.layout.listing_view_layout, listing);
        lv.setAdapter(adapter) ;
        adapter.notifyDataSetChanged();
        //getRequesterInfo();
    }

    private RequestQueue mQueue;
    private void getRequestInfo() {
        // Log.d("?current location: ", latLng.toString());
        String url = "http://csc660.allprojectcs270.com/getCurrentRequest.php";

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
                                //showSnackbar(0, "There are no requests nearby");
                            }
                            for (int i = 0; i < jsonArray.length(); i++) {
                                JSONObject request = jsonArray.getJSONObject(i);

                                String itemName = request.getString("itemName");
                                //String distanceFromCenter = request.getString("distance_in_km");
                                String imageBase64 = request.getString("imageBase64");
                                String requesterID = request.getString("requesterUserID");

                                //mTextViewResult.append(firstName + ", " + String.valueOf(age) + ", " + mail + "\n\n");
                                Log.d("?resultofFetch 1st: ", itemName+ "by: "+ requesterID);
                                txtName.setText(itemName);
                                displayImage.setImageBitmap(StringToBitMap(imageBase64));
                                currentRequester = requesterID;
                                getRequesterInfo();
                                getAllCurrentRespond();
                            }
                            //putMarkers();
                        } catch (JSONException e) {
                            e.printStackTrace();
                            Log.d("? php error:", "onErrorResponse: "+e.getMessage());

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
                params.put("requestID", currentRequest);
                //Log.d("confirm current req ?", "current req: "+currentRequest);
                //params.put("responderLng", String.valueOf(currentLocation.longitude));
                return params;
            }
        };

        if(request != null)
            mQueue.add(request);
        else
            Log.e("?RESPONSE", "stringRequest is null");

        //putMarkers();

    }

    private void getRequesterInfo() {
        // Log.d("?current location: ", latLng.toString());
        String url = "http://csc660.allprojectcs270.com/getCurrentRequester.php";

        StringRequest request = new StringRequest(Request.Method.POST, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        try {
                            JSONObject rows = new JSONObject(response);
                            JSONObject statusJson = rows.getJSONObject("status");
                            String statusCode = statusJson.getString("statusCode");
                            Log.d("?getRequesterInfo: ", statusCode);

                            JSONArray jsonArray = rows.getJSONArray("result");
                            resultArray = jsonArray;
                            //putMarkers(jsonArray);
                            if (jsonArray.length() <= 0){
                                Log.d("?RESULT STATUS: ", "THERE'S NO RESULT");
                                //showSnackbar(0, "There are no requests nearby");
                            }
                            for (int i = 0; i < jsonArray.length(); i++) {
                                JSONObject request = jsonArray.getJSONObject(i);

                                String requesterID = request.getString("displayName");
                                txtRequesterName.setText("Requested by: "+requesterID);
                                //mTextViewResult.append(firstName + ", " + String.valueOf(age) + ", " + mail + "\n\n");
                                //Log.d("?resultofFetch 2nd: ", requesterID+ "is the requestion: ");
                            }
                            //putMarkers();
                        } catch (JSONException e) {
                            e.printStackTrace();
                            Log.d("? php error:", "onErrorResponse: "+e.getMessage());

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
                params.put("requesterID", currentRequester);
                //Log.d("confirm requester ?", "current req: "+currentRequester);
                //params.put("responderLng", String.valueOf(currentLocation.longitude));
                return params;
            }
        };

        if(request != null)
            mQueue.add(request);
        else
            Log.e("?RESPONSE", "stringRequest is null");

        //putMarkers();

    }

    private void getAllCurrentRespond() {
        // Log.d("?current location: ", latLng.toString());
        String url = "http://csc660.allprojectcs270.com/getListing.php";

        StringRequest request = new StringRequest(Request.Method.POST, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        try {
                            JSONObject rows = new JSONObject(response);
                            JSONObject statusJson = rows.getJSONObject("status");
                            String statusCode = statusJson.getString("statusCode");
                            Log.d("?getAllCurrentRespond: ", statusCode);

                            JSONArray jsonArray = rows.getJSONArray("result");
                            resultArray = jsonArray;
                            //putMarkers(jsonArray);
                            if (jsonArray.length() <= 0){
                                Log.d("?RESULT STATUS: ", "THERE'S NO RESULT");
                                txtRespondPlaceHolder.setText("There are no responses yet...");
                                //showSnackbar(0, "There are no requests nearby");
                            }

                            Log.d("? rowcount", "onResponse: returns "+ jsonArray.length());
                            for (int i = 0; i < jsonArray.length(); i++) {
                                JSONObject request = jsonArray.getJSONObject(i);

                                String storeName = request.getString("storeName");
                                String dateSubmitted = request.getString("dateSubmitted");
                                Double storeLocationLat = Double.parseDouble(request.getString("storeLocationLat"));
                                Double storeLocationLng = Double.parseDouble(request.getString("storeLocationLng"));
                                LatLng storeLocation = new LatLng(storeLocationLat, storeLocationLng);
                                String imageBase64 = request.getString("imageBase64");

                                Listing currentRespond = new Listing(imageBase64, storeName, dateSubmitted, storeLocation);
                                listing.add(currentRespond);
                                adapter.notifyDataSetChanged();
                                //adapter.clear();
                            }

                            //putMarkers();
                        } catch (JSONException e) {
                            e.printStackTrace();
                            Log.d("? php error:", "onErrorResponse: "+e.getMessage());
                            txtRespondPlaceHolder.setText("There are no responses yet...");


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
                params.put("requestID", currentRequest);
                Log.d("confirm req ?", "current req: "+currentRequest);
                //params.put("responderLng", String.valueOf(currentLocation.longitude));
                return params;
            }
        };

        if(request != null)
            mQueue.add(request);
        else
            Log.e("?RESPONSE", "stringRequest is null");

        //putMarkers();

    }

    public Bitmap StringToBitMap(String encodedString){
        try{
            byte [] encodeByte = Base64.decode(encodedString,Base64.DEFAULT);
            Bitmap bitmap = BitmapFactory.decodeByteArray(encodeByte, 0, encodeByte.length);
            return bitmap;
        }
        catch(Exception e){
            e.getMessage();
            return null;
        }
    }
}