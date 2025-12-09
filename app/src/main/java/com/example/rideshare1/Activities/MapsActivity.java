package com.example.rideshare1.Activities;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.example.rideshare1.Models.Trip;
import com.example.rideshare1.R;
import com.example.rideshare1.Utils.GeocodingHelper;
import com.example.rideshare1.ViewModels.TripViewModel;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MapsActivity extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private TripViewModel tripViewModel;
    private String tripId;
    private ExecutorService executorService;
    private ImageView ivBack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Vérifier l'authentification
        if (!com.example.rideshare1.Utils.AuthGuard.requireAuth(this)) {
            finish();
            return;
        }
        
        setContentView(R.layout.activity_maps);

        tripId = getIntent().getStringExtra("tripId");
        tripViewModel = new ViewModelProvider(this).get(TripViewModel.class);
        executorService = Executors.newSingleThreadExecutor();

        ivBack = findViewById(R.id.ivBack);
        ivBack.setOnClickListener(v -> finish());

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        } else {
            Toast.makeText(this, "Erreur: Impossible de charger la carte", Toast.LENGTH_SHORT).show();
        }

        if (tripId != null) {
            loadTripAndDisplayRoute();
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        // Enable zoom controls
        mMap.getUiSettings().setZoomControlsEnabled(true);
        mMap.getUiSettings().setMyLocationButtonEnabled(true);
        
        // If trip is already loaded, display it
        if (tripId != null) {
            loadTripAndDisplayRoute();
        }
    }

    private void loadTripAndDisplayRoute() {
        tripViewModel.getTripById(tripId);
        tripViewModel.getTripResult().observe(this, trip -> {
            if (trip != null) {
                if (mMap != null) {
                    displayRoute(trip);
                } else {
                    // Wait for map to be ready
                    // This will be handled by onMapReady
                }
            } else {
                Toast.makeText(this, "Trajet introuvable", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void displayRoute(Trip trip) {
        // Check if coordinates are available
        boolean hasCoordinates = (trip.getOriginLat() != 0.0 && trip.getOriginLng() != 0.0 &&
                                trip.getDestLat() != 0.0 && trip.getDestLng() != 0.0);

        if (hasCoordinates) {
            // Use existing coordinates
            LatLng origin = new LatLng(trip.getOriginLat(), trip.getOriginLng());
            LatLng destination = new LatLng(trip.getDestLat(), trip.getDestLng());
            addMarkersAndRoute(origin, destination, trip.getOrigin(), trip.getDestination());
        } else {
            // Get coordinates from addresses using Geocoding
            executorService.execute(() -> {
                GeocodingHelper.LocationCoordinates originCoords = 
                    GeocodingHelper.getCoordinatesFromAddress(this, trip.getOrigin());
                GeocodingHelper.LocationCoordinates destCoords = 
                    GeocodingHelper.getCoordinatesFromAddress(this, trip.getDestination());

                runOnUiThread(() -> {
                    if (originCoords.isValid && destCoords.isValid) {
                        LatLng origin = new LatLng(originCoords.latitude, originCoords.longitude);
                        LatLng destination = new LatLng(destCoords.latitude, destCoords.longitude);
                        addMarkersAndRoute(origin, destination, trip.getOrigin(), trip.getDestination());
                    } else {
                        Toast.makeText(this, 
                            "Impossible d'obtenir les coordonnées pour ce trajet", 
                            Toast.LENGTH_LONG).show();
                    }
                });
            });
        }
    }

    private void addMarkersAndRoute(LatLng origin, LatLng destination, String originName, String destName) {
        // Clear existing markers
        mMap.clear();

        // Add markers
        mMap.addMarker(new MarkerOptions()
                .position(origin)
                .title("Départ: " + originName));

        mMap.addMarker(new MarkerOptions()
                .position(destination)
                .title("Arrivée: " + destName));

        // Add polyline (simple straight line)
        mMap.addPolyline(new PolylineOptions()
                .add(origin, destination)
                .width(8)
                .color(getResources().getColor(android.R.color.holo_blue_dark, getTheme())));

        // Move camera to show both points
        LatLng center = new LatLng(
            (origin.latitude + destination.latitude) / 2,
            (origin.longitude + destination.longitude) / 2
        );
        
        // Calculate zoom level based on distance
        float zoom = calculateZoomLevel(origin, destination);
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(center, zoom));
    }

    private float calculateZoomLevel(LatLng origin, LatLng destination) {
        // Simple distance calculation
        double latDiff = Math.abs(origin.latitude - destination.latitude);
        double lngDiff = Math.abs(origin.longitude - destination.longitude);
        double maxDiff = Math.max(latDiff, lngDiff);
        
        if (maxDiff > 0.1) return 8.0f;
        if (maxDiff > 0.05) return 9.0f;
        if (maxDiff > 0.01) return 10.0f;
        if (maxDiff > 0.005) return 11.0f;
        return 12.0f;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (executorService != null) {
            executorService.shutdown();
        }
    }
}
