package com.example.rideshare1.Repositories;

import android.util.Log;

import androidx.annotation.NonNull;

import com.example.rideshare1.Models.Trip;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class TripRepository {
    private FirebaseFirestore firestore;
    private static final String TAG = "TripRepository";
    private static final String COLLECTION_TRIPS = "trips";

    public TripRepository() {
        firestore = FirebaseFirestore.getInstance();
    }

    public interface TripCallback {
        void onSuccess(Trip trip);
        void onFailure(String error);
    }

    public interface TripListCallback {
        void onSuccess(List<Trip> trips);
        void onFailure(String error);
    }

    public interface UpdateCallback {
        void onSuccess();
        void onFailure(String error);
    }

    public void createTrip(Trip trip, TripCallback callback) {
        Map<String, Object> tripMap = trip.toMap();
        
        firestore.collection(COLLECTION_TRIPS).document(trip.getTripId())
                .set(tripMap)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Log.d(TAG, "Trip created successfully");
                        callback.onSuccess(trip);
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.e(TAG, "Error creating trip", e);
                        callback.onFailure(e.getMessage());
                    }
                });
    }

    public void getTripById(String tripId, TripCallback callback) {
        firestore.collection(COLLECTION_TRIPS).document(tripId)
                .get()
                .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                        if (task.isSuccessful()) {
                            DocumentSnapshot document = task.getResult();
                            if (document != null && document.exists()) {
                                Trip trip = document.toObject(Trip.class);
                                if (trip != null) {
                                    // Ne pas modifier automatiquement le statut si c'est "in_progress" ou "completed"
                                    // Le conducteur doit démarrer et terminer manuellement le trajet
                                    // On ne marque automatiquement comme terminé que les trajets "active" dont la date est passée
                                    Date now = new Date();
                                    if (trip.getDate() != null && trip.getDate().before(now) && 
                                        "active".equals(trip.getStatus()) && 
                                        !"cancelled".equals(trip.getStatus())) {
                                        trip.setStatus("completed");
                                        // Mettre à jour dans Firestore
                                        Map<String, Object> updates = new HashMap<>();
                                        updates.put("status", "completed");
                                        firestore.collection(COLLECTION_TRIPS).document(tripId)
                                                .update(updates)
                                                .addOnSuccessListener(aVoid -> Log.d(TAG, "Trip marked as completed"))
                                                .addOnFailureListener(e -> Log.e(TAG, "Error updating trip status", e));
                                    }
                                    callback.onSuccess(trip);
                                } else {
                                    callback.onFailure("Failed to parse trip data");
                                }
                            } else {
                                callback.onFailure("Trip not found");
                            }
                        } else {
                            callback.onFailure(task.getException() != null ? 
                                    task.getException().getMessage() : "Failed to get trip");
                        }
                    }
                });
    }

    public void searchTrips(String origin, String destination, Date date, TripListCallback callback) {
        Log.d(TAG, "Searching trips - Origin: " + origin + ", Destination: " + destination + ", Date: " + date);
        
        // Récupérer tous les trajets actifs seulement pour la recherche
        // On filtrera manuellement par places disponibles et autres critères pour éviter les problèmes d'index
        firestore.collection(COLLECTION_TRIPS)
                .whereEqualTo("status", "active")
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            List<Trip> allTrips = new ArrayList<>();
                            QuerySnapshot querySnapshot = task.getResult();
                            Log.d(TAG, "Found " + querySnapshot.size() + " active trips");
                            
                            Date now = new Date();
                            for (DocumentSnapshot document : querySnapshot) {
                                Trip trip = document.toObject(Trip.class);
                                if (trip != null) {
                                    // Ne pas modifier automatiquement le statut si c'est "in_progress" ou "completed"
                                    // Le conducteur doit démarrer et terminer manuellement le trajet
                                    // On ne marque automatiquement comme terminé que les trajets "active" dont la date est passée
                                    if (trip.getDate() != null && trip.getDate().before(now) && 
                                        "active".equals(trip.getStatus()) && 
                                        !"cancelled".equals(trip.getStatus())) {
                                        trip.setStatus("completed");
                                        // Mettre à jour dans Firestore en arrière-plan
                                        Map<String, Object> updates = new HashMap<>();
                                        updates.put("status", "completed");
                                        firestore.collection(COLLECTION_TRIPS).document(trip.getTripId())
                                                .update(updates)
                                                .addOnSuccessListener(aVoid -> Log.d(TAG, "Trip " + trip.getTripId() + " marked as completed"))
                                                .addOnFailureListener(e -> Log.e(TAG, "Error updating trip status", e));
                                    }
                                    allTrips.add(trip);
                                }
                            }
                            
                            // Filtrer manuellement par origine, destination et date
                            List<Trip> filteredTrips = new ArrayList<>();
                            
                            for (Trip trip : allTrips) {
                                boolean matches = true;
                                
                                // Vérifier si le trajet est terminé (date passée)
                                boolean isCompleted = trip.getDate() != null && trip.getDate().before(now);
                                
                                // Filtrer par places disponibles (au moins 1) pour les trajets actifs
                                // Les trajets terminés peuvent être inclus même sans places
                                if (!isCompleted && trip.getAvailableSeats() < 1) {
                                    matches = false;
                                }
                                
                                // Filtrer par origine (insensible à la casse)
                                if (origin != null && !origin.trim().isEmpty() && matches) {
                                    String tripOrigin = trip.getOrigin() != null ? trip.getOrigin().toLowerCase().trim() : "";
                                    String searchOrigin = origin.toLowerCase().trim();
                                    if (!tripOrigin.contains(searchOrigin) && !searchOrigin.contains(tripOrigin)) {
                                        matches = false;
                                    }
                                }
                                
                                // Filtrer par destination (insensible à la casse)
                                if (destination != null && !destination.trim().isEmpty() && matches) {
                                    String tripDestination = trip.getDestination() != null ? trip.getDestination().toLowerCase().trim() : "";
                                    String searchDestination = destination.toLowerCase().trim();
                                    if (!tripDestination.contains(searchDestination) && !searchDestination.contains(tripDestination)) {
                                        matches = false;
                                    }
                                }
                                
                                // Filtrer par date (même jour)
                                if (date != null && matches) {
                                    if (trip.getDate() != null) {
                                        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                                        String tripDateStr = dateFormat.format(trip.getDate());
                                        String searchDateStr = dateFormat.format(date);
                                        if (!tripDateStr.equals(searchDateStr)) {
                                            matches = false;
                                        }
                                    } else {
                                        matches = false;
                                    }
                                }
                                
                                if (matches) {
                                    filteredTrips.add(trip);
                                }
                            }
                            
                            // Trier par date croissante (plus proches en premier)
                            filteredTrips.sort((t1, t2) -> {
                                if (t1.getDate() == null || t2.getDate() == null) {
                                    return 0;
                                }
                                return t1.getDate().compareTo(t2.getDate());
                            });
                            
                            Log.d(TAG, "Returning " + filteredTrips.size() + " filtered trips");
                            callback.onSuccess(filteredTrips);
                        } else {
                            Exception exception = task.getException();
                            String errorMsg = exception != null ? exception.getMessage() : "Failed to search trips";
                            Log.e(TAG, "Error searching trips: " + errorMsg, exception);
                            callback.onFailure(errorMsg);
                        }
                    }
                });
    }

    public void getAllActiveTrips(TripListCallback callback) {
        Log.d(TAG, "Getting all active trips");
        
        // Utiliser seulement whereEqualTo pour éviter les problèmes d'index
        // Filtrer manuellement par places disponibles
        firestore.collection(COLLECTION_TRIPS)
                .whereEqualTo("status", "active")
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            List<Trip> trips = new ArrayList<>();
                            QuerySnapshot querySnapshot = task.getResult();
                            Log.d(TAG, "Found " + querySnapshot.size() + " active trips");
                            
                            Date now = new Date();
                            for (DocumentSnapshot document : querySnapshot) {
                                Trip trip = document.toObject(Trip.class);
                                // Filtrer manuellement : places disponibles >= 1 et date non passée
                                if (trip != null && 
                                    trip.getAvailableSeats() >= 1 && 
                                    trip.getDate() != null && 
                                    !trip.getDate().before(now)) {
                                    trips.add(trip);
                                }
                            }
                            
                            // Trier par date croissante
                            trips.sort((t1, t2) -> {
                                if (t1.getDate() == null || t2.getDate() == null) {
                                    return 0;
                                }
                                return t1.getDate().compareTo(t2.getDate());
                            });
                            
                            Log.d(TAG, "Returning " + trips.size() + " trips");
                            callback.onSuccess(trips);
                        } else {
                            Exception exception = task.getException();
                            String errorMsg = exception != null ? exception.getMessage() : "Failed to get trips";
                            Log.e(TAG, "Error getting trips: " + errorMsg, exception);
                            callback.onFailure(errorMsg);
                        }
                    }
                });
    }

    public void getAllTrips(TripListCallback callback) {
        Log.d(TAG, "Getting all trips (active and completed)");
        
        // Récupérer tous les trajets (actifs et terminés)
        firestore.collection(COLLECTION_TRIPS)
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            List<Trip> trips = new ArrayList<>();
                            QuerySnapshot querySnapshot = task.getResult();
                            Log.d(TAG, "Found " + querySnapshot.size() + " trips total");
                            
                            Date now = new Date();
                            for (DocumentSnapshot document : querySnapshot) {
                                Trip trip = document.toObject(Trip.class);
                                if (trip != null) {
                                    // Ne pas modifier automatiquement le statut si c'est "in_progress" ou "completed"
                                    // Le conducteur doit démarrer et terminer manuellement le trajet
                                    // On ne marque automatiquement comme terminé que les trajets "active" dont la date est passée
                                    if (trip.getDate() != null && trip.getDate().before(now) && 
                                        "active".equals(trip.getStatus()) && 
                                        !"cancelled".equals(trip.getStatus())) {
                                        trip.setStatus("completed");
                                        // Mettre à jour dans Firestore en arrière-plan
                                        Map<String, Object> updates = new HashMap<>();
                                        updates.put("status", "completed");
                                        firestore.collection(COLLECTION_TRIPS).document(trip.getTripId())
                                                .update(updates)
                                                .addOnSuccessListener(aVoid -> Log.d(TAG, "Trip " + trip.getTripId() + " marked as completed"))
                                                .addOnFailureListener(e -> Log.e(TAG, "Error updating trip status", e));
                                    }
                                    
                                    // Inclure tous les trajets (actifs et terminés) avec au moins 1 place disponible
                                    // ou les trajets terminés même sans places (pour l'historique)
                                    if (trip.getAvailableSeats() >= 1 || 
                                        (trip.getDate() != null && trip.getDate().before(now))) {
                                        trips.add(trip);
                                    }
                                }
                            }
                            
                            // Trier par date croissante
                            trips.sort((t1, t2) -> {
                                if (t1.getDate() == null || t2.getDate() == null) {
                                    return 0;
                                }
                                return t1.getDate().compareTo(t2.getDate());
                            });
                            
                            Log.d(TAG, "Returning " + trips.size() + " trips");
                            callback.onSuccess(trips);
                        } else {
                            Exception exception = task.getException();
                            String errorMsg = exception != null ? exception.getMessage() : "Failed to get trips";
                            Log.e(TAG, "Error getting trips: " + errorMsg, exception);
                            callback.onFailure(errorMsg);
                        }
                    }
                });
    }

    public void getTripsByDriver(String driverId, TripListCallback callback) {
        Log.d(TAG, "Getting trips for driver: " + driverId);
        
        // Utiliser une requête simple sans orderBy pour éviter les problèmes d'index
        // On triera manuellement après
        firestore.collection(COLLECTION_TRIPS)
                .whereEqualTo("driverId", driverId)
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            List<Trip> trips = new ArrayList<>();
                            QuerySnapshot querySnapshot = task.getResult();
                            Log.d(TAG, "Found " + querySnapshot.size() + " trips");
                            
                            Date now = new Date();
                            for (DocumentSnapshot document : querySnapshot) {
                                Trip trip = document.toObject(Trip.class);
                                if (trip != null) {
                                    // Ne pas modifier automatiquement le statut si c'est "in_progress" ou "completed"
                                    // Le conducteur doit démarrer et terminer manuellement le trajet
                                    // On ne marque automatiquement comme terminé que les trajets "active" dont la date est passée
                                    if (trip.getDate() != null && trip.getDate().before(now) && 
                                        "active".equals(trip.getStatus()) && 
                                        !"cancelled".equals(trip.getStatus())) {
                                        trip.setStatus("completed");
                                        // Mettre à jour dans Firestore en arrière-plan
                                        Map<String, Object> updates = new HashMap<>();
                                        updates.put("status", "completed");
                                        firestore.collection(COLLECTION_TRIPS).document(trip.getTripId())
                                                .update(updates)
                                                .addOnSuccessListener(aVoid -> Log.d(TAG, "Trip " + trip.getTripId() + " marked as completed"))
                                                .addOnFailureListener(e -> Log.e(TAG, "Error updating trip status", e));
                                    }
                                    
                                    Log.d(TAG, "Trip found: " + trip.getTripId() + " - " + trip.getOrigin() + " to " + trip.getDestination());
                                    trips.add(trip);
                                } else {
                                    Log.w(TAG, "Failed to parse trip from document: " + document.getId());
                                }
                            }
                            
                            // Trier par date décroissante manuellement
                            trips.sort((t1, t2) -> {
                                if (t1.getDate() == null || t2.getDate() == null) {
                                    return 0;
                                }
                                return t2.getDate().compareTo(t1.getDate());
                            });
                            
                            Log.d(TAG, "Returning " + trips.size() + " trips");
                            callback.onSuccess(trips);
                        } else {
                            Exception exception = task.getException();
                            String errorMsg = exception != null ? exception.getMessage() : "Failed to get trips";
                            Log.e(TAG, "Error getting trips: " + errorMsg, exception);
                            callback.onFailure(errorMsg);
                        }
                    }
                });
    }

    public void updateTrip(String tripId, Map<String, Object> updates, UpdateCallback callback) {
        firestore.collection(COLLECTION_TRIPS).document(tripId)
                .update(updates)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Log.d(TAG, "Trip updated successfully");
                        callback.onSuccess();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.e(TAG, "Error updating trip", e);
                        callback.onFailure(e.getMessage());
                    }
                });
    }

    public void deleteTrip(String tripId, UpdateCallback callback) {
        firestore.collection(COLLECTION_TRIPS).document(tripId)
                .delete()
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Log.d(TAG, "Trip deleted successfully");
                        callback.onSuccess();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.e(TAG, "Error deleting trip", e);
                        callback.onFailure(e.getMessage());
                    }
                });
    }
}
