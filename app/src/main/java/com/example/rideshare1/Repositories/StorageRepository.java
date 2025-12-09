package com.example.rideshare1.Repositories;

import android.net.Uri;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.util.UUID;

public class StorageRepository {
    private FirebaseStorage storage;
    private static final String TAG = "StorageRepository";
    private static final String PROFILE_PHOTOS = "profile_photos";
    private static final String VEHICLE_PHOTOS = "vehicle_photos";

    public StorageRepository() {
        storage = FirebaseStorage.getInstance();
    }

    public interface UploadCallback {
        void onSuccess(String downloadUrl);
        void onFailure(String error);
    }

    public void uploadProfilePhoto(String userId, Uri imageUri, UploadCallback callback) {
        uploadImage(userId, imageUri, PROFILE_PHOTOS, callback);
    }

    public void uploadVehiclePhoto(String userId, Uri imageUri, UploadCallback callback) {
        uploadImage(userId, imageUri, VEHICLE_PHOTOS, callback);
    }

    private void uploadImage(String userId, Uri imageUri, String folder, UploadCallback callback) {
        if (imageUri == null) {
            callback.onFailure("Image URI is null");
            return;
        }

        String fileName = userId + "_" + UUID.randomUUID().toString() + ".jpg";
        StorageReference storageRef = storage.getReference().child(folder).child(fileName);

        storageRef.putFile(imageUri)
                .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                        storageRef.getDownloadUrl()
                                .addOnSuccessListener(new OnSuccessListener<Uri>() {
                                    @Override
                                    public void onSuccess(Uri downloadUri) {
                                        Log.d(TAG, "Image uploaded successfully: " + downloadUri.toString());
                                        callback.onSuccess(downloadUri.toString());
                                    }
                                })
                                .addOnFailureListener(new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {
                                        Log.e(TAG, "Error getting download URL", e);
                                        callback.onFailure(e.getMessage());
                                    }
                                });
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.e(TAG, "Error uploading image", e);
                        callback.onFailure(e.getMessage());
                    }
                });
    }

    public void deleteImage(String imageUrl, UploadCallback callback) {
        if (imageUrl == null || imageUrl.isEmpty()) {
            callback.onSuccess("No image to delete");
            return;
        }

        StorageReference storageRef = storage.getReferenceFromUrl(imageUrl);
        storageRef.delete()
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Log.d(TAG, "Image deleted successfully");
                        callback.onSuccess("Image deleted");
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.e(TAG, "Error deleting image", e);
                        callback.onFailure(e.getMessage());
                    }
                });
    }
}

