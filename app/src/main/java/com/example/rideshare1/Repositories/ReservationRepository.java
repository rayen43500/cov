package com.example.rideshare1.Repositories;

import android.util.Log;

import androidx.annotation.NonNull;

import com.example.rideshare1.Models.Reservation;
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

public class ReservationRepository {
    private FirebaseFirestore firestore;
    private static final String TAG = "ReservationRepository";
    private static final String COLLECTION_RESERVATIONS = "reservations";

    public ReservationRepository() {
        firestore = FirebaseFirestore.getInstance();
    }

    public interface ReservationCallback {
        void onSuccess(Reservation reservation);
        void onFailure(String error);
    }

    public interface ReservationListCallback {
        void onSuccess(List<Reservation> reservations);
        void onFailure(String error);
    }

    public interface UpdateCallback {
        void onSuccess();
        void onFailure(String error);
    }

    public void createReservation(Reservation reservation, ReservationCallback callback) {
        // Vérifier d'abord si une réservation existe déjà pour ce trajet et ce passager
        firestore.collection(COLLECTION_RESERVATIONS)
                .whereEqualTo("tripId", reservation.getTripId())
                .whereEqualTo("passengerId", reservation.getPassengerId())
                .whereEqualTo("status", "pending")
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful() && task.getResult() != null && !task.getResult().isEmpty()) {
                            callback.onFailure("Vous avez déjà une demande de réservation en attente pour ce trajet");
                            return;
                        }

                        // Créer la réservation
                        Map<String, Object> reservationMap = new HashMap<>();
                        reservationMap.put("reservationId", reservation.getReservationId());
                        reservationMap.put("tripId", reservation.getTripId());
                        reservationMap.put("passengerId", reservation.getPassengerId());
                        reservationMap.put("driverId", reservation.getDriverId());
                        reservationMap.put("status", reservation.getStatus());
                        reservationMap.put("numberOfSeats", reservation.getNumberOfSeats());
                        reservationMap.put("createdAt", reservation.getCreatedAt());
                        reservationMap.put("updatedAt", reservation.getUpdatedAt());

                        firestore.collection(COLLECTION_RESERVATIONS).document(reservation.getReservationId())
                                .set(reservationMap)
                                .addOnSuccessListener(new OnSuccessListener<Void>() {
                                    @Override
                                    public void onSuccess(Void aVoid) {
                                        Log.d(TAG, "Reservation created successfully");
                                        callback.onSuccess(reservation);
                                    }
                                })
                                .addOnFailureListener(new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {
                                        Log.e(TAG, "Error creating reservation", e);
                                        callback.onFailure(e.getMessage());
                                    }
                                });
                    }
                });
    }

    public void getReservationById(String reservationId, ReservationCallback callback) {
        firestore.collection(COLLECTION_RESERVATIONS).document(reservationId)
                .get()
                .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                        if (task.isSuccessful()) {
                            DocumentSnapshot document = task.getResult();
                            if (document != null && document.exists()) {
                                Reservation reservation = document.toObject(Reservation.class);
                                if (reservation != null) {
                                    callback.onSuccess(reservation);
                                } else {
                                    callback.onFailure("Failed to parse reservation data");
                                }
                            } else {
                                callback.onFailure("Reservation not found");
                            }
                        } else {
                            callback.onFailure(task.getException() != null ? 
                                    task.getException().getMessage() : "Failed to get reservation");
                        }
                    }
                });
    }

    public void getReservationsByTrip(String tripId, ReservationListCallback callback) {
        firestore.collection(COLLECTION_RESERVATIONS)
                .whereEqualTo("tripId", tripId)
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            List<Reservation> reservations = new ArrayList<>();
                            for (DocumentSnapshot document : task.getResult()) {
                                Reservation reservation = document.toObject(Reservation.class);
                                if (reservation != null) {
                                    reservations.add(reservation);
                                }
                            }
                            callback.onSuccess(reservations);
                        } else {
                            callback.onFailure(task.getException() != null ? 
                                    task.getException().getMessage() : "Failed to get reservations");
                        }
                    }
                });
    }

    public void getAcceptedReservationsByTrip(String tripId, ReservationListCallback callback) {
        firestore.collection(COLLECTION_RESERVATIONS)
                .whereEqualTo("tripId", tripId)
                .whereEqualTo("status", "accepted")
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            List<Reservation> reservations = new ArrayList<>();
                            for (DocumentSnapshot document : task.getResult()) {
                                Reservation reservation = document.toObject(Reservation.class);
                                if (reservation != null) {
                                    reservations.add(reservation);
                                }
                            }
                            callback.onSuccess(reservations);
                        } else {
                            callback.onFailure(task.getException() != null ? 
                                    task.getException().getMessage() : "Failed to get reservations");
                        }
                    }
                });
    }

    public void getReservationsByPassenger(String passengerId, ReservationListCallback callback) {
        firestore.collection(COLLECTION_RESERVATIONS)
                .whereEqualTo("passengerId", passengerId)
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            List<Reservation> reservations = new ArrayList<>();
                            for (DocumentSnapshot document : task.getResult()) {
                                Reservation reservation = document.toObject(Reservation.class);
                                if (reservation != null) {
                                    reservations.add(reservation);
                                }
                            }
                            // Trier par date de création décroissante
                            reservations.sort((r1, r2) -> {
                                if (r1.getCreatedAt() == null || r2.getCreatedAt() == null) {
                                    return 0;
                                }
                                return r2.getCreatedAt().compareTo(r1.getCreatedAt());
                            });
                            callback.onSuccess(reservations);
                        } else {
                            callback.onFailure(task.getException() != null ? 
                                    task.getException().getMessage() : "Failed to get reservations");
                        }
                    }
                });
    }

    public void getAcceptedReservationsByPassenger(String passengerId, ReservationListCallback callback) {
        firestore.collection(COLLECTION_RESERVATIONS)
                .whereEqualTo("passengerId", passengerId)
                .whereEqualTo("status", "accepted")
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            List<Reservation> reservations = new ArrayList<>();
                            for (DocumentSnapshot document : task.getResult()) {
                                Reservation reservation = document.toObject(Reservation.class);
                                if (reservation != null) {
                                    reservations.add(reservation);
                                }
                            }
                            // Trier par date de création décroissante
                            reservations.sort((r1, r2) -> {
                                if (r1.getCreatedAt() == null || r2.getCreatedAt() == null) {
                                    return 0;
                                }
                                return r2.getCreatedAt().compareTo(r1.getCreatedAt());
                            });
                            callback.onSuccess(reservations);
                        } else {
                            callback.onFailure(task.getException() != null ? 
                                    task.getException().getMessage() : "Failed to get reservations");
                        }
                    }
                });
    }

    public void getReservationsByDriver(String driverId, ReservationListCallback callback) {
        firestore.collection(COLLECTION_RESERVATIONS)
                .whereEqualTo("driverId", driverId)
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            List<Reservation> reservations = new ArrayList<>();
                            for (DocumentSnapshot document : task.getResult()) {
                                Reservation reservation = document.toObject(Reservation.class);
                                if (reservation != null) {
                                    reservations.add(reservation);
                                }
                            }
                            // Trier par date de création décroissante
                            reservations.sort((r1, r2) -> {
                                if (r1.getCreatedAt() == null || r2.getCreatedAt() == null) {
                                    return 0;
                                }
                                return r2.getCreatedAt().compareTo(r1.getCreatedAt());
                            });
                            callback.onSuccess(reservations);
                        } else {
                            callback.onFailure(task.getException() != null ? 
                                    task.getException().getMessage() : "Failed to get reservations");
                        }
                    }
                });
    }

    public void getPendingReservationsByDriver(String driverId, ReservationListCallback callback) {
        firestore.collection(COLLECTION_RESERVATIONS)
                .whereEqualTo("driverId", driverId)
                .whereEqualTo("status", "pending")
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            List<Reservation> reservations = new ArrayList<>();
                            for (DocumentSnapshot document : task.getResult()) {
                                Reservation reservation = document.toObject(Reservation.class);
                                if (reservation != null) {
                                    reservations.add(reservation);
                                }
                            }
                            // Trier par date de création décroissante
                            reservations.sort((r1, r2) -> {
                                if (r1.getCreatedAt() == null || r2.getCreatedAt() == null) {
                                    return 0;
                                }
                                return r2.getCreatedAt().compareTo(r1.getCreatedAt());
                            });
                            callback.onSuccess(reservations);
                        } else {
                            callback.onFailure(task.getException() != null ? 
                                    task.getException().getMessage() : "Failed to get reservations");
                        }
                    }
                });
    }

    public void updateReservationStatus(String reservationId, String status, UpdateCallback callback) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("status", status);
        updates.put("updatedAt", new java.util.Date());

        firestore.collection(COLLECTION_RESERVATIONS).document(reservationId)
                .update(updates)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Log.d(TAG, "Reservation status updated successfully");
                        callback.onSuccess();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.e(TAG, "Error updating reservation", e);
                        callback.onFailure(e.getMessage());
                    }
                });
    }

    public void acceptReservation(String reservationId, String tripId,
                                  int numberOfSeats, UpdateCallback callback) {
        // Vérifier d'abord les places disponibles
        firestore.collection("trips").document(tripId)
                .get()
                .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                        if (task.isSuccessful()) {
                            DocumentSnapshot document = task.getResult();
                            if (document != null && document.exists()) {
                                Long availableSeats = document.getLong("availableSeats");
                                if (availableSeats != null && availableSeats >= numberOfSeats) {
                                    // Mettre à jour le statut de la réservation
                                    updateReservationStatus(reservationId, "accepted", callback);
                                    
                                    // Décrémenter les places disponibles
                                    Map<String, Object> tripUpdates = new HashMap<>();
                                    tripUpdates.put("availableSeats", availableSeats - numberOfSeats);
                                    firestore.collection("trips").document(tripId)
                                            .update(tripUpdates);
                                } else {
                                    callback.onFailure("Plus assez de places disponibles");
                                }
                            } else {
                                callback.onFailure("Trajet introuvable");
                            }
                        } else {
                            callback.onFailure("Erreur lors de la vérification des places");
                        }
                    }
                });
    }

    public void rejectReservation(String reservationId, UpdateCallback callback) {
        updateReservationStatus(reservationId, "rejected", callback);
    }

    public void cancelReservation(String reservationId, UpdateCallback callback) {
        updateReservationStatus(reservationId, "cancelled", callback);
    }

    public void checkPassengerHasAcceptedReservation(String tripId, String passengerId, ReservationCallback callback) {
        firestore.collection(COLLECTION_RESERVATIONS)
                .whereEqualTo("tripId", tripId)
                .whereEqualTo("passengerId", passengerId)
                .whereEqualTo("status", "accepted")
                .limit(1)
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            if (task.getResult() != null && !task.getResult().isEmpty()) {
                                Reservation reservation = task.getResult().getDocuments().get(0).toObject(Reservation.class);
                                if (reservation != null) {
                                    callback.onSuccess(reservation);
                                } else {
                                    callback.onFailure("Failed to parse reservation");
                                }
                            } else {
                                callback.onFailure("No accepted reservation found");
                            }
                        } else {
                            callback.onFailure(task.getException() != null ? 
                                    task.getException().getMessage() : "Failed to check reservation");
                        }
                    }
                });
    }
}
