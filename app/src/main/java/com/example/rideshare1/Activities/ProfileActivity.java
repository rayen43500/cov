package com.example.rideshare1.Activities;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.bumptech.glide.Glide;
import com.example.rideshare1.Models.Driver;
import com.example.rideshare1.Models.User;
import com.example.rideshare1.R;
import com.example.rideshare1.Repositories.StorageRepository;
import com.example.rideshare1.Repositories.UserRepository;
import com.example.rideshare1.Utils.ImagePicker;
import com.example.rideshare1.Utils.SessionManager;
import com.example.rideshare1.ViewModels.AuthViewModel;

public class ProfileActivity extends AppCompatActivity {

    private EditText etFirstName, etLastName, etEmail, etPhone;
    private ImageView ivProfilePhoto, ivVehiclePhoto;
    private Button btnUpdate, btnDeleteAccount, btnUploadPhoto, btnUploadVehiclePhoto, btnLogout;
    private TextView tvRating, tvTotalRatings, tvVehicleInfo;
    private SessionManager sessionManager;
    private UserRepository userRepository;
    private StorageRepository storageRepository;
    private User currentUser;
    private Uri selectedImageUri;
    private boolean isDriver;

    private ActivityResultLauncher<Intent> imagePickerLauncher;
    private ActivityResultLauncher<Intent> vehicleImagePickerLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Vérifier l'authentification
        if (!com.example.rideshare1.Utils.AuthGuard.requireAuth(this)) {
            finish();
            return;
        }
        
        setContentView(R.layout.activity_profile);

        initializeViews();
        sessionManager = new SessionManager(this);
        userRepository = new UserRepository();
        storageRepository = new StorageRepository();
        
        // Vérifier si c'est un conducteur
        isDriver = "driver".equals(sessionManager.getUserType());
        
        setupImagePickers();
        loadUserData();

        btnUpdate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                updateProfile();
            }
        });

        btnDeleteAccount.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDeleteConfirmation();
            }
        });

        btnLogout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showLogoutConfirmation();
            }
        });
        
        btnUploadPhoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pickProfileImage();
            }
        });
        
        if (isDriver && btnUploadVehiclePhoto != null) {
            btnUploadVehiclePhoto.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    pickVehicleImage();
                }
            });
        }
    }

    private void setupImagePickers() {
        imagePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    selectedImageUri = result.getData().getData();
                    if (selectedImageUri != null) {
                        uploadProfilePhoto(selectedImageUri);
                    }
                }
            }
        );
        
        vehicleImagePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    selectedImageUri = result.getData().getData();
                    if (selectedImageUri != null) {
                        uploadVehiclePhoto(selectedImageUri);
                    }
                }
            }
        );
    }

    private void initializeViews() {
        etFirstName = findViewById(R.id.etFirstName);
        etLastName = findViewById(R.id.etLastName);
        etEmail = findViewById(R.id.etEmail);
        etPhone = findViewById(R.id.etPhone);
        ivProfilePhoto = findViewById(R.id.ivProfilePhoto);
        btnUpdate = findViewById(R.id.btnUpdate);
        btnDeleteAccount = findViewById(R.id.btnDeleteAccount);
        btnLogout = findViewById(R.id.btnLogout);
        btnUploadPhoto = findViewById(R.id.btnUploadPhoto);
        tvRating = findViewById(R.id.tvRating);
        tvTotalRatings = findViewById(R.id.tvTotalRatings);
        
        // Views pour conducteur
        ivVehiclePhoto = findViewById(R.id.ivVehiclePhoto);
        btnUploadVehiclePhoto = findViewById(R.id.btnUploadVehiclePhoto);
        tvVehicleInfo = findViewById(R.id.tvVehicleInfo);
        
        // Masquer les éléments conducteur si ce n'est pas un conducteur
        if (!isDriver) {
            if (ivVehiclePhoto != null) ivVehiclePhoto.setVisibility(View.GONE);
            if (btnUploadVehiclePhoto != null) btnUploadVehiclePhoto.setVisibility(View.GONE);
            if (tvVehicleInfo != null) tvVehicleInfo.setVisibility(View.GONE);
        }
    }

    private void loadUserData() {
        String userId = sessionManager.getUserId();
        if (userId != null) {
            userRepository.getUserById(userId, new UserRepository.UserCallback() {
                @Override
                public void onSuccess(User user) {
                    currentUser = user;
                    etFirstName.setText(user.getFirstName());
                    etLastName.setText(user.getLastName());
                    etEmail.setText(user.getEmail());
                    etPhone.setText(user.getPhoneNumber());
                    tvRating.setText(String.format("%.1f ⭐", user.getRating()));
                    tvTotalRatings.setText(String.format("(%d avis)", user.getTotalRatings()));
                    
                    // Charger la photo de profil
                    if (user.getProfilePhotoUrl() != null && !user.getProfilePhotoUrl().isEmpty()) {
                        Glide.with(ProfileActivity.this)
                                .load(user.getProfilePhotoUrl())
                                .circleCrop()
                                .placeholder(R.drawable.ic_launcher_foreground)
                                .into(ivProfilePhoto);
                    }
                    
                    // Si conducteur, charger les infos du véhicule
                    if (isDriver && user instanceof Driver) {
                        Driver driver = (Driver) user;
                        if (driver.getVehiclePhotoUrl() != null && !driver.getVehiclePhotoUrl().isEmpty()) {
                            Glide.with(ProfileActivity.this)
                                    .load(driver.getVehiclePhotoUrl())
                                    .placeholder(R.drawable.ic_launcher_foreground)
                                    .into(ivVehiclePhoto);
                        }
                        if (tvVehicleInfo != null && driver.getVehicleModel() != null) {
                            tvVehicleInfo.setText(String.format("%s - %s", 
                                driver.getVehicleModel(), 
                                driver.getVehiclePlate() != null ? driver.getVehiclePlate() : ""));
                        }
                    }
                }

                @Override
                public void onFailure(String error) {
                    Toast.makeText(ProfileActivity.this, error, Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private void pickProfileImage() {
        Intent intent = ImagePicker.getPickImageIntent();
        imagePickerLauncher.launch(intent);
    }
    
    private void pickVehicleImage() {
        Intent intent = ImagePicker.getPickImageIntent();
        vehicleImagePickerLauncher.launch(intent);
    }

    private void uploadProfilePhoto(Uri imageUri) {
        String userId = sessionManager.getUserId();
        if (userId == null) return;
        
        Toast.makeText(this, "Upload en cours...", Toast.LENGTH_SHORT).show();
        
        storageRepository.uploadProfilePhoto(userId, imageUri, new StorageRepository.UploadCallback() {
            @Override
            public void onSuccess(String downloadUrl) {
                userRepository.updateProfilePhoto(userId, downloadUrl, new UserRepository.UpdateCallback() {
                    @Override
                    public void onSuccess() {
                        Toast.makeText(ProfileActivity.this, "Photo de profil mise à jour", Toast.LENGTH_SHORT).show();
                        Glide.with(ProfileActivity.this)
                                .load(downloadUrl)
                                .circleCrop()
                                .into(ivProfilePhoto);
                    }

                    @Override
                    public void onFailure(String error) {
                        Toast.makeText(ProfileActivity.this, error, Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onFailure(String error) {
                Toast.makeText(ProfileActivity.this, "Erreur upload: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    private void uploadVehiclePhoto(Uri imageUri) {
        String userId = sessionManager.getUserId();
        if (userId == null) return;
        
        Toast.makeText(this, "Upload en cours...", Toast.LENGTH_SHORT).show();
        
        storageRepository.uploadVehiclePhoto(userId, imageUri, new StorageRepository.UploadCallback() {
            @Override
            public void onSuccess(String downloadUrl) {
                java.util.Map<String, Object> updates = new java.util.HashMap<>();
                updates.put("vehiclePhotoUrl", downloadUrl);
                
                userRepository.updateUser(userId, updates, new UserRepository.UpdateCallback() {
                    @Override
                    public void onSuccess() {
                        Toast.makeText(ProfileActivity.this, "Photo du véhicule mise à jour", Toast.LENGTH_SHORT).show();
                        Glide.with(ProfileActivity.this)
                                .load(downloadUrl)
                                .into(ivVehiclePhoto);
                    }

                    @Override
                    public void onFailure(String error) {
                        Toast.makeText(ProfileActivity.this, error, Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onFailure(String error) {
                Toast.makeText(ProfileActivity.this, "Erreur upload: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateProfile() {
        if (currentUser == null) return;

        java.util.Map<String, Object> updates = new java.util.HashMap<>();
        updates.put("firstName", etFirstName.getText().toString().trim());
        updates.put("lastName", etLastName.getText().toString().trim());
        updates.put("phoneNumber", etPhone.getText().toString().trim());

        userRepository.updateUser(currentUser.getUserId(), updates, new UserRepository.UpdateCallback() {
            @Override
            public void onSuccess() {
                Toast.makeText(ProfileActivity.this, "Profil mis à jour avec succès", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onFailure(String error) {
                Toast.makeText(ProfileActivity.this, error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showDeleteConfirmation() {
        new AlertDialog.Builder(this)
                .setTitle("Supprimer le compte")
                .setMessage("Êtes-vous sûr de vouloir supprimer votre compte ? Cette action est irréversible.")
                .setPositiveButton("Supprimer", (dialog, which) -> {
                    deleteAccount();
                })
                .setNegativeButton("Annuler", null)
                .show();
    }

    private void deleteAccount() {
        String userId = sessionManager.getUserId();
        if (userId != null) {
            userRepository.deleteUser(userId, new UserRepository.UpdateCallback() {
                @Override
                public void onSuccess() {
                    AuthViewModel authViewModel = new ViewModelProvider(ProfileActivity.this).get(AuthViewModel.class);
                    authViewModel.logout();
                    sessionManager.logout();
                    Toast.makeText(ProfileActivity.this, "Compte supprimé", Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(ProfileActivity.this, LoginActivity.class));
                    finish();
                }

                @Override
                public void onFailure(String error) {
                    Toast.makeText(ProfileActivity.this, error, Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private void showLogoutConfirmation() {
        new AlertDialog.Builder(this)
                .setTitle("Déconnexion")
                .setMessage("Êtes-vous sûr de vouloir vous déconnecter ?")
                .setPositiveButton("Déconnexion", (dialog, which) -> {
                    logout();
                })
                .setNegativeButton("Annuler", null)
                .show();
    }

    private void logout() {
        AuthViewModel authViewModel = new ViewModelProvider(ProfileActivity.this).get(AuthViewModel.class);
        authViewModel.logout();
        sessionManager.logout();
        Toast.makeText(ProfileActivity.this, "Déconnexion réussie", Toast.LENGTH_SHORT).show();
        startActivity(new Intent(ProfileActivity.this, LoginActivity.class));
        finish();
    }
}
