package com.example.rideshare1.Activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.rideshare1.Adapters.ReviewAdapter;
import com.example.rideshare1.Models.Reservation;
import com.example.rideshare1.Models.Review;
import com.example.rideshare1.Models.Trip;
import com.example.rideshare1.R;
import com.example.rideshare1.Repositories.ReservationRepository;
import com.example.rideshare1.Repositories.ReviewRepository;
import com.example.rideshare1.Utils.NetworkUtils;
import com.example.rideshare1.Utils.SessionManager;
import com.example.rideshare1.ViewModels.ReservationViewModel;
import com.example.rideshare1.ViewModels.TripViewModel;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public class TripDetailsActivity extends AppCompatActivity {

    private static final String TAG = "TripDetailsActivity";
    private TextView tvOrigin, tvDestination, tvDate, tvTime, tvPrice, tvSeats;
    private MaterialButton btnReserve, btnViewMap, btnChat, btnViewReviews;
    private ImageView ivBack;
    private TripViewModel tripViewModel;
    private ReservationViewModel reservationViewModel;
    private ReviewRepository reviewRepository;
    private ReservationRepository reservationRepository;
    private SessionManager sessionManager;
    private Trip currentTrip;
    private String tripId;
    private ProgressBar progressBar;
    private boolean isDriver;
    private String currentUserId;
    private MaterialCardView reviewsCard;
    private RecyclerView recyclerViewReviews;
    private TextView tvAverageRating, tvReviewsCount;
    private ReviewAdapter reviewAdapter;
    private List<Review> reviewList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Vérifier l'authentification
        if (!com.example.rideshare1.Utils.AuthGuard.requireAuth(this)) {
            finish();
            return;
        }
        
        setContentView(R.layout.activity_trip_details);

        tripId = getIntent().getStringExtra("tripId");
        if (tripId == null) {
            finish();
            return;
        }

        initializeViews();
        tripViewModel = new ViewModelProvider(this).get(TripViewModel.class);
        reservationViewModel = new ViewModelProvider(this).get(ReservationViewModel.class);
        reviewRepository = new ReviewRepository();
        reservationRepository = new ReservationRepository();
        sessionManager = new SessionManager(this);
        currentUserId = sessionManager.getUserId();
        isDriver = "driver".equals(sessionManager.getUserType());

        reviewList = new ArrayList<>();
        reviewAdapter = new ReviewAdapter(reviewList);
        recyclerViewReviews.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewReviews.setAdapter(reviewAdapter);

        ivBack.setOnClickListener(v -> finish());
        loadTripDetails();

        btnReserve.setOnClickListener(v -> {
            if (!NetworkUtils.isNetworkAvailable(this)) {
                Toast.makeText(this, "Pas de connexion Internet", Toast.LENGTH_SHORT).show();
                return;
            }
            showReservationDialog();
        });

        btnViewMap.setOnClickListener(v -> {
            Intent intent = new Intent(TripDetailsActivity.this, MapsActivity.class);
            intent.putExtra("tripId", tripId);
            startActivity(intent);
        });

        btnChat.setOnClickListener(v -> {
            if (currentTrip != null) {
                openChat();
            } else {
                Toast.makeText(this, "Chargement des détails du trajet...", Toast.LENGTH_SHORT).show();
            }
        });

        btnViewReviews.setOnClickListener(v -> {
            if (currentTrip != null) {
                loadDriverReviews(currentTrip.getDriverId());
            }
        });

        observeTripResult();
        observeReservationResult();
    }

    private void openChat() {
        if (currentTrip == null) {
            Log.e(TAG, "Current trip is null");
            return;
        }

        String otherUserId;
        
        if (isDriver) {
            // Si c'est le conducteur, il veut chatter avec le passager
            // Pour l'instant, on ne peut chatter qu'avec le conducteur du trajet
            // On pourrait améliorer cela en récupérant les passagers depuis les réservations
            Toast.makeText(this, "Fonctionnalité en développement", Toast.LENGTH_SHORT).show();
            return;
        } else {
            // Si c'est un passager, il veut chatter avec le conducteur
            otherUserId = currentTrip.getDriverId();
        }

        if (otherUserId == null || otherUserId.isEmpty()) {
            Toast.makeText(this, "Impossible de trouver l'utilisateur", Toast.LENGTH_SHORT).show();
            return;
        }

        if (otherUserId.equals(currentUserId)) {
            Toast.makeText(this, "Vous ne pouvez pas chatter avec vous-même", Toast.LENGTH_SHORT).show();
            return;
        }

        Log.d(TAG, "Opening chat with: " + otherUserId);
        Intent intent = new Intent(TripDetailsActivity.this, ChatActivity.class);
        intent.putExtra("otherUserId", otherUserId);
        startActivity(intent);
    }

    private void initializeViews() {
        tvOrigin = findViewById(R.id.tvOrigin);
        tvDestination = findViewById(R.id.tvDestination);
        tvDate = findViewById(R.id.tvDate);
        tvTime = findViewById(R.id.tvTime);
        tvPrice = findViewById(R.id.tvPrice);
        tvSeats = findViewById(R.id.tvSeats);
        btnReserve = findViewById(R.id.btnReserve);
        btnViewMap = findViewById(R.id.btnViewMap);
        btnChat = findViewById(R.id.btnChat);
        btnViewReviews = findViewById(R.id.btnViewReviews);
        ivBack = findViewById(R.id.ivBack);
        progressBar = findViewById(R.id.progressBar);
        reviewsCard = findViewById(R.id.reviewsCard);
        recyclerViewReviews = findViewById(R.id.recyclerViewReviews);
        tvAverageRating = findViewById(R.id.tvAverageRating);
        tvReviewsCount = findViewById(R.id.tvReviewsCount);
    }

    private void loadTripDetails() {
        if (progressBar != null) {
            progressBar.setVisibility(View.VISIBLE);
        }
        tripViewModel.getTripById(tripId);
    }

    private void loadDriverReviews(String driverId) {
        if (!NetworkUtils.isNetworkAvailable(this)) {
            return;
        }

        reviewRepository.getReviewsByUser(driverId, new ReviewRepository.ReviewListCallback() {
            @Override
            public void onSuccess(List<Review> reviews) {
                // Filtrer pour ne garder que les avis provenant de passagers ayant réellement effectué des trajets
                // (c'est-à-dire avec une réservation acceptée)
                if (reviews.isEmpty()) {
                    updateReviewsUI(new ArrayList<>());
                    return;
                }

                List<Review> validReviews = new ArrayList<>();
                final int[] checkedCount = {0};
                final int totalReviews = reviews.size();

                for (Review review : reviews) {
                    // Vérifier que l'avis est associé à un trajet et que le reviewer a une réservation acceptée
                    reservationRepository.checkPassengerHasAcceptedReservation(
                        review.getTripId(), review.getReviewerId(),
                        new ReservationRepository.ReservationCallback() {
                            @Override
                            public void onSuccess(Reservation reservation) {
                                validReviews.add(review);
                                checkedCount[0]++;
                                if (checkedCount[0] == totalReviews) {
                                    updateReviewsUI(validReviews);
                                }
                            }

                            @Override
                            public void onFailure(String error) {
                                // Cet avis n'est pas valide (pas de réservation acceptée)
                                checkedCount[0]++;
                                if (checkedCount[0] == totalReviews) {
                                    updateReviewsUI(validReviews);
                                }
                            }
                        });
                }
            }

            @Override
            public void onFailure(String error) {
                // Ne pas afficher d'erreur si c'est juste qu'il n'y a pas d'avis
                updateReviewsUI(new ArrayList<>());
            }
        });
    }

    private void updateReviewsUI(List<Review> reviews) {
        reviewList.clear();
        reviewList.addAll(reviews);
        reviewAdapter.notifyDataSetChanged();

        // Calculer la note moyenne
        if (reviews.isEmpty()) {
            tvAverageRating.setText("Aucune note");
            tvReviewsCount.setText("0 avis");
            reviewsCard.setVisibility(View.GONE);
        } else {
            double totalRating = 0.0;
            for (Review review : reviews) {
                totalRating += review.getRating();
            }
            double averageRating = totalRating / reviews.size();
            
            tvAverageRating.setText(String.format(Locale.getDefault(), "%.1f", averageRating));
            tvReviewsCount.setText(String.format("%d avis", reviews.size()));
            reviewsCard.setVisibility(View.VISIBLE);
        }
    }

    private void showReservationDialog() {
        if (currentTrip == null) return;
        
        if (currentTrip.getAvailableSeats() <= 0) {
            Toast.makeText(this, "Aucune place disponible", Toast.LENGTH_SHORT).show();
            return;
        }
        
        if (isDriver && currentTrip.getDriverId().equals(currentUserId)) {
            Toast.makeText(this, "Vous ne pouvez pas réserver votre propre trajet", Toast.LENGTH_SHORT).show();
            return;
        }

        new AlertDialog.Builder(this)
                .setTitle("Confirmer la réservation")
                .setMessage(String.format("Voulez-vous réserver ce trajet pour %.2f TND ?", currentTrip.getPrice()))
                .setPositiveButton("Confirmer", (dialog, which) -> createReservation())
                .setNegativeButton("Annuler", null)
                .show();
    }

    private void createReservation() {
        if (currentTrip == null) return;

        String reservationId = UUID.randomUUID().toString();
        String passengerId = currentUserId;
        String driverId = currentTrip.getDriverId();

        Reservation reservation = new Reservation(reservationId, tripId, passengerId, driverId, 1);
        reservationViewModel.createReservation(reservation);
    }

    private void observeTripResult() {
        tripViewModel.getTripResult().observe(this, trip -> {
            if (progressBar != null) {
                progressBar.setVisibility(View.GONE);
            }
            if (trip != null) {
                currentTrip = trip;
                displayTripDetails(trip);
                
                // Masquer le bouton réserver si c'est le conducteur propriétaire du trajet
                if (isDriver && trip.getDriverId().equals(currentUserId)) {
                    btnReserve.setVisibility(View.GONE);
                    // Le conducteur peut voir le chat mais il faudrait récupérer les passagers
                    // Pour l'instant, on laisse le bouton visible mais il affichera un message
                } else {
                    // Pour les passagers, afficher le bouton chat et charger les avis du conducteur
                    btnChat.setVisibility(View.VISIBLE);
                    loadDriverReviews(trip.getDriverId());
                }
            }
        });

        tripViewModel.getErrorMessage().observe(this, error -> {
            if (progressBar != null) {
                progressBar.setVisibility(View.GONE);
            }
            if (error != null && !error.isEmpty()) {
                Toast.makeText(this, error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void observeReservationResult() {
        reservationViewModel.getReservationResult().observe(this, reservation -> {
            if (reservation != null) {
                Toast.makeText(this, "Demande de réservation envoyée avec succès", Toast.LENGTH_SHORT).show();
                finish();
            }
        });

        reservationViewModel.getErrorMessage().observe(this, error -> {
            if (error != null && !error.isEmpty()) {
                Toast.makeText(this, "Erreur: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void displayTripDetails(Trip trip) {
        tvOrigin.setText(trip.getOrigin());
        tvDestination.setText(trip.getDestination());
        
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        tvDate.setText(dateFormat.format(trip.getDate()));
        tvTime.setText(trip.getTime());
        tvPrice.setText(String.format("%.2f TND", trip.getPrice()));
        tvSeats.setText(String.format("%d place(s) disponible(s)", trip.getAvailableSeats()));
        
        // Désactiver le bouton si plus de places
        if (trip.getAvailableSeats() <= 0) {
            btnReserve.setEnabled(false);
            btnReserve.setText("Complet");
        }
    }
}
