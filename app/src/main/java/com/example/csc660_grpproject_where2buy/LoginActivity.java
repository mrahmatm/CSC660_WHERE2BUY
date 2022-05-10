package com.example.csc660_grpproject_where2buy;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInStatusCodes;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;

public class LoginActivity extends AppCompatActivity {
    GoogleSignInOptions gso;
    GoogleSignInClient mGoogleSignInClient;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        //int RC_SIGN_IN = 1;


        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        SignInButton googleSignIn = findViewById(R.id.google_btn);
        // Configure sign-in to request the user's ID, email address, and basic
        // profile. ID and basic profile are included in DEFAULT_SIGN_IN.
        gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken("919579181968-7gfn0b36v1f96aqan09pq2iupsqrg3te.apps.googleusercontent.com")
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
        if(account != null){
            finish();
            goToMain(account);
        }
    }

    /*@Override
    protected void onStart() {
        super.onStart();

        // Check for existing Google Sign In account, if the user is already signed in
        // the GoogleSignInAccount will be non-null.
        GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(this);
        updateUI(account);
    }

    private void updateUI(GoogleSignInAccount account) {
        if(account == null){ // if user has not signed in

        }else{ // if user has signed in
            // redirect to main activity

            Intent i = new Intent();
        }
    }*/

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

                finish();
                goToMain(task.getResult());
            } catch (ApiException e) {
                e.printStackTrace();

                if(e.getStatusCode() != GoogleSignInStatusCodes.SIGN_IN_CANCELLED){
                    String statusMsg = GoogleSignInStatusCodes.getStatusCodeString(e.getStatusCode());
                    Toast.makeText(getApplicationContext(), "Something went wrong\nError Message: " + statusMsg, Toast.LENGTH_LONG).show();
                }else{
                    Toast.makeText(getApplicationContext(), "Please sign in to proceed.", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    private void goToMain(GoogleSignInAccount acc){
        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
        intent.putExtra("googleID", acc.getId());
        intent.putExtra("userName", acc.getDisplayName());
        startActivity(intent);
    }
}