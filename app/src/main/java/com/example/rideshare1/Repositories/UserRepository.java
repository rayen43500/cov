package com.example.rideshare1.Repositories;

import android.util.Log;

import androidx.annotation.NonNull;

import com.example.rideshare1.Models.User;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class UserRepository {
    private FirebaseFirestore firestore;
    private static final String TAG = "UserRepository";
    private static final String COLLECTION_USERS = "users";

    public UserRepository() {
        firestore = FirebaseFirestore.getInstance();
    }

    public interface UserCallback {
        void onSuccess(User user);
        void onFailure(String error);
    }

    public interface UpdateCallback {
        void onSuccess();
        void onFailure(String error);
    }

    public void getUserById(String userId, UserCallback callback) {
        firestore.collection(COLLECTION_USERS).document(userId)
                .get()
                .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                        if (task.isSuccessful()) {
                            DocumentSnapshot document = task.getResult();
                            if (document != null && document.exists()) {
                                User user = document.toObject(User.class);
                                if (user != null) {
                                    callback.onSuccess(user);
                                } else {
                                    callback.onFailure("Failed to parse user data");
                                }
                            } else {
                                callback.onFailure("User not found");
                            }
                        } else {
                            callback.onFailure(task.getException() != null ? 
                                    task.getException().getMessage() : "Failed to get user");
                        }
                    }
                });
    }

    public void updateUser(String userId, Map<String, Object> updates, UpdateCallback callback) {
        firestore.collection(COLLECTION_USERS).document(userId)
                .update(updates)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Log.d(TAG, "User updated successfully");
                        callback.onSuccess();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.e(TAG, "Error updating user", e);
                        callback.onFailure(e.getMessage());
                    }
                });
    }

    public void updateProfilePhoto(String userId, String photoUrl, UpdateCallback callback) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("profilePhotoUrl", photoUrl);
        updateUser(userId, updates, callback);
    }

    public void deleteUser(String userId, UpdateCallback callback) {
        firestore.collection(COLLECTION_USERS).document(userId)
                .delete()
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Log.d(TAG, "User deleted successfully");
                        callback.onSuccess();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.e(TAG, "Error deleting user", e);
                        callback.onFailure(e.getMessage());
                    }
                });
    }
}

