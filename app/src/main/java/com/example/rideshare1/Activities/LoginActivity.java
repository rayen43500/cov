package com.example.rideshare1.Activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.example.rideshare1.R;
import com.example.rideshare1.Utils.SessionManager;
import com.example.rideshare1.ViewModels.AuthViewModel;

public class LoginActivity extends AppCompatActivity {

    private EditText etEmail, etPassword;
    private Button btnLogin;
    private TextView tvRegister, tvForgotPassword;
    private AuthViewModel authViewModel;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        tvRegister = findViewById(R.id.tvRegister);
        tvForgotPassword = findViewById(R.id.tvForgotPassword);

        authViewModel = new ViewModelProvider(this).get(AuthViewModel.class);
        sessionManager = new SessionManager(this);

        btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loginUser();
            }
        });

        tvRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(LoginActivity.this, RegisterActivity.class));
            }
        });

        tvForgotPassword.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                resetPassword();
            }
        });

        observeAuthResult();
    }

    private void loginUser() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (TextUtils.isEmpty(email)) {
            etEmail.setError("Email is required");
            return;
        }

        if (TextUtils.isEmpty(password)) {
            etPassword.setError("Password is required");
            return;
        }

        authViewModel.loginUser(email, password);
    }

    private void resetPassword() {
        String email = etEmail.getText().toString().trim();
        if (TextUtils.isEmpty(email)) {
            Toast.makeText(this, "Please enter your email", Toast.LENGTH_SHORT).show();
            return;
        }
        authViewModel.resetPassword(email);
    }

    private void observeAuthResult() {
        authViewModel.getLoginResult().observe(this, result -> {
            if (result != null) {
                if (result.startsWith("SUCCESS:")) {
                    // Format: "SUCCESS:userId:userType"
                    String[] parts = result.substring(8).split(":");
                    if (parts.length >= 2) {
                        String userId = parts[0];
                        String userType = parts[1];
                        
                        // Save session with correct user type
                        sessionManager.createSession(userId, userType, etEmail.getText().toString());
                        
                        // Navigate based on user type
                        Intent intent;
                        if ("driver".equals(userType)) {
                            intent = new Intent(LoginActivity.this, DriverMainActivity.class);
                        } else {
                            intent = new Intent(LoginActivity.this, PassengerMainActivity.class);
                        }
                        startActivity(intent);
                        finish();
                    } else {
                        Toast.makeText(this, "Erreur lors de la récupération des informations utilisateur", Toast.LENGTH_SHORT).show();
                    }
                } else if (result.startsWith("ERROR:")) {
                    String error = result.substring(6);
                    Toast.makeText(this, error, Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
}

