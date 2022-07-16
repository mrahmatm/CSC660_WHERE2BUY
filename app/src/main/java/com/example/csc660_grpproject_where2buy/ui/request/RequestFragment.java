package com.example.csc660_grpproject_where2buy.ui.request;

import android.Manifest;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.provider.Settings;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
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
import com.example.csc660_grpproject_where2buy.databinding.FragmentRequestBinding;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
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

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class RequestFragment extends Fragment implements OnMapReadyCallback {

    private FragmentRequestBinding binding;
    private View root;

    private TextView textView2, textView3, textView6;
    private EditText editText1;
    //private MapView mapView;
    private SeekBar seekBar;
    private Button button;

    private boolean isPermissionGranted;
    private SupportMapFragment supportMapFragment;
    private FusedLocationProviderClient client;
    private LatLng latLng;
    private MarkerOptions option;
    private Circle circle;
    private CircleOptions circleOption;

    private ImageButton addImageButton;
    private ImageView viewImage;
    private Uri imageUri, selectedImageUri;
    private Bitmap selectedImageBitmap;
    private ActivityResultLauncher<String> cameraPerms;

    private String URL = "http://csc660.allprojectcs270.com/addRequest.php";
    private RequestQueue queue;

    private String itemName, statusCode, statusMessage, userID;
    private int areaRadius;

    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        RequestViewModel requestViewModel = new ViewModelProvider(this).get(RequestViewModel.class);

        binding = FragmentRequestBinding.inflate(inflater, container, false);
        root = binding.getRoot();

        textView6 = binding.textView6;
        editText1 = binding.editTextTextPersonName;
        //mapView = binding.mapView;
        seekBar = binding.seekBar;
        button = binding.button;
        addImageButton = binding.requestAddImage;
        viewImage = binding.requestCapturedImage;

        userID = MainActivity.getUserId();

        supportMapFragment = (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.mapView);

        client = LocationServices.getFusedLocationProviderClient(getContext());

        circleOption = new CircleOptions();

        getCurrentLocation();

        queue = Volley.newRequestQueue(getContext());

        //supportMapFragment.getMapAsync(this);

        addImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                imageChooser();
            }
        });

        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sendPOST();
            }
        });

        return root;
    }

    private void imageChooser()
    {
        Intent i = new Intent();
        i.setType("image/*");
        i.setAction(Intent.ACTION_GET_CONTENT);

        launchSomeActivity.launch(i);
    }

    ActivityResultLauncher<Intent> launchSomeActivity = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK) {
                    Intent data = result.getData();
                    // do your operation from here....
                    if (data != null && data.getData() != null) {
                        selectedImageUri = data.getData();

                        try {
                            selectedImageBitmap = MediaStore.Images.Media.getBitmap(getContext().getContentResolver(), selectedImageUri);
                        }
                        catch (IOException e) {
                            e.printStackTrace();
                        }

                        viewImage.setImageBitmap(selectedImageBitmap);
                        viewImage.setImageURI(imageUri);
                        viewImage.setScaleType(ImageView.ScaleType.CENTER_CROP);
                        selectedImageBitmap = ((BitmapDrawable)viewImage.getDrawable()).getBitmap();
                    }
                }
            });

    // convert bitmap to base64, from https://stackoverflow.com/a/38796456
    private String imageToString(Bitmap bmp){
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bmp.compress(Bitmap.CompressFormat.JPEG, 70, baos);
        byte[] imageBytes = baos.toByteArray();
        String encodedImage = Base64.encodeToString(imageBytes, Base64.DEFAULT);
        return encodedImage;
    }

    private void sendPOST() { // Send response to server
        checkMyPermission();
        if(latLng != null) {
            //Toast.makeText(getContext(), textView6.getText().toString().replace(" KM", ""), Toast.LENGTH_SHORT).show();
            StringRequest stringRequest = new StringRequest(Request.Method.POST, URL, new Response.Listener<String>() {
                @Override
                public void onResponse(String response) {
                    try {
                        JSONObject rows = new JSONObject(response);
                        JSONObject statusJson = rows.getJSONObject("status");

                        statusCode = statusJson.getString("statusCode");
                        //statusMessage = statusJson.getString("statusMessage");
                        switch (statusCode) {
                            case "600": { // if query is successful
                                showSnackbar(1,"Your request has successfully been sent.");
                                editText1.setText("");
                                circle.remove();
                                seekBar.setProgress(10);
                                getCurrentLocation();
                                circle.remove();
                                break;
                            }
                            case "610": {
                                showSnackbar(1,"Fatal error. Please try again.");
                                //getActivity().finish();
                                break;
                            }
                            default: {
                                break;
                            }
                        }
                        // activityRespondText.setText(requestObject.toString());
                    } catch (JSONException e) {
                        e.printStackTrace();
                        showSnackbar(-2, "An unexpected error occurred.");
                    }
                }
            }, errorListener){
                @Nullable
                @Override
                protected Map<String, String> getParams() { //POST values
                    Map<String, String> params = new HashMap<>();
                    params.put("requesterID", MainActivity.getUserId());
                    params.put("itemName", editText1.getText().toString().trim());
                    params.put("requesterLat", String.valueOf(latLng.latitude));
                    params.put("requesterLng", String.valueOf(latLng.longitude));
                    params.put("areaRadius", textView6.getText().toString().replace(" KM", ""));
                    params.put("imageBase64", imageToString(selectedImageBitmap));

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
    }

    private boolean inputsAreValid(){
        boolean itemNameValid = false;
        boolean markerOptionValid = false;
        boolean proceed;
        String msg = "";

        // Check if store name is valid
        if(!editText1.getText().toString().trim().equals("")) { // if input is not empty, set as valid
            itemNameValid = true;
        }
        else {
            msg = "Please enter the item name.";
        }

        if (latLng != null) {  // if user location can be retrieved, set as valid
            markerOptionValid = true;
        }
        else {                        // if user location can't be retrieved, set warning message
            msg += "Unable to get location. Please turn on location services or try again later.";
        }

        proceed = itemNameValid && markerOptionValid; // proceed will only be true if both inputs are valid

        if(!proceed) // if inputs are invalid, display message
            showSnackbar(-1, msg);

        return proceed;
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
                            option = new MarkerOptions().position(latLng).icon(bitmapDescriptorFromVector(getContext().getApplicationContext(), R.drawable.ic_baseline_my_location_24_blue)).anchor(0.5f, 0.5f);;
//                            option = new MarkerOptions().position(latLng);
                            googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 12.9F), 10, null);
                            googleMap.addMarker(option);
                            googleMap.getUiSettings().setAllGesturesEnabled(false);

                            circle = googleMap.addCircle(circleOption.center(latLng).radius(1000).fillColor(Color.argb(50, 16, 21, 115)).strokeColor(Color.rgb(16, 21, 115)).strokeWidth(2f));

                            seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                                @RequiresApi(api = Build.VERSION_CODES.O)
                                @Override
                                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                                    seekBar.setMin(10);
                                    seekBar.setMax(1000);
                                    textView6.setText(progress/10 + " KM");
                                    if (progress <= 20) {
                                        googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 12.9F), 10, null);
                                    }
                                    else if (progress > 21 && progress <= 30) {
                                        googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 12.7F), 10, null);
                                    }
                                    else if (progress > 31 && progress <= 40) {
                                        googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 12.5F), 10, null);
                                    }
                                    else if (progress > 41 && progress <= 50) {
                                        googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 12.3F), 10, null);
                                    }
                                    else if (progress > 51 && progress <= 60) {
                                        googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 12.1F), 10, null);
                                    }
                                    else if (progress > 61 && progress <= 70) {
                                        googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 11.9F), 10, null);
                                    }
                                    else if (progress > 71 && progress <= 80) {
                                        googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 11.7F), 10, null);
                                    }
                                    else if (progress > 81 && progress <= 90) {
                                        googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 11.5F), 10, null);
                                    }
                                    else if (progress > 91 && progress <= 140) {
                                        googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 11), 10, null);
                                    }
                                    else if (progress > 141 && progress <= 190) {
                                        googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 10.5F), 10, null);
                                    }
                                    else if (progress > 191 && progress <= 240) {
//                                        googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 10));
                                        googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 10), 10, null);
                                    }
                                    else if (progress > 241 && progress <= 490) {
                                        googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 9), 10, null);
                                    }
                                    else if (progress > 491) {
                                        googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 8), 10, null);
                                    }
                                    circle.remove();
                                    circle = googleMap.addCircle(circleOption.center(latLng).radius((progress*100)).fillColor(Color.argb(50, 16, 21, 115)).strokeColor(Color.rgb(16, 21, 115)).strokeWidth(2f));

                                }

                                @Override
                                public void onStartTrackingTouch(SeekBar seekBar) {

                                }

                                @Override
                                public void onStopTrackingTouch(SeekBar seekBar) {

                                }
                            });
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

    private BitmapDescriptor bitmapDescriptorFromVector(Context context, int vectoriID){
        Drawable vectorDrawable  = ContextCompat.getDrawable(context, vectoriID);
        vectorDrawable.setBounds(0, 0, vectorDrawable.getIntrinsicWidth(), vectorDrawable.getIntrinsicHeight());
        Bitmap bitmap=Bitmap.createBitmap(vectorDrawable.getIntrinsicWidth(), vectorDrawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);

        vectorDrawable.draw(canvas);
        return BitmapDescriptorFactory.fromBitmap(bitmap);
    }

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

    public Response.ErrorListener errorListener = new Response.ErrorListener() {
        @Override
        public void onErrorResponse(VolleyError error) {
            Log.e("ERRORS", "onErrorResponse: " + error.getLocalizedMessage());
            showSnackbar(-2,"An unexpected error occurred.");
        }
    };

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {

    }
}