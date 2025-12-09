package com.example.rideshare1.Activities;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.appcompat.app.AppCompatActivity;

import com.example.rideshare1.R;
import com.example.rideshare1.Utils.AuthGuard;
import com.example.rideshare1.Utils.SessionManager;

public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        SessionManager sessionManager = new SessionManager(this);
        
        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {
                // Vérifier l'authentification Firebase
                if (AuthGuard.isAuthenticated() && sessionManager.isLoggedIn()) {
                    String userType = sessionManager.getUserType();
                    Intent intent;
                    if ("driver".equals(userType)) {
                        intent = new Intent(SplashActivity.this, DriverMainActivity.class);
                    } else {
                        intent = new Intent(SplashActivity.this, PassengerMainActivity.class);
                    }
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                } else {
                    // Si pas authentifié, nettoyer la session et aller au login
                    sessionManager.logout();
                    Intent intent = new Intent(SplashActivity.this, LoginActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                }
                finish();
            }
        }, 2000);
    }
}
