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

import com.example.rideshare1.Activities.ChatActivity;
import com.example.rideshare1.Activities.TripDetailsActivity;
import com.example.rideshare1.Adapters.ReservationAdapter;
import com.example.rideshare1.Models.Reservation;
import com.example.rideshare1.R;
import com.example.rideshare1.Utils.NetworkUtils;
import com.example.rideshare1.Utils.SessionManager;
import com.example.rideshare1.ViewModels.ReservationViewModel;

import java.util.ArrayList;
import java.util.List;

public class ReservationsFragment extends Fragment {

    private static final String TAG = "ReservationsFragment";
    private RecyclerView recyclerView;
    private ReservationAdapter reservationAdapter;
    private ReservationViewModel reservationViewModel;
    private List<Reservation> reservationList;
    private TextView tvEmptyState;
    private SessionManager sessionManager;
    private SwipeRefreshLayout swipeRefreshLayout;
    private ProgressBar progressBar;
    private boolean isDriver;
    private String currentUserId;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_reservations, container, false);

        initializeViews(view);
        setupRecyclerView();
        setupListeners();
        observeReservations();

        loadReservations();

        return view;
    }

    private void initializeViews(View view) {
        recyclerView = view.findViewById(R.id.recyclerViewReservations);
        tvEmptyState = view.findViewById(R.id.tvEmptyState);
        swipeRefreshLayout = view.findViewById(R.id.swipeRefreshLayout);
        progressBar = view.findViewById(R.id.progressBar);
        sessionManager = new SessionManager(getContext());
        currentUserId = sessionManager.getUserId();
        isDriver = "driver".equals(sessionManager.getUserType());

        reservationList = new ArrayList<>();
        reservationViewModel = new ViewModelProvider(this).get(ReservationViewModel.class);
    }

    private void setupRecyclerView() {
        reservationAdapter = new ReservationAdapter(reservationList, isDriver, new ReservationAdapter.OnReservationClickListener() {
            @Override
            public void onReservationClick(Reservation reservation) {
                Intent intent = new Intent(getActivity(), TripDetailsActivity.class);
                intent.putExtra("tripId", reservation.getTripId());
                startActivity(intent);
            }

            @Override
            public void onAcceptClick(Reservation reservation) {
                showAcceptConfirmation(reservation);
            }

            @Override
            public void onRejectClick(Reservation reservation) {
                showRejectConfirmation(reservation);
            }

            @Override
            public void onChatClick(Reservation reservation) {
                openChat(reservation);
            }
        });

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(reservationAdapter);
    }

    private void openChat(Reservation reservation) {
        if (reservation == null) {
            Log.e(TAG, "Reservation is null");
            return;
        }

        String otherUserId;
        
        if (isDriver) {
            // Le conducteur veut chatter avec le passager
            otherUserId = reservation.getPassengerId();
        } else {
            // Le passager veut chatter avec le conducteur
            otherUserId = reservation.getDriverId();
        }

        if (otherUserId == null || otherUserId.isEmpty()) {
            Toast.makeText(getContext(), "Impossible de trouver l'utilisateur", Toast.LENGTH_SHORT).show();
            return;
        }

        if (otherUserId.equals(currentUserId)) {
            Toast.makeText(getContext(), "Erreur: ID utilisateur invalide", Toast.LENGTH_SHORT).show();
            return;
        }

        Log.d(TAG, "Opening chat - isDriver: " + isDriver + ", otherUserId: " + otherUserId);
        Intent intent = new Intent(getActivity(), ChatActivity.class);
        intent.putExtra("otherUserId", otherUserId);
        startActivity(intent);
    }

    private void setupListeners() {
        swipeRefreshLayout.setOnRefreshListener(() -> {
            if (!NetworkUtils.isNetworkAvailable(getContext())) {
                Toast.makeText(getContext(), "Pas de connexion Internet", Toast.LENGTH_SHORT).show();
                swipeRefreshLayout.setRefreshing(false);
                return;
            }
            loadReservations();
        });
    }

    private void showAcceptConfirmation(Reservation reservation) {
        new AlertDialog.Builder(getContext())
                .setTitle("Accepter la réservation")
                .setMessage("Voulez-vous accepter cette demande de réservation ?")
                .setPositiveButton("Accepter", (dialog, which) -> {
                    if (!NetworkUtils.isNetworkAvailable(getContext())) {
                        Toast.makeText(getContext(), "Pas de connexion Internet", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    reservationViewModel.acceptReservation(reservation.getReservationId(), reservation.getTripId(), reservation.getNumberOfSeats());
                })
                .setNegativeButton("Annuler", null)
                .show();
    }

    private void showRejectConfirmation(Reservation reservation) {
        new AlertDialog.Builder(getContext())
                .setTitle("Refuser la réservation")
                .setMessage("Voulez-vous refuser cette demande de réservation ?")
                .setPositiveButton("Refuser", (dialog, which) -> {
                    if (!NetworkUtils.isNetworkAvailable(getContext())) {
                        Toast.makeText(getContext(), "Pas de connexion Internet", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    reservationViewModel.rejectReservation(reservation.getReservationId());
                })
                .setNegativeButton("Annuler", null)
                .show();
    }

    @Override
    public void onResume() {
        super.onResume();
        loadReservations();
    }

    public void refreshReservations() {
        loadReservations();
    }

    private void loadReservations() {
        String userId = sessionManager.getUserId();
        if (userId != null) {
            if (progressBar != null) {
                progressBar.setVisibility(View.VISIBLE);
            }
            if (isDriver) {
                reservationViewModel.getPendingReservationsByDriver(userId);
            } else {
                reservationViewModel.getReservationsByPassenger(userId);
            }
        }
    }

    private void observeReservations() {
        reservationViewModel.getReservationListResult().observe(getViewLifecycleOwner(), reservations -> {
            if (progressBar != null) {
                progressBar.setVisibility(View.GONE);
            }
            swipeRefreshLayout.setRefreshing(false);
            
            if (reservations != null) {
                reservationList.clear();
                reservationList.addAll(reservations);
                reservationAdapter.notifyDataSetChanged();
                
                if (reservations.isEmpty()) {
                    if (tvEmptyState != null) {
                        tvEmptyState.setVisibility(View.VISIBLE);
                        if (isDriver) {
                            tvEmptyState.setText("Aucune demande de réservation");
                        } else {
                            tvEmptyState.setText("Aucune réservation");
                        }
                    }
                } else {
                    if (tvEmptyState != null) {
                        tvEmptyState.setVisibility(View.GONE);
                    }
                }
            }
        });

        reservationViewModel.getErrorMessage().observe(getViewLifecycleOwner(), error -> {
            if (progressBar != null) {
                progressBar.setVisibility(View.GONE);
            }
            swipeRefreshLayout.setRefreshing(false);
            if (error != null && !error.isEmpty()) {
                if (error.contains("accepted") || error.contains("rejected")) {
                    Toast.makeText(getContext(), error, Toast.LENGTH_SHORT).show();
                    loadReservations(); // Refresh after action
                } else {
                    Toast.makeText(getContext(), "Erreur: " + error, Toast.LENGTH_SHORT).show();
                }
            }
        });
        
        reservationViewModel.getIsLoading().observe(getViewLifecycleOwner(), isLoading -> {
            if (!isLoading) {
                if (progressBar != null) {
                    progressBar.setVisibility(View.GONE);
                }
                swipeRefreshLayout.setRefreshing(false);
            }
        });
    }
}
