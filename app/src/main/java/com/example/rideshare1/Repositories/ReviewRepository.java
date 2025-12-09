package com.example.rideshare1.Repositories;

import android.util.Log;

import androidx.annotation.NonNull;

import com.example.rideshare1.Models.Review;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ReviewRepository {
    private FirebaseFirestore firestore;
    private static final String TAG = "ReviewRepository";
    private static final String COLLECTION_REVIEWS = "reviews";

    public ReviewRepository() {
        firestore = FirebaseFirestore.getInstance();
    }

    public interface ReviewCallback {
        void onSuccess(Review review);
        void onFailure(String error);
    }

    public interface ReviewListCallback {
        void onSuccess(List<Review> reviews);
        void onFailure(String error);
    }

    public void createReview(Review review, ReviewCallback callback) {
        Map<String, Object> reviewMap = new HashMap<>();
        reviewMap.put("reviewId", review.getReviewId());
        reviewMap.put("tripId", review.getTripId());
        reviewMap.put("reviewerId", review.getReviewerId());
        reviewMap.put("reviewedId", review.getReviewedId());
        reviewMap.put("rating", review.getRating());
        reviewMap.put("comment", review.getComment());
        reviewMap.put("createdAt", review.getCreatedAt());

        firestore.collection(COLLECTION_REVIEWS).document(review.getReviewId())
                .set(reviewMap)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Log.d(TAG, "Review created successfully");
                        updateUserRating(review.getReviewedId());
                        callback.onSuccess(review);
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.e(TAG, "Error creating review", e);
                        callback.onFailure(e.getMessage());
                    }
                });
    }

    public void getReviewsByUser(String userId, ReviewListCallback callback) {
        // Retirer orderBy pour éviter les problèmes d'index - trier manuellement après
        firestore.collection(COLLECTION_REVIEWS)
                .whereEqualTo("reviewedId", userId)
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            List<Review> reviews = new ArrayList<>();
                            for (DocumentSnapshot document : task.getResult()) {
                                Review review = document.toObject(Review.class);
                                if (review != null) {
                                    reviews.add(review);
                                }
                            }
                            // Trier par date décroissante manuellement
                            reviews.sort((r1, r2) -> {
                                if (r1.getCreatedAt() == null || r2.getCreatedAt() == null) {
                                    return 0;
                                }
                                return r2.getCreatedAt().compareTo(r1.getCreatedAt());
                            });
                            callback.onSuccess(reviews);
                        } else {
                            callback.onFailure(task.getException() != null ? 
                                    task.getException().getMessage() : "Failed to get reviews");
                        }
                    }
                });
    }

    public void getReviewByTrip(String tripId, String reviewerId, ReviewCallback callback) {
        firestore.collection(COLLECTION_REVIEWS)
                .whereEqualTo("tripId", tripId)
                .whereEqualTo("reviewerId", reviewerId)
                .limit(1)
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            if (task.getResult() != null && !task.getResult().isEmpty()) {
                                Review review = task.getResult().getDocuments().get(0).toObject(Review.class);
                                if (review != null) {
                                    callback.onSuccess(review);
                                } else {
                                    callback.onFailure("Failed to parse review data");
                                }
                            } else {
                                callback.onFailure("Review not found");
                            }
                        } else {
                            callback.onFailure(task.getException() != null ? 
                                    task.getException().getMessage() : "Failed to get review");
                        }
                    }
                });
    }

    private void updateUserRating(String userId) {
        // Note: Cette méthode met à jour la note moyenne de l'utilisateur
        // Elle inclut tous les avis, mais les avis invalides seront filtrés lors de l'affichage
        // dans TripDetailsActivity.loadDriverReviews() qui vérifie les réservations acceptées
        firestore.collection(COLLECTION_REVIEWS)
                .whereEqualTo("reviewedId", userId)
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            double totalRating = 0.0;
                            int count = 0;
                            
                            for (DocumentSnapshot document : task.getResult()) {
                                Review review = document.toObject(Review.class);
                                if (review != null) {
                                    totalRating += review.getRating();
                                    count++;
                                }
                            }
                            
                            if (count > 0) {
                                final double averageRating = totalRating / count;
                                final int finalCount = count;
                                Map<String, Object> updates = new HashMap<>();
                                updates.put("rating", averageRating);
                                updates.put("totalRatings", finalCount);
                                
                                firestore.collection("users").document(userId)
                                        .update(updates)
                                        .addOnSuccessListener(new OnSuccessListener<Void>() {
                                            @Override
                                            public void onSuccess(Void aVoid) {
                                                Log.d(TAG, "User rating updated: " + averageRating + " (" + finalCount + " reviews)");
                                            }
                                        })
                                        .addOnFailureListener(new OnFailureListener() {
                                            @Override
                                            public void onFailure(@NonNull Exception e) {
                                                Log.e(TAG, "Error updating user rating", e);
                                            }
                                        });
                            } else {
                                // Si aucun avis, mettre la note à 0
                                Map<String, Object> updates = new HashMap<>();
                                updates.put("rating", 0.0);
                                updates.put("totalRatings", 0);
                                
                                firestore.collection("users").document(userId)
                                        .update(updates)
                                        .addOnSuccessListener(new OnSuccessListener<Void>() {
                                            @Override
                                            public void onSuccess(Void aVoid) {
                                                Log.d(TAG, "User rating reset to 0");
                                            }
                                        });
                            }
                        } else {
                            Log.e(TAG, "Error getting reviews for rating update", task.getException());
                        }
                    }
                });
    }
}
