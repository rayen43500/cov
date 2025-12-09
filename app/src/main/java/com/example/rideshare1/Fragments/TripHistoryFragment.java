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
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.rideshare1.Activities.ReviewActivity;
import com.example.rideshare1.Activities.TripDetailsActivity;
import com.example.rideshare1.Adapters.TripHistoryAdapter;
import com.example.rideshare1.Models.Reservation;
import com.example.rideshare1.Models.Trip;
import com.example.rideshare1.R;
import com.example.rideshare1.Repositories.ReservationRepository;
import com.example.rideshare1.Repositories.ReviewRepository;
import com.example.rideshare1.Repositories.TripRepository;
import com.example.rideshare1.Utils.NetworkUtils;
import com.example.rideshare1.Utils.SessionManager;
import com.example.rideshare1.ViewModels.ReservationViewModel;
import com.example.rideshare1.ViewModels.TripViewModel;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class TripHistoryFragment extends Fragment {

    private static final String TAG = "TripHistoryFragment";
    private RecyclerView recyclerView;
    private TripHistoryAdapter tripAdapter;
    private TripViewModel tripViewModel;
    private ReservationViewModel reservationViewModel;
    private TripRepository tripRepository;
    private ReservationRepository reservationRepository;
    private ReviewRepository reviewRepository;
    private List<Trip> tripList;
    private SwipeRefreshLayout swipeRefreshLayout;
    private ProgressBar progressBar;
    private TextView tvEmptyState;
    private SessionManager sessionManager;
    private boolean isDriver;
    private String currentUserId;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_trip_history, container, false);

        recyclerView = view.findViewById(R.id.recyclerViewTrips);
        swipeRefreshLayout = view.findViewById(R.id.swipeRefreshLayout);
        progressBar = view.findViewById(R.id.progressBar);
        tvEmptyState = view.findViewById(R.id.tvEmptyState);
        sessionManager = new SessionManager(getContext());
        currentUserId = sessionManager.getUserId();
        isDriver = "driver".equals(sessionManager.getUserType());

        tripList = new ArrayList<>();
        tripViewModel = new ViewModelProvider(this).get(TripViewModel.class);
        reservationViewModel = new ViewModelProvider(this).get(ReservationViewModel.class);
        tripRepository = new TripRepository();
        reservationRepository = new ReservationRepository();
        reviewRepository = new ReviewRepository();

        setupRecyclerView();
        setupListeners();
        loadTripHistory();
        observeTripList();

        return view;
    }

    private void setupRecyclerView() {
        tripAdapter = new TripHistoryAdapter(tripList, isDriver, currentUserId, new TripHistoryAdapter.OnTripHistoryClickListener() {
            @Override
            public void onTripClick(Trip trip) {
                Intent intent = new Intent(getActivity(), TripDetailsActivity.class);
                intent.putExtra("tripId", trip.getTripId());
                startActivity(intent);
            }

            @Override
            public void onReviewClick(Trip trip) {
                // Vérifier que le trajet est TERMINE avant d'autoriser l'avis
                if (!"completed".equals(trip.getStatus())) {
                    Toast.makeText(getContext(), 
                        "Vous ne pouvez laisser un avis que pour un trajet terminé", 
                        Toast.LENGTH_LONG).show();
                    return;
                }

                // Vérifier si l'utilisateur a déjà laissé un avis pour ce trajet
                reviewRepository.getReviewByTrip(trip.getTripId(), currentUserId, new ReviewRepository.ReviewCallback() {
                    @Override
                    public void onSuccess(com.example.rideshare1.Models.Review review) {
                        // L'utilisateur a déjà laissé un avis
                        Toast.makeText(getContext(), "Vous avez déjà laissé un avis pour ce trajet", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onFailure(String error) {
                        // Pas d'avis trouvé, vérifier que le passager a une réservation acceptée
                        if (error.contains("not found") || error.contains("Review not found")) {
                            reservationRepository.checkPassengerHasAcceptedReservation(
                                trip.getTripId(), currentUserId,
                                new ReservationRepository.ReservationCallback() {
                                    @Override
                                    public void onSuccess(Reservation reservation) {
                                        // Ouvrir l'activité de notation
                                        Intent intent = new Intent(getActivity(), ReviewActivity.class);
                                        intent.putExtra("reviewedUserId", trip.getDriverId());
                                        intent.putExtra("tripId", trip.getTripId());
                                        startActivity(intent);
                                    }

                                    @Override
                                    public void onFailure(String error) {
                                        Toast.makeText(getContext(), 
                                            "Vous devez avoir une réservation acceptée pour ce trajet pour laisser un avis", 
                                            Toast.LENGTH_LONG).show();
                                    }
                                });
                        } else {
                            Toast.makeText(getContext(), "Erreur: " + error, Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }
        });

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(tripAdapter);
    }

    private void setupListeners() {
        swipeRefreshLayout.setOnRefreshListener(() -> {
            if (!NetworkUtils.isNetworkAvailable(getContext())) {
                Toast.makeText(getContext(), "Pas de connexion Internet", Toast.LENGTH_SHORT).show();
                swipeRefreshLayout.setRefreshing(false);
                return;
            }
            loadTripHistory();
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        loadTripHistory();
    }

    public void refreshHistory() {
        loadTripHistory();
    }

    private void loadTripHistory() {
        String userId = sessionManager.getUserId();
        Log.d(TAG, "Loading trip history for user: " + userId + " (isDriver: " + isDriver + ")");
        
        if (userId != null && !userId.isEmpty()) {
            if (progressBar != null) {
                progressBar.setVisibility(View.VISIBLE);
            }
            
            if (isDriver) {
                // Pour les conducteurs, charger tous les trajets puis filtrer les terminés
                tripViewModel.getTripsByDriver(userId);
            } else {
                // Pour les passagers, charger les réservations acceptées/complétées
                reservationViewModel.getAcceptedReservationsByPassenger(userId);
            }
        } else {
            Log.e(TAG, "User ID is null or empty!");
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
            
            if (trips != null && isDriver) {
                // Filtrer pour ne garder que les trajets terminés (status = "completed")
                // Utiliser un Set pour éviter les doublons basés sur tripId
                Set<String> seenTripIds = new HashSet<>();
                List<Trip> historyTrips = new ArrayList<>();
                
                for (Trip trip : trips) {
                    if ("completed".equals(trip.getStatus())) {
                        String tripId = trip.getTripId();
                        // Vérifier que le trajet n'a pas déjà été ajouté
                        if (tripId != null && !seenTripIds.contains(tripId)) {
                            seenTripIds.add(tripId);
                            historyTrips.add(trip);
                        }
                    }
                }
                
                // Trier par date décroissante (plus récents en premier)
                historyTrips.sort((t1, t2) -> {
                    if (t1.getDate() == null || t2.getDate() == null) {
                        return 0;
                    }
                    return t2.getDate().compareTo(t1.getDate());
                });
                
                tripList.clear();
                tripList.addAll(historyTrips);
                tripAdapter.notifyDataSetChanged();
                
                Log.d(TAG, "History trips: " + historyTrips.size());
                
                // Afficher/masquer le message d'état vide
                if (historyTrips.isEmpty()) {
                    if (tvEmptyState != null) {
                        tvEmptyState.setVisibility(View.VISIBLE);
                        tvEmptyState.setText("Aucun trajet terminé");
                    }
                } else {
                    if (tvEmptyState != null) {
                        tvEmptyState.setVisibility(View.GONE);
                    }
                }
            }
        });

        reservationViewModel.getReservationListResult().observe(getViewLifecycleOwner(), reservations -> {
            if (progressBar != null) {
                progressBar.setVisibility(View.GONE);
            }
            swipeRefreshLayout.setRefreshing(false);
            
            Log.d(TAG, "Reservations received: " + (reservations != null ? reservations.size() : 0));
            
            if (reservations != null && !isDriver) {
                // Pour les passagers, charger les trajets correspondant aux réservations acceptées
                // Utiliser un LinkedHashSet pour dédupliquer les tripIds tout en préservant l'ordre
                Set<String> uniqueTripIds = new LinkedHashSet<>();
                for (Reservation reservation : reservations) {
                    String tripId = reservation.getTripId();
                    if (tripId != null && !tripId.isEmpty()) {
                        uniqueTripIds.add(tripId);
                    }
                }
                
                List<String> tripIds = new ArrayList<>(uniqueTripIds);
                
                if (!tripIds.isEmpty()) {
                    loadTripsForReservations(tripIds);
                } else {
                    tripList.clear();
                    tripAdapter.notifyDataSetChanged();
                    if (tvEmptyState != null) {
                        tvEmptyState.setVisibility(View.VISIBLE);
                        tvEmptyState.setText("Aucun trajet terminé");
                    }
                }
            }
        });
    }

    private void loadTripsForReservations(List<String> tripIds) {
        // Utiliser un Set pour éviter les doublons basés sur tripId
        Set<String> seenTripIds = new HashSet<>();
        List<Trip> historyTrips = new ArrayList<>();
        final int[] loadedCount = {0};
        
        for (String tripId : tripIds) {
            tripRepository.getTripById(tripId, new TripRepository.TripCallback() {
                @Override
                public void onSuccess(Trip trip) {
                    // Ne garder que les trajets terminés et éviter les doublons
                    if ("completed".equals(trip.getStatus())) {
                        String tripId = trip.getTripId();
                        if (tripId != null && !seenTripIds.contains(tripId)) {
                            seenTripIds.add(tripId);
                            historyTrips.add(trip);
                        }
                    }
                    
                    loadedCount[0]++;
                    if (loadedCount[0] == tripIds.size()) {
                        // Trier par date décroissante
                        historyTrips.sort((t1, t2) -> {
                            if (t1.getDate() == null || t2.getDate() == null) {
                                return 0;
                            }
                            return t2.getDate().compareTo(t1.getDate());
                        });
                        
                        tripList.clear();
                        tripList.addAll(historyTrips);
                        tripAdapter.notifyDataSetChanged();
                        
                        Log.d(TAG, "Loaded " + historyTrips.size() + " unique completed trips for passenger");
                        
                        if (tvEmptyState != null) {
                            if (historyTrips.isEmpty()) {
                                tvEmptyState.setVisibility(View.VISIBLE);
                                tvEmptyState.setText("Aucun trajet terminé");
                            } else {
                                tvEmptyState.setVisibility(View.GONE);
                            }
                        }
                    }
                }

                @Override
                public void onFailure(String error) {
                    Log.e(TAG, "Error loading trip: " + error);
                    loadedCount[0]++;
                    if (loadedCount[0] == tripIds.size()) {
                        tripList.clear();
                        tripAdapter.notifyDataSetChanged();
                        if (tvEmptyState != null) {
                            tvEmptyState.setVisibility(View.VISIBLE);
                            tvEmptyState.setText("Aucun trajet terminé");
                        }
                    }
                }
            });
        }
    }
}
