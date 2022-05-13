package com.example.csc660_grpproject_where2buy;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.example.csc660_grpproject_where2buy.databinding.ActivityMainBinding;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    static String userName, userId;
    Bundle bundle;
    boolean backButtonPressed = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        bundle = getIntent().getExtras();
        userName = bundle.getString("userName");
        userId = bundle.getString("googleID");

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        BottomNavigationView navView = findViewById(R.id.nav_view);
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        AppBarConfiguration appBarConfiguration = new AppBarConfiguration.Builder(
                R.id.navigation_map, R.id.navigation_request, R.id.navigation_respond)
                .build();
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_activity_main);
        //NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);
        NavigationUI.setupWithNavController(binding.navView, navController);


    }

    @Override
    public void onBackPressed() {
        if(backButtonPressed){
            super.onBackPressed();
            return;
        }

        this.backButtonPressed = true;
        Toast.makeText(getApplicationContext(), "Press back button again to exit app", Toast.LENGTH_LONG).show();

        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {
                backButtonPressed = false;
            }
        }, 2000);
    }


    // To get these values, in fragments write as below:
    // datatype variablename = ((MainActivity)getActivity()).methodname;

    public String getUserName(){
        return userName;
    }
    public String getUserId(){
        return userId;
    }
}