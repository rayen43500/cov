package com.example.rideshare1.Repositories;

import android.util.Log;

import androidx.annotation.NonNull;

import com.example.rideshare1.Models.Driver;
import com.example.rideshare1.Models.User;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class AuthRepository {
    private FirebaseAuth firebaseAuth;
    private FirebaseFirestore firestore;
    private static final String TAG = "AuthRepository";

    public AuthRepository() {
        firebaseAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();
    }

    public interface AuthCallback {
        void onSuccess(String userId);
        void onFailure(String error);
    }
    
    public interface LoginCallback {
        void onSuccess(String userId, String userType);
        void onFailure(String error);
    }

    public void registerUser(String email, String password, String firstName, String lastName, 
                            String phoneNumber, String userType, AuthCallback callback) {
        firebaseAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            FirebaseUser firebaseUser = firebaseAuth.getCurrentUser();
                            if (firebaseUser != null) {
                                String userId = firebaseUser.getUid();
                                User user;
                                
                                if ("driver".equals(userType)) {
                                    user = new Driver(userId, firstName, lastName, email, phoneNumber);
                                } else {
                                    user = new User(userId, firstName, lastName, email, phoneNumber, userType);
                                }
                                
                                saveUserToFirestore(user, callback);
                            }
                        } else {
                            callback.onFailure(task.getException() != null ? 
                                    task.getException().getMessage() : "Registration failed");
                        }
                    }
                });
    }

    public void registerDriver(String email, String password, String firstName, String lastName,
                              String phoneNumber, String licenseNumber, String vehiclePlate,
                              String vehicleModel, AuthCallback callback) {
        registerUser(email, password, firstName, lastName, phoneNumber, "driver", new AuthCallback() {
            @Override
            public void onSuccess(String userId) {
                // Update driver with additional info
                Map<String, Object> driverData = new HashMap<>();
                driverData.put("licenseNumber", licenseNumber);
                driverData.put("vehiclePlate", vehiclePlate);
                driverData.put("vehicleModel", vehicleModel);
                
                firestore.collection("users").document(userId)
                        .update(driverData)
                        .addOnSuccessListener(new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void aVoid) {
                                callback.onSuccess(userId);
                            }
                        })
                        .addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                callback.onFailure(e.getMessage());
                            }
                        });
            }

            @Override
            public void onFailure(String error) {
                callback.onFailure(error);
            }
        });
    }

    private void saveUserToFirestore(User user, AuthCallback callback) {
        Map<String, Object> userMap = new HashMap<>();
        userMap.put("userId", user.getUserId());
        userMap.put("firstName", user.getFirstName());
        userMap.put("lastName", user.getLastName());
        userMap.put("email", user.getEmail());
        userMap.put("phoneNumber", user.getPhoneNumber());
        userMap.put("userType", user.getUserType());
        userMap.put("rating", user.getRating());
        userMap.put("totalRatings", user.getTotalRatings());

        firestore.collection("users").document(user.getUserId())
                .set(userMap)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Log.d(TAG, "User saved to Firestore");
                        callback.onSuccess(user.getUserId());
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.e(TAG, "Error saving user", e);
                        callback.onFailure(e.getMessage());
                    }
                });
    }

    public void loginUser(String email, String password, AuthCallback callback) {
        firebaseAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            FirebaseUser firebaseUser = firebaseAuth.getCurrentUser();
                            if (firebaseUser != null) {
                                callback.onSuccess(firebaseUser.getUid());
                            } else {
                                callback.onFailure("User not found");
                            }
                        } else {
                            callback.onFailure(task.getException() != null ? 
                                    task.getException().getMessage() : "Login failed");
                        }
                    }
                });
    }
    
    public void loginUserWithType(String email, String password, LoginCallback callback) {
        firebaseAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            FirebaseUser firebaseUser = firebaseAuth.getCurrentUser();
                            if (firebaseUser != null) {
                                String userId = firebaseUser.getUid();
                                // Get user type from Firestore
                                firestore.collection("users").document(userId)
                                        .get()
                                        .addOnCompleteListener(new OnCompleteListener<com.google.firebase.firestore.DocumentSnapshot>() {
                                            @Override
                                            public void onComplete(@NonNull Task<com.google.firebase.firestore.DocumentSnapshot> task) {
                                                if (task.isSuccessful() && task.getResult() != null) {
                                                    com.google.firebase.firestore.DocumentSnapshot document = task.getResult();
                                                    if (document.exists()) {
                                                        String userType = document.getString("userType");
                                                        if (userType != null) {
                                                            callback.onSuccess(userId, userType);
                                                        } else {
                                                            callback.onFailure("User type not found");
                                                        }
                                                    } else {
                                                        callback.onFailure("User document not found");
                                                    }
                                                } else {
                                                    callback.onFailure("Failed to get user data");
                                                }
                                            }
                                        });
                            } else {
                                callback.onFailure("User not found");
                            }
                        } else {
                            callback.onFailure(task.getException() != null ? 
                                    task.getException().getMessage() : "Login failed");
                        }
                    }
                });
    }

    public void logout() {
        firebaseAuth.signOut();
    }

    public FirebaseUser getCurrentUser() {
        return firebaseAuth.getCurrentUser();
    }

    public void resetPassword(String email, AuthCallback callback) {
        firebaseAuth.sendPasswordResetEmail(email)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            callback.onSuccess("Password reset email sent");
                        } else {
                            callback.onFailure(task.getException() != null ? 
                                    task.getException().getMessage() : "Failed to send reset email");
                        }
                    }
                });
    }
}

