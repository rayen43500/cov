package com.example.rideshare1.Activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.rideshare1.Fragments.MyTripsFragment;
import com.example.rideshare1.R;
import com.example.rideshare1.Utils.SessionManager;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationBarView;

public class DriverMainActivity extends AppCompatActivity {

    private BottomNavigationView bottomNavigationView;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Vérifier l'authentification et le rôle
        if (!com.example.rideshare1.Utils.AuthGuard.checkRole(this, "driver")) {
            finish();
            return;
        }
        
        setContentView(R.layout.activity_driver_main);

        sessionManager = new SessionManager(this);
        bottomNavigationView = findViewById(R.id.bottomNavigation);

        bottomNavigationView.setOnItemSelectedListener(new NavigationBarView.OnItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                int itemId = item.getItemId();
                if (itemId == R.id.nav_my_trips) {
                    loadMyTripsFragment();
                    return true;
                } else if (itemId == R.id.nav_reservations) {
                    loadReservationsFragment();
                    return true;
                } else if (itemId == R.id.nav_messages) {
                    loadConversationsFragment();
                    return true;
                } else if (itemId == R.id.nav_history) {
                    loadTripHistoryFragment();
                    return true;
                } else if (itemId == R.id.nav_profile) {
                    startActivity(new Intent(DriverMainActivity.this, ProfileActivity.class));
                    return true;
                }
                return false;
            }
        });

        // Load my trips fragment by default
        loadMyTripsFragment();
    }
    
    private void loadMyTripsFragment() {
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragmentContainer, new MyTripsFragment())
                .commit();
    }
    
    private void loadReservationsFragment() {
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragmentContainer, new com.example.rideshare1.Fragments.ReservationsFragment())
                .commit();
    }
    
    private void loadTripHistoryFragment() {
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragmentContainer, new com.example.rideshare1.Fragments.TripHistoryFragment())
                .commit();
    }
    
    private void loadConversationsFragment() {
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragmentContainer, new com.example.rideshare1.Fragments.ConversationsFragment())
                .commit();
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        // Rafraîchir les trajets quand on revient sur l'activité
        MyTripsFragment fragment = (MyTripsFragment) getSupportFragmentManager()
                .findFragmentById(R.id.fragmentContainer);
        if (fragment != null) {
            fragment.refreshTrips();
        }
    }

    public void onLogoutClick(View view) {
        // Logout from Firebase
        com.example.rideshare1.ViewModels.AuthViewModel authViewModel = 
            new androidx.lifecycle.ViewModelProvider(this).get(com.example.rideshare1.ViewModels.AuthViewModel.class);
        authViewModel.logout();
        
        // Clear session
        sessionManager.logout();
        
        // Redirect to login
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}

