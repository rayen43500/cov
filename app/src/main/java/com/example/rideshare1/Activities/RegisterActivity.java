package com.example.rideshare1.Activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.example.rideshare1.R;
import com.example.rideshare1.Utils.SessionManager;
import com.example.rideshare1.ViewModels.AuthViewModel;

public class RegisterActivity extends AppCompatActivity {

    private EditText etFirstName, etLastName, etEmail, etPhone, etPassword, etConfirmPassword;
    private EditText etLicenseNumber, etVehiclePlate, etVehicleModel;
    private RadioGroup rgUserType;
    private RadioButton rbPassenger, rbDriver;
    private Button btnRegister;
    private View driverInfoLayout;
    private AuthViewModel authViewModel;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        initializeViews();
        authViewModel = new ViewModelProvider(this).get(AuthViewModel.class);
        sessionManager = new SessionManager(this);

        rgUserType.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                if (checkedId == R.id.rbDriver) {
                    driverInfoLayout.setVisibility(View.VISIBLE);
                } else {
                    driverInfoLayout.setVisibility(View.GONE);
                }
            }
        });

        btnRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                registerUser();
            }
        });

        observeAuthResult();
    }

    private void initializeViews() {
        etFirstName = findViewById(R.id.etFirstName);
        etLastName = findViewById(R.id.etLastName);
        etEmail = findViewById(R.id.etEmail);
        etPhone = findViewById(R.id.etPhone);
        etPassword = findViewById(R.id.etPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);
        rgUserType = findViewById(R.id.rgUserType);
        rbPassenger = findViewById(R.id.rbPassenger);
        rbDriver = findViewById(R.id.rbDriver);
        btnRegister = findViewById(R.id.btnRegister);
        driverInfoLayout = findViewById(R.id.driverInfoLayout);
        etLicenseNumber = findViewById(R.id.etLicenseNumber);
        etVehiclePlate = findViewById(R.id.etVehiclePlate);
        etVehicleModel = findViewById(R.id.etVehicleModel);
    }

    private void registerUser() {
        String firstName = etFirstName.getText().toString().trim();
        String lastName = etLastName.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String phone = etPhone.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        String confirmPassword = etConfirmPassword.getText().toString().trim();

        if (validateInput(firstName, lastName, email, phone, password, confirmPassword)) {
            int selectedId = rgUserType.getCheckedRadioButtonId();
            if (selectedId == R.id.rbDriver) {
                String licenseNumber = etLicenseNumber.getText().toString().trim();
                String vehiclePlate = etVehiclePlate.getText().toString().trim();
                String vehicleModel = etVehicleModel.getText().toString().trim();

                if (TextUtils.isEmpty(licenseNumber) || TextUtils.isEmpty(vehiclePlate) || 
                    TextUtils.isEmpty(vehicleModel)) {
                    Toast.makeText(this, "Please fill all driver information", Toast.LENGTH_SHORT).show();
                    return;
                }

                authViewModel.registerDriver(email, password, firstName, lastName, phone,
                        licenseNumber, vehiclePlate, vehicleModel);
            } else {
                authViewModel.registerUser(email, password, firstName, lastName, phone, "passenger");
            }
        }
    }

    private boolean validateInput(String firstName, String lastName, String email, 
                                  String phone, String password, String confirmPassword) {
        if (TextUtils.isEmpty(firstName)) {
            etFirstName.setError("First name is required");
            return false;
        }
        if (TextUtils.isEmpty(lastName)) {
            etLastName.setError("Last name is required");
            return false;
        }
        if (TextUtils.isEmpty(email)) {
            etEmail.setError("Email is required");
            return false;
        }
        if (TextUtils.isEmpty(phone)) {
            etPhone.setError("Phone number is required");
            return false;
        }
        if (TextUtils.isEmpty(password)) {
            etPassword.setError("Password is required");
            return false;
        }
        if (password.length() < 6) {
            etPassword.setError("Password must be at least 6 characters");
            return false;
        }
        if (!password.equals(confirmPassword)) {
            etConfirmPassword.setError("Passwords do not match");
            return false;
        }
        return true;
    }

    private void observeAuthResult() {
        authViewModel.getAuthResult().observe(this, result -> {
            if (result != null) {
                if (result.startsWith("SUCCESS:")) {
                    String userId = result.substring(8);
                    String userType = rbDriver.isChecked() ? "driver" : "passenger";
                    sessionManager.createSession(userId, userType, etEmail.getText().toString());
                    
                    Intent intent;
                    if (userType.equals("driver")) {
                        intent = new Intent(RegisterActivity.this, DriverMainActivity.class);
                    } else {
                        intent = new Intent(RegisterActivity.this, PassengerMainActivity.class);
                    }
                    startActivity(intent);
                    finish();
                } else if (result.startsWith("ERROR:")) {
                    String error = result.substring(6);
                    Toast.makeText(this, error, Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
}

