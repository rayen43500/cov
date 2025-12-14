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
            if (result != null && !result.isEmpty()) {
                if (result.startsWith("SUCCESS:")) {
                    try {
                        // Format: "SUCCESS:userId:userType"
                        String data = result.substring(8); // Enlever "SUCCESS:"
                        String[] parts = data.split(":");
                        
                        if (parts.length >= 2) {
                            String userId = parts[0];
                            String userType = parts[1];
                            
                            // Vérifier que le userType est valide
                            if (!"driver".equals(userType) && !"passenger".equals(userType)) {
                                Toast.makeText(this, "Type d'utilisateur invalide", Toast.LENGTH_SHORT).show();
                                return;
                            }
                            
                            // Save session with correct user type
                            String email = etEmail.getText().toString().trim();
                            sessionManager.createSession(userId, userType, email);
                            
                            // Navigate based on user type
                            Intent intent;
                            if ("driver".equals(userType)) {
                                intent = new Intent(LoginActivity.this, DriverMainActivity.class);
                            } else {
                                intent = new Intent(LoginActivity.this, PassengerMainActivity.class);
                            }
                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            startActivity(intent);
                            finish();
                        } else {
                            Toast.makeText(this, "Erreur : Format de réponse invalide", Toast.LENGTH_SHORT).show();
                        }
                    } catch (Exception e) {
                        Toast.makeText(this, "Erreur lors de la connexion : " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                } else if (result.startsWith("ERROR:")) {
                    String error = result.substring(6);
                    // Traduire les erreurs Firebase en français
                    String translatedError = translateFirebaseError(error);
                    Toast.makeText(this, translatedError, Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(this, "Réponse inattendue du serveur", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
    
    /**
     * Traduit les erreurs Firebase en français pour une meilleure expérience utilisateur
     */
    private String translateFirebaseError(String error) {
        if (error == null) return "Une erreur est survenue";
        
        String lowerError = error.toLowerCase();
        if (lowerError.contains("password") && lowerError.contains("invalid")) {
            return "Mot de passe incorrect";
        } else if (lowerError.contains("user") && (lowerError.contains("not found") || lowerError.contains("doesn't exist"))) {
            return "Aucun compte trouvé avec cet email";
        } else if (lowerError.contains("email") && lowerError.contains("badly formatted")) {
            return "Format d'email invalide";
        } else if (lowerError.contains("network")) {
            return "Problème de connexion Internet";
        } else if (lowerError.contains("too many requests")) {
            return "Trop de tentatives. Veuillez réessayer plus tard";
        } else if (lowerError.contains("user disabled")) {
            return "Ce compte a été désactivé";
        }
        return error; // Retourner l'erreur originale si non traduite
    }
}

