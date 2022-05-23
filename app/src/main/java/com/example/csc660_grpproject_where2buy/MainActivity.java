package com.example.csc660_grpproject_where2buy;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.NavDestination;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.example.csc660_grpproject_where2buy.databinding.ActivityMainBinding;

public class MainActivity extends AppCompatActivity {
    private static String userName, userID;

    private ActivityMainBinding binding;
    Bundle bundle;
    boolean backButtonPressed = false;
    TextView title;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        bundle = getIntent().getExtras();
        userName = bundle.getString("userName");
        userID = bundle.getString("googleID");

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        title = binding.textView4;

        BottomNavigationView navView = findViewById(R.id.nav_view);
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        AppBarConfiguration appBarConfiguration = new AppBarConfiguration.Builder(
                R.id.navigation_map, R.id.navigation_request, R.id.navigation_respond)
                .build();
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_activity_main);
        //NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);
        NavigationUI.setupWithNavController(binding.navView, navController);

        navController.addOnDestinationChangedListener(new NavController.OnDestinationChangedListener() {
            @Override
            public void onDestinationChanged(@NonNull NavController controller, @NonNull NavDestination destination, @Nullable Bundle arguments) {
                title.setText(destination.getLabel());
            }
        });
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


    /**To get these values, in fragments:
     *
     *      in onCreateView():
     *          varName = MainActivity.getX();
     *          varName2 = MainActivity.getY();
     *          varName3 = ...
     *
     *      for sample code, see MapFragment.java
     */
    public static String getUserName(){
        return userName;
    }
    public static String getUserId(){
        return userID;
    }
}