package com.example.rideshare1;

import android.app.Application;
import android.util.Log;

import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreSettings;
import com.google.firebase.messaging.FirebaseMessaging;

/**
 * Classe Application personnalisée
 * Initialise Firebase et configure les paramètres de l'application
 */
public class RideShareApplication extends Application {

    private static final String TAG = "RideShareApplication";

    @Override
    public void onCreate() {
        super.onCreate();
        
        // Initialiser Firebase
        initializeFirebase();
        
        // Configurer Firestore pour le mode développement (optionnel)
        configureFirestore();
        
        // Initialiser FCM (optionnel, se fait automatiquement)
        initializeFCM();
    }

    /**
     * Initialise Firebase
     * Firebase s'initialise automatiquement via google-services.json
     * Cette méthode vérifie que l'initialisation s'est bien passée
     */
    private void initializeFirebase() {
        try {
            // Vérifier que Firebase est initialisé
            if (FirebaseApp.getApps(this).isEmpty()) {
                Log.w(TAG, "Firebase n'est pas initialisé. Vérifiez google-services.json");
            } else {
                Log.d(TAG, "Firebase initialisé avec succès");
            }
            
            // Vérifier Firebase Auth
            FirebaseAuth auth = FirebaseAuth.getInstance();
            if (auth != null) {
                Log.d(TAG, "Firebase Auth initialisé");
            }
        } catch (Exception e) {
            Log.e(TAG, "Erreur lors de l'initialisation de Firebase", e);
        }
    }

    /**
     * Configure Firestore avec des paramètres optimisés
     */
    private void configureFirestore() {
        try {
            FirebaseFirestore firestore = FirebaseFirestore.getInstance();
            FirebaseFirestoreSettings settings = new FirebaseFirestoreSettings.Builder()
                    .setPersistenceEnabled(true) // Activer la persistance locale
                    .build();
            firestore.setFirestoreSettings(settings);
            Log.d(TAG, "Firestore configuré avec succès");
        } catch (Exception e) {
            Log.e(TAG, "Erreur lors de la configuration de Firestore", e);
        }
    }

    /**
     * Initialise Firebase Cloud Messaging
     * Note: L'erreur 400 sur /auth/devicekey est normale sur les émulateurs
     * et n'empêche pas l'application de fonctionner
     */
    private void initializeFCM() {
        try {
            FirebaseMessaging.getInstance().getToken()
                    .addOnCompleteListener(task -> {
                        if (!task.isSuccessful()) {
                            Log.w(TAG, "Impossible d'obtenir le token FCM. " +
                                    "C'est normal sur les émulateurs sans Google Play Services.", 
                                    task.getException());
                            return;
                        }

                        // Récupérer le token FCM
                        String token = task.getResult();
                        Log.d(TAG, "Token FCM obtenu avec succès: " + token.substring(0, 20) + "...");
                    });
        } catch (Exception e) {
            // L'erreur 400 sur /auth/devicekey est normale sur les émulateurs
            // Elle n'empêche pas l'application de fonctionner
            Log.w(TAG, "Erreur lors de l'initialisation FCM (normale sur émulateur): " + e.getMessage());
        }
    }
}

