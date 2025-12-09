package com.example.rideshare1.Activities;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.example.rideshare1.Models.Trip;
import com.example.rideshare1.R;
import com.example.rideshare1.Utils.FormValidator;
import com.example.rideshare1.Utils.GeocodingHelper;
import com.example.rideshare1.Utils.NetworkUtils;
import com.example.rideshare1.Utils.SessionManager;
import com.example.rideshare1.ViewModels.TripViewModel;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.UUID;

public class CreateTripActivity extends AppCompatActivity {

    private TextInputEditText etOrigin, etDestination, etDate, etTime, etPrice, etSeats;
    private TextInputLayout tilOrigin, tilDestination, tilDate, tilTime, tilPrice, tilSeats;
    private MaterialButton btnCreate;
    private TripViewModel tripViewModel;
    private SessionManager sessionManager;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Vérifier que l'utilisateur est un conducteur
        if (!com.example.rideshare1.Utils.AuthGuard.checkRole(this, "driver")) {
            finish();
            return;
        }
        
        setContentView(R.layout.activity_create_trip);

        initializeViews();
        tripViewModel = new ViewModelProvider(this).get(TripViewModel.class);
        sessionManager = new SessionManager(this);

        setupDateAndTimePickers();
        setupListeners();
        observeTripResult();
    }

    private void initializeViews() {
        etOrigin = findViewById(R.id.etOrigin);
        etDestination = findViewById(R.id.etDestination);
        etDate = findViewById(R.id.etDate);
        etTime = findViewById(R.id.etTime);
        etPrice = findViewById(R.id.etPrice);
        etSeats = findViewById(R.id.etSeats);
        btnCreate = findViewById(R.id.btnCreate);
        progressBar = findViewById(R.id.progressBar);
        
        tilOrigin = findViewById(R.id.tilOrigin);
        tilDestination = findViewById(R.id.tilDestination);
        tilDate = findViewById(R.id.tilDate);
        tilTime = findViewById(R.id.tilTime);
        tilPrice = findViewById(R.id.tilPrice);
        tilSeats = findViewById(R.id.tilSeats);
    }
    
    private void setupDateAndTimePickers() {
        // Setup Date Picker
        etDate.setFocusable(false);
        etDate.setOnClickListener(v -> showDatePicker());
        
        // Setup Time Picker
        etTime.setFocusable(false);
        etTime.setOnClickListener(v -> showTimePicker());
    }
    
    private void setupListeners() {
        btnCreate.setOnClickListener(v -> {
            if (!NetworkUtils.isNetworkAvailable(this)) {
                Toast.makeText(this, "Pas de connexion Internet", Toast.LENGTH_SHORT).show();
                return;
            }
            createTrip();
        });
    }
    
    private void showDatePicker() {
        Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);
        
        DatePickerDialog datePickerDialog = new DatePickerDialog(
            this,
            (view, selectedYear, selectedMonth, selectedDay) -> {
                calendar.set(selectedYear, selectedMonth, selectedDay);
                SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                etDate.setText(sdf.format(calendar.getTime()));
                if (tilDate != null) {
                    tilDate.setError(null);
                }
            },
            year, month, day
        );
        
        // Set minimum date to today
        datePickerDialog.getDatePicker().setMinDate(System.currentTimeMillis() - 1000);
        datePickerDialog.show();
    }
    
    private void showTimePicker() {
        Calendar calendar = Calendar.getInstance();
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        int minute = calendar.get(Calendar.MINUTE);
        
        TimePickerDialog timePickerDialog = new TimePickerDialog(
            this,
            (view, selectedHour, selectedMinute) -> {
                String time = String.format(Locale.getDefault(), "%02d:%02d", selectedHour, selectedMinute);
                etTime.setText(time);
                if (tilTime != null) {
                    tilTime.setError(null);
                }
            },
            hour, minute, true
        );
        timePickerDialog.show();
    }

    private void createTrip() {
        String origin = etOrigin.getText().toString().trim();
        String destination = etDestination.getText().toString().trim();
        String dateStr = etDate.getText().toString().trim();
        String time = etTime.getText().toString().trim();
        String priceStr = etPrice.getText().toString().trim();
        String seatsStr = etSeats.getText().toString().trim();

        // Clear previous errors
        clearErrors();

        if (!validateInput(origin, destination, dateStr, time, priceStr, seatsStr)) {
            return;
        }

        try {
            Date date = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).parse(dateStr);
            double price = Double.parseDouble(priceStr);
            int seats = Integer.parseInt(seatsStr);

            // Validate date is not in the past
            if (date.before(new Date())) {
                if (tilDate != null) {
                    tilDate.setError("La date ne peut pas être dans le passé");
                }
                return;
            }

            String tripId = UUID.randomUUID().toString();
            String driverId = sessionManager.getUserId();
            
            if (driverId == null || driverId.isEmpty()) {
                Toast.makeText(this, "Erreur: ID utilisateur non trouvé", Toast.LENGTH_SHORT).show();
                if (progressBar != null) {
                    progressBar.setVisibility(View.GONE);
                }
                btnCreate.setEnabled(true);
                return;
            }

            Trip trip = new Trip(tripId, driverId, origin, destination, date, time, price, seats);
            trip.setStatus("active"); // S'assurer que le statut est défini
            
            // Show loading
            if (progressBar != null) {
                progressBar.setVisibility(View.VISIBLE);
            }
            btnCreate.setEnabled(false);
            
            // Get coordinates from addresses
            new Thread(() -> {
                GeocodingHelper.LocationCoordinates originCoords = 
                    GeocodingHelper.getCoordinatesFromAddress(CreateTripActivity.this, origin);
                GeocodingHelper.LocationCoordinates destCoords = 
                    GeocodingHelper.getCoordinatesFromAddress(CreateTripActivity.this, destination);
                
                runOnUiThread(() -> {
                    if (originCoords.isValid) {
                        trip.setOriginLat(originCoords.latitude);
                        trip.setOriginLng(originCoords.longitude);
                    }
                    if (destCoords.isValid) {
                        trip.setDestLat(destCoords.latitude);
                        trip.setDestLng(destCoords.longitude);
                    }
                    
                    // Log pour déboguer
                    android.util.Log.d("CreateTrip", "Creating trip with driverId: " + driverId);
                    android.util.Log.d("CreateTrip", "Trip ID: " + tripId);
                    android.util.Log.d("CreateTrip", "Origin: " + origin + " -> " + destination);
                    
                    tripViewModel.createTrip(trip);
                });
            }).start();
        } catch (Exception e) {
            Toast.makeText(this, "Format invalide", Toast.LENGTH_SHORT).show();
            if (progressBar != null) {
                progressBar.setVisibility(View.GONE);
            }
            btnCreate.setEnabled(true);
        }
    }

    private void clearErrors() {
        if (tilOrigin != null) tilOrigin.setError(null);
        if (tilDestination != null) tilDestination.setError(null);
        if (tilDate != null) tilDate.setError(null);
        if (tilTime != null) tilTime.setError(null);
        if (tilPrice != null) tilPrice.setError(null);
        if (tilSeats != null) tilSeats.setError(null);
    }

    private boolean validateInput(String origin, String destination, String date, 
                                  String time, String price, String seats) {
        boolean isValid = true;

        if (TextUtils.isEmpty(origin) || !FormValidator.isValidName(origin)) {
            if (tilOrigin != null) {
                tilOrigin.setError("Ville de départ requise (min 2 caractères)");
            }
            isValid = false;
        }

        if (TextUtils.isEmpty(destination) || !FormValidator.isValidName(destination)) {
            if (tilDestination != null) {
                tilDestination.setError("Ville de destination requise (min 2 caractères)");
            }
            isValid = false;
        }

        if (TextUtils.isEmpty(date)) {
            if (tilDate != null) {
                tilDate.setError("Date requise");
            }
            isValid = false;
        }

        if (TextUtils.isEmpty(time)) {
            if (tilTime != null) {
                tilTime.setError("Heure requise");
            }
            isValid = false;
        }

        if (TextUtils.isEmpty(price) || !FormValidator.isValidPrice(price)) {
            if (tilPrice != null) {
                tilPrice.setError("Prix invalide (doit être > 0)");
            }
            isValid = false;
        }

        if (TextUtils.isEmpty(seats) || !FormValidator.isValidSeats(seats)) {
            if (tilSeats != null) {
                tilSeats.setError("Nombre de places invalide (1-8)");
            }
            isValid = false;
        }

        return isValid;
    }

    private void observeTripResult() {
        tripViewModel.getTripResult().observe(this, trip -> {
            if (progressBar != null) {
                progressBar.setVisibility(View.GONE);
            }
            btnCreate.setEnabled(true);
            
            if (trip != null) {
                Toast.makeText(this, "Trajet créé avec succès", Toast.LENGTH_SHORT).show();
                Intent resultIntent = new Intent();
                resultIntent.putExtra("tripCreated", true);
                setResult(RESULT_OK, resultIntent);
                finish();
            }
        });

        tripViewModel.getErrorMessage().observe(this, error -> {
            if (progressBar != null) {
                progressBar.setVisibility(View.GONE);
            }
            btnCreate.setEnabled(true);
            
            if (error != null && !error.isEmpty() && !error.contains("successfully")) {
                Toast.makeText(this, "Erreur: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }
}
