package com.example.rideshare1.Fragments;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.rideshare1.Activities.CreateTripActivity;
import com.example.rideshare1.Activities.TripDetailsActivity;
import com.example.rideshare1.Adapters.TripAdapter;
import com.example.rideshare1.Models.Trip;
import com.example.rideshare1.R;
import com.example.rideshare1.Utils.NetworkUtils;
import com.example.rideshare1.Utils.SessionManager;
import com.example.rideshare1.ViewModels.TripViewModel;
import com.google.android.material.button.MaterialButton;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MyTripsFragment extends Fragment {

    private static final String TAG = "MyTripsFragment";
    private RecyclerView recyclerView;
    private TripAdapter tripAdapter;
    private TripViewModel tripViewModel;
    private List<Trip> tripList;
    private MaterialButton btnCreateTrip;
    private TextView tvEmptyState;
    private SessionManager sessionManager;
    private SwipeRefreshLayout swipeRefreshLayout;
    private ProgressBar progressBar;
    private String currentUserId;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_my_trips, container, false);

        recyclerView = view.findViewById(R.id.recyclerViewTrips);
        btnCreateTrip = view.findViewById(R.id.btnCreateTrip);
        tvEmptyState = view.findViewById(R.id.tvEmptyState);
        swipeRefreshLayout = view.findViewById(R.id.swipeRefreshLayout);
        progressBar = view.findViewById(R.id.progressBar);
        sessionManager = new SessionManager(getContext());
        currentUserId = sessionManager.getUserId();

        tripList = new ArrayList<>();
        tripAdapter = new TripAdapter(tripList, new TripAdapter.OnTripClickListener() {
            @Override
            public void onTripClick(Trip trip) {
                Intent intent = new Intent(getActivity(), TripDetailsActivity.class);
                intent.putExtra("tripId", trip.getTripId());
                startActivity(intent);
            }

            @Override
            public void onStartTripClick(Trip trip) {
                handleStartTrip(trip);
            }

            @Override
            public void onEndTripClick(Trip trip) {
                handleEndTrip(trip);
            }
        }, true, currentUserId);

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(tripAdapter);

        tripViewModel = new ViewModelProvider(this).get(TripViewModel.class);

        btnCreateTrip.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getActivity(), CreateTripActivity.class);
                startActivityForResult(intent, 100);
            }
        });

        swipeRefreshLayout.setOnRefreshListener(() -> {
            if (!NetworkUtils.isNetworkAvailable(getContext())) {
                Toast.makeText(getContext(), "Pas de connexion Internet", Toast.LENGTH_SHORT).show();
                swipeRefreshLayout.setRefreshing(false);
                return;
            }
            loadTrips();
        });

        loadTrips();
        observeTripList();

        return view;
    }

    private void handleStartTrip(Trip trip) {
        // Vérifier que le trajet est bien "active"
        if (!"active".equals(trip.getStatus())) {
            Toast.makeText(getContext(), "Ce trajet ne peut pas être démarré (statut: " + trip.getStatus() + ")", Toast.LENGTH_LONG).show();
            return;
        }

        new AlertDialog.Builder(getContext())
                .setTitle("Démarrer le trajet")
                .setMessage("Voulez-vous démarrer ce trajet ? Le statut passera à 'En cours'.")
                .setPositiveButton("Démarrer", (dialog, which) -> {
                    startTrip(trip);
                })
                .setNegativeButton("Annuler", null)
                .show();
    }

    private void handleEndTrip(Trip trip) {
        // Vérifier que le trajet est bien "in_progress"
        if (!"in_progress".equals(trip.getStatus())) {
            Toast.makeText(getContext(), "Ce trajet ne peut pas être terminé (statut: " + trip.getStatus() + ")", Toast.LENGTH_LONG).show();
            return;
        }
        
        new AlertDialog.Builder(getContext())
                .setTitle("Terminer le trajet")
                .setMessage("Voulez-vous terminer ce trajet ? Le statut passera à 'Terminé' et les passagers pourront laisser des avis.")
                .setPositiveButton("Terminer", (dialog, which) -> {
                    endTrip(trip);
                })
                .setNegativeButton("Annuler", null)
                .show();
    }

    private void startTrip(Trip trip) {
        if (!NetworkUtils.isNetworkAvailable(getContext())) {
            Toast.makeText(getContext(), "Pas de connexion Internet", Toast.LENGTH_SHORT).show();
            return;
        }

        java.util.Map<String, Object> updates = new java.util.HashMap<>();
        updates.put("status", "in_progress");

        tripViewModel.updateTrip(trip.getTripId(), updates);
        
        tripViewModel.getErrorMessage().observe(getViewLifecycleOwner(), error -> {
            if (error != null && !error.isEmpty()) {
                if (error.contains("successfully")) {
                    Toast.makeText(getContext(), "Trajet démarré avec succès", Toast.LENGTH_SHORT).show();
                    loadTrips(); // Recharger la liste
                } else {
                    Toast.makeText(getContext(), "Erreur: " + error, Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void endTrip(Trip trip) {
        if (!NetworkUtils.isNetworkAvailable(getContext())) {
            Toast.makeText(getContext(), "Pas de connexion Internet", Toast.LENGTH_SHORT).show();
            return;
        }

        java.util.Map<String, Object> updates = new java.util.HashMap<>();
        updates.put("status", "completed");

        tripViewModel.updateTrip(trip.getTripId(), updates);
        
        tripViewModel.getErrorMessage().observe(getViewLifecycleOwner(), error -> {
            if (error != null && !error.isEmpty()) {
                if (error.contains("successfully")) {
                    Toast.makeText(getContext(), "Trajet terminé avec succès", Toast.LENGTH_SHORT).show();
                    loadTrips(); // Recharger la liste
                } else {
                    Toast.makeText(getContext(), "Erreur: " + error, Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    public void refreshTrips() {
        Log.d(TAG, "Refreshing trips...");
        loadTrips();
    }
    
    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "Fragment resumed, refreshing trips...");
        // Rafraîchir les trajets quand le fragment devient visible
        loadTrips();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.d(TAG, "Activity result received: requestCode=" + requestCode + ", resultCode=" + resultCode);
        if (requestCode == 100 && resultCode == android.app.Activity.RESULT_OK) {
            Log.d(TAG, "Trip created successfully, refreshing list...");
            // Attendre un peu pour que Firestore soit synchronisé
            recyclerView.postDelayed(() -> loadTrips(), 500);
        }
    }

    private void loadTrips() {
        String driverId = sessionManager.getUserId();
        Log.d(TAG, "Loading trips for driver: " + driverId);
        
        if (driverId != null && !driverId.isEmpty()) {
            if (progressBar != null) {
                progressBar.setVisibility(View.VISIBLE);
            }
            tripViewModel.getTripsByDriver(driverId);
        } else {
            Log.e(TAG, "Driver ID is null or empty!");
            Toast.makeText(getContext(), "Erreur: ID utilisateur non trouvé", Toast.LENGTH_SHORT).show();
            if (progressBar != null) {
                progressBar.setVisibility(View.GONE);
            }
        }
    }

    private void observeTripList() {
        tripViewModel.getTripListResult().observe(getViewLifecycleOwner(), trips -> {
            if (progressBar != null) {
                progressBar.setVisibility(View.GONE);
            }
            swipeRefreshLayout.setRefreshing(false);
            
            Log.d(TAG, "Trips received: " + (trips != null ? trips.size() : 0));
            
            if (trips != null) {
                tripList.clear();
                tripList.addAll(trips);
                tripAdapter.notifyDataSetChanged();
                
                Log.d(TAG, "Trip list updated with " + trips.size() + " trips");
                
                // Afficher/masquer le message d'état vide
                if (trips.isEmpty()) {
                    if (tvEmptyState != null) {
                        tvEmptyState.setVisibility(View.VISIBLE);
                        tvEmptyState.setText("Aucun trajet créé\nCréez votre premier trajet !");
                    }
                    Log.d(TAG, "No trips found, showing empty state");
                } else {
                    if (tvEmptyState != null) {
                        tvEmptyState.setVisibility(View.GONE);
                    }
                    Log.d(TAG, "Trips found, hiding empty state");
                }
            } else {
                Log.w(TAG, "Trips list is null");
            }
        });

        tripViewModel.getErrorMessage().observe(getViewLifecycleOwner(), error -> {
            if (progressBar != null) {
                progressBar.setVisibility(View.GONE);
            }
            swipeRefreshLayout.setRefreshing(false);
            if (error != null && !error.isEmpty() && !error.contains("successfully")) {
                Log.e(TAG, "Error loading trips: " + error);
                Toast.makeText(getContext(), "Erreur: " + error, Toast.LENGTH_SHORT).show();
            }
        });
        
        tripViewModel.getIsLoading().observe(getViewLifecycleOwner(), isLoading -> {
            Log.d(TAG, "Loading state: " + isLoading);
            if (!isLoading) {
                if (progressBar != null) {
                    progressBar.setVisibility(View.GONE);
                }
                swipeRefreshLayout.setRefreshing(false);
            }
        });
    }
}
