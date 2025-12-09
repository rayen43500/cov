package com.example.rideshare1.Utils;

import android.content.Context;
import android.content.Intent;

import com.example.rideshare1.Activities.DriverMainActivity;
import com.example.rideshare1.Activities.LoginActivity;
import com.example.rideshare1.Activities.PassengerMainActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class AuthGuard {
    
    /**
     * Vérifie si l'utilisateur est authentifié
     */
    public static boolean isAuthenticated() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        return user != null;
    }
    
    /**
     * Vérifie si l'utilisateur a le bon rôle pour accéder à une activité
     * @param context Le contexte
     * @param requiredRole Le rôle requis ("driver" ou "passenger")
     * @return true si l'utilisateur a le bon rôle, false sinon
     */
    public static boolean checkRole(Context context, String requiredRole) {
        SessionManager sessionManager = new SessionManager(context);
        
        if (!sessionManager.isLoggedIn()) {
            redirectToLogin(context);
            return false;
        }
        
        String userType = sessionManager.getUserType();
        if (userType == null || !userType.equals(requiredRole)) {
            // Rediriger vers la bonne activité selon le rôle
            redirectToCorrectActivity(context, userType);
            return false;
        }
        
        return true;
    }
    
    /**
     * Redirige vers l'activité de connexion
     */
    public static void redirectToLogin(Context context) {
        Intent intent = new Intent(context, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        context.startActivity(intent);
    }
    
    /**
     * Redirige vers la bonne activité selon le rôle de l'utilisateur
     */
    public static void redirectToCorrectActivity(Context context, String userType) {
        Intent intent;
        if ("driver".equals(userType)) {
            intent = new Intent(context, DriverMainActivity.class);
        } else {
            intent = new Intent(context, PassengerMainActivity.class);
        }
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        context.startActivity(intent);
    }
    
    /**
     * Vérifie l'authentification et redirige si nécessaire
     */
    public static boolean requireAuth(Context context) {
        if (!isAuthenticated()) {
            redirectToLogin(context);
            return false;
        }
        return true;
    }
}

