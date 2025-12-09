package com.example.rideshare1.Activities;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.rideshare1.Models.Review;
import com.example.rideshare1.R;
import com.example.rideshare1.Repositories.ReservationRepository;
import com.example.rideshare1.Repositories.ReviewRepository;
import com.example.rideshare1.Repositories.TripRepository;
import com.example.rideshare1.Repositories.UserRepository;
import com.example.rideshare1.Utils.NetworkUtils;
import com.example.rideshare1.Utils.SessionManager;
import com.example.rideshare1.ViewModels.ReviewViewModel;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import java.util.UUID;

public class ReviewActivity extends AppCompatActivity {

    private RatingBar ratingBar;
    private TextInputEditText etComment;
    private MaterialButton btnSubmit;
    private TextView tvUserName, tvTitle;
    private ImageView ivBack;
    private ReviewViewModel reviewViewModel;
    private SessionManager sessionManager;
    private UserRepository userRepository;
    private TripRepository tripRepository;
    private ReservationRepository reservationRepository;
    private ReviewRepository reviewRepository;
    private String reviewedUserId;
    private String tripId;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        if (!com.example.rideshare1.Utils.AuthGuard.requireAuth(this)) {
            finish();
            return;
        }
        
        setContentView(R.layout.activity_review);

        reviewedUserId = getIntent().getStringExtra("reviewedUserId");
        tripId = getIntent().getStringExtra("tripId");
        
        if (reviewedUserId == null || tripId == null) {
            Toast.makeText(this, "Erreur: Informations manquantes", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initializeViews();
        reviewViewModel = new androidx.lifecycle.ViewModelProvider(this).get(ReviewViewModel.class);
        sessionManager = new SessionManager(this);
        userRepository = new UserRepository();
        tripRepository = new TripRepository();
        reservationRepository = new ReservationRepository();
        reviewRepository = new ReviewRepository();

        ivBack.setOnClickListener(v -> finish());
        loadUserInfo();
        
        // Vérifier les règles métier avant de permettre la soumission
        validateReviewConditions();
        
        btnSubmit.setOnClickListener(v -> submitReview());
        observeReviewResult();
    }

    private void initializeViews() {
        ratingBar = findViewById(R.id.ratingBar);
        etComment = findViewById(R.id.etComment);
        btnSubmit = findViewById(R.id.btnSubmit);
        tvUserName = findViewById(R.id.tvUserName);
        tvTitle = findViewById(R.id.tvTitle);
        ivBack = findViewById(R.id.ivBack);
        progressBar = findViewById(R.id.progressBar);
    }

    private void loadUserInfo() {
        if (progressBar != null) {
            progressBar.setVisibility(View.VISIBLE);
        }
        
        userRepository.getUserById(reviewedUserId, new UserRepository.UserCallback() {
            @Override
            public void onSuccess(com.example.rideshare1.Models.User user) {
                if (progressBar != null) {
                    progressBar.setVisibility(View.GONE);
                }
                if (tvUserName != null) {
                    tvUserName.setText(user.getFirstName() + " " + user.getLastName());
                }
                if (tvTitle != null) {
                    tvTitle.setText("Laissez un avis");
                }
            }

            @Override
            public void onFailure(String error) {
                if (progressBar != null) {
                    progressBar.setVisibility(View.GONE);
                }
                if (tvUserName != null) {
                    tvUserName.setText("Utilisateur");
                }
            }
        });
    }

    private void validateReviewConditions() {
        String reviewerId = sessionManager.getUserId();
        
        // Vérifier que le conducteur ne peut pas se noter lui-même
        if (reviewerId.equals(reviewedUserId)) {
            Toast.makeText(this, "Vous ne pouvez pas vous noter vous-même", Toast.LENGTH_LONG).show();
            btnSubmit.setEnabled(false);
            finish();
            return;
        }

        // Vérifier que le trajet est TERMINE
        tripRepository.getTripById(tripId, new TripRepository.TripCallback() {
            @Override
            public void onSuccess(com.example.rideshare1.Models.Trip trip) {
                if (!"completed".equals(trip.getStatus())) {
                    Toast.makeText(ReviewActivity.this, 
                        "Vous ne pouvez laisser un avis que pour un trajet terminé", 
                        Toast.LENGTH_LONG).show();
                    btnSubmit.setEnabled(false);
                    finish();
                    return;
                }

                // Vérifier que le passager a une réservation acceptée pour ce trajet
                reservationRepository.checkPassengerHasAcceptedReservation(
                    tripId, reviewerId, 
                    new ReservationRepository.ReservationCallback() {
                        @Override
                        public void onSuccess(com.example.rideshare1.Models.Reservation reservation) {
                            // Vérifier qu'un avis n'existe pas déjà
                            reviewRepository.getReviewByTrip(tripId, reviewerId, 
                                new ReviewRepository.ReviewCallback() {
                                    @Override
                                    public void onSuccess(Review review) {
                                        // Un avis existe déjà
                                        Toast.makeText(ReviewActivity.this, 
                                            "Vous avez déjà laissé un avis pour ce trajet", 
                                            Toast.LENGTH_LONG).show();
                                        btnSubmit.setEnabled(false);
                                        finish();
                                    }

                                    @Override
                                    public void onFailure(String error) {
                                        // Pas d'avis existant, tout est OK
                                        if (error.contains("not found") || error.contains("Review not found")) {
                                            btnSubmit.setEnabled(true);
                                        } else {
                                            Toast.makeText(ReviewActivity.this, 
                                                "Erreur: " + error, 
                                                Toast.LENGTH_SHORT).show();
                                            btnSubmit.setEnabled(false);
                                        }
                                    }
                                });
                        }

                        @Override
                        public void onFailure(String error) {
                            Toast.makeText(ReviewActivity.this, 
                                "Vous devez avoir une réservation acceptée pour ce trajet pour laisser un avis", 
                                Toast.LENGTH_LONG).show();
                            btnSubmit.setEnabled(false);
                            finish();
                        }
                    });
            }

            @Override
            public void onFailure(String error) {
                Toast.makeText(ReviewActivity.this, 
                    "Erreur: Impossible de charger les informations du trajet", 
                    Toast.LENGTH_SHORT).show();
                btnSubmit.setEnabled(false);
            }
        });
    }

    private void submitReview() {
        if (!NetworkUtils.isNetworkAvailable(this)) {
            Toast.makeText(this, "Pas de connexion Internet", Toast.LENGTH_SHORT).show();
            return;
        }

        // Vérifier à nouveau que le bouton est activé (les validations ont réussi)
        if (!btnSubmit.isEnabled()) {
            Toast.makeText(this, "Vous ne pouvez pas soumettre cet avis", Toast.LENGTH_SHORT).show();
            return;
        }

        float rating = ratingBar.getRating();
        String comment = etComment.getText().toString().trim();

        if (rating == 0) {
            Toast.makeText(this, "Veuillez donner une note", Toast.LENGTH_SHORT).show();
            return;
        }

        // Vérifier une dernière fois qu'un avis n'existe pas déjà (race condition)
        String reviewerId = sessionManager.getUserId();
        reviewRepository.getReviewByTrip(tripId, reviewerId, new ReviewRepository.ReviewCallback() {
            @Override
            public void onSuccess(Review existingReview) {
                // Un avis existe déjà
                Toast.makeText(ReviewActivity.this, 
                    "Un avis existe déjà pour ce trajet", 
                    Toast.LENGTH_LONG).show();
                btnSubmit.setEnabled(false);
            }

            @Override
            public void onFailure(String error) {
                // Pas d'avis existant, on peut créer l'avis
                if (error.contains("not found") || error.contains("Review not found")) {
                    String reviewId = UUID.randomUUID().toString();
                    Review review = new Review(reviewId, tripId, reviewerId, reviewedUserId, rating, comment);
                    
                    if (progressBar != null) {
                        progressBar.setVisibility(View.VISIBLE);
                    }
                    btnSubmit.setEnabled(false);
                    
                    reviewViewModel.createReview(review);
                } else {
                    Toast.makeText(ReviewActivity.this, 
                        "Erreur: " + error, 
                        Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void observeReviewResult() {
        reviewViewModel.getReviewResult().observe(this, review -> {
            if (progressBar != null) {
                progressBar.setVisibility(View.GONE);
            }
            btnSubmit.setEnabled(true);
            
            if (review != null) {
                Toast.makeText(this, "Avis soumis avec succès", Toast.LENGTH_SHORT).show();
                finish();
            }
        });

        reviewViewModel.getErrorMessage().observe(this, error -> {
            if (progressBar != null) {
                progressBar.setVisibility(View.GONE);
            }
            btnSubmit.setEnabled(true);
            
            if (error != null && !error.isEmpty()) {
                Toast.makeText(this, "Erreur: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }
}
