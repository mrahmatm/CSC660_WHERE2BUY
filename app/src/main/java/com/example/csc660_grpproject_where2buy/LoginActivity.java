package com.example.csc660_grpproject_where2buy;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInStatusCodes;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class LoginActivity extends AppCompatActivity {
    private final String loginURL = "http://csc660.allprojectcs270.com/login.php";
    private RequestQueue queue;
    private static final String TAG = null;
    GoogleSignInOptions gso;
    GoogleSignInClient mGoogleSignInClient;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        //int RC_SIGN_IN = 1;

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        queue = Volley.newRequestQueue(getApplicationContext());

        SignInButton googleSignIn = findViewById(R.id.google_btn);
        // Configure sign-in to request the user's ID, email address, and basic
        // profile. ID and basic profile are included in DEFAULT_SIGN_IN.
        gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .build();

        // Build a GoogleSignInClient with the options specified by gso.
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        // https://www.youtube.com/watch?v=suVgcrPwYKQ Based indian tutorial channels god bless

        googleSignIn.setSize(SignInButton.SIZE_STANDARD);
        googleSignIn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                signIn();
            }
        });

        GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(this);
        if(account != null){ //if user has previously signed in
            goToMain(account); //go straight to MainActivity
        }
    }

    private void signIn() {
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, 1000);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == 1000){
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                task.getResult(ApiException.class);
                GoogleSignInAccount gsia = task.getResult();

                StringRequest stringRequest = new StringRequest(Request.Method.POST, loginURL, new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        try {
                            JSONObject jsonObject = new JSONObject(response);

                            int statusCode = jsonObject.getInt("statusCode");
                            String statusMsg = jsonObject.getString("statusDesc");

                            if(statusCode == 200 || statusCode == 202)
                                goToMain(gsia);
                            else
                                Toast.makeText(LoginActivity.this, "ERROR: " + statusMsg, Toast.LENGTH_SHORT).show();
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                }, new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.e(TAG, "onErrorResponse: " + error.getMessage());
                    }
                })
                { // POST values
                    @Override
                    protected Map<String, String> getParams() {
                        Map<String, String> params = new HashMap<>();
                        params.put("googleID", String.valueOf(gsia.getId()));
                        params.put("displayName", String.valueOf(gsia.getDisplayName()));
                        return params;
                    }
                };
                queue.add(stringRequest);
            } catch (ApiException e) {
                e.printStackTrace();

                if(e.getStatusCode() != GoogleSignInStatusCodes.SIGN_IN_CANCELLED){
                    String statusMsg = GoogleSignInStatusCodes.getStatusCodeString(e.getStatusCode());
                    Toast.makeText(getApplicationContext(), "Something went wrong\nError Message: " + statusMsg, Toast.LENGTH_LONG).show();
                    Log.e(TAG, "signInResult:failed code=" + e.getStatusCode());
                }else{
                    Toast.makeText(getApplicationContext(), "Please sign in to proceed.", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    private void goToMain(GoogleSignInAccount acc){
        finish(); //prevents going back to LoginActivity
        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
        intent.putExtra("googleID", acc.getId());
        intent.putExtra("userName", acc.getDisplayName());
        startActivity(intent);
    }
}