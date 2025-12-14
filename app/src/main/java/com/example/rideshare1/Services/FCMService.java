package com.example.rideshare1.Services;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.os.Build;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import com.example.rideshare1.Activities.PassengerMainActivity;
import com.example.rideshare1.Activities.DriverMainActivity;
import com.example.rideshare1.R;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

/**
 * Service Firebase Cloud Messaging (FCM)
 * Gère la réception et l'affichage des notifications push
 * Utilisé pour notifier les utilisateurs des nouvelles réservations, messages, etc.
 */
public class FCMService extends FirebaseMessagingService {

    private static final String TAG = "FCMService";
    private static final String CHANNEL_ID = "rideshare_notifications";
    private static final String CHANNEL_NAME = "RideShare Notifications";

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
    }

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);
        
        Log.d(TAG, "Message FCM reçu depuis: " + remoteMessage.getFrom());

        // Vérifier si le message contient des données
        if (remoteMessage.getData().size() > 0) {
            Log.d(TAG, "Données du message: " + remoteMessage.getData());
        }

        // Vérifier si le message contient une notification
        if (remoteMessage.getNotification() != null) {
            String title = remoteMessage.getNotification().getTitle();
            String body = remoteMessage.getNotification().getBody();
            String type = remoteMessage.getData().get("type");
            
            Log.d(TAG, "Notification reçue - Titre: " + title + ", Corps: " + body);
            showNotification(title, body, type);
        } else if (remoteMessage.getData().size() > 0) {
            // Si pas de notification mais des données, créer une notification manuellement
            String title = remoteMessage.getData().get("title");
            String body = remoteMessage.getData().get("body");
            String type = remoteMessage.getData().get("type");
            
            if (title != null && body != null) {
                Log.d(TAG, "Notification créée depuis les données - Titre: " + title);
                showNotification(title, body, type);
            }
        }
    }

    @Override
    public void onNewToken(String token) {
        super.onNewToken(token);
        Log.d(TAG, "Nouveau token FCM reçu: " + token);
        // Enregistrer le token dans Firestore pour envoyer des notifications
        saveTokenToFirestore(token);
    }
    
    @Override
    public void onDeletedMessages() {
        super.onDeletedMessages();
        Log.d(TAG, "Messages FCM supprimés");
    }
    
    @Override
    public void onMessageSent(String msgId) {
        super.onMessageSent(msgId);
        Log.d(TAG, "Message FCM envoyé: " + msgId);
    }
    
    @Override
    public void onSendError(String msgId, Exception exception) {
        super.onSendError(msgId, exception);
        Log.e(TAG, "Erreur lors de l'envoi du message FCM: " + msgId, exception);
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("Notifications pour les réservations et messages");
            channel.enableVibration(true);
            channel.setVibrationPattern(new long[]{0, 500, 250, 500});

            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    private void showNotification(String title, String body, String type) {
        Intent intent;
        
        // Déterminer l'activité cible selon le type de notification
        if ("reservation".equals(type)) {
            intent = new Intent(this, DriverMainActivity.class);
        } else {
            intent = new Intent(this, PassengerMainActivity.class);
        }
        
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        
        PendingIntent pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle(title != null ? title : "RideShare")
                .setContentText(body)
                .setAutoCancel(true)
                .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(body));

        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager != null) {
            notificationManager.notify((int) System.currentTimeMillis(), notificationBuilder.build());
        }
    }

    /**
     * Sauvegarde le token FCM dans Firestore
     * Permet d'envoyer des notifications ciblées à l'utilisateur
     * @param token Le token FCM à sauvegarder
     */
    private void saveTokenToFirestore(String token) {
        try {
            // Récupérer l'userId depuis les SharedPreferences
            android.content.SharedPreferences prefs = getSharedPreferences("RideShareSession", Context.MODE_PRIVATE);
            String userId = prefs.getString("userId", null);
            
            if (userId != null && !userId.isEmpty()) {
                com.google.firebase.firestore.FirebaseFirestore firestore = 
                    com.google.firebase.firestore.FirebaseFirestore.getInstance();
                
                java.util.Map<String, Object> tokenData = new java.util.HashMap<>();
                tokenData.put("fcmToken", token);
                tokenData.put("updatedAt", com.google.firebase.Timestamp.now());
                
                firestore.collection("users").document(userId)
                    .update("fcmToken", token)
                    .addOnSuccessListener(aVoid -> {
                        Log.d(TAG, "Token FCM sauvegardé avec succès pour l'utilisateur: " + userId);
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Erreur lors de la sauvegarde du token FCM", e);
                    });
            } else {
                Log.w(TAG, "Impossible de sauvegarder le token FCM: userId non trouvé dans la session");
            }
        } catch (Exception e) {
            Log.e(TAG, "Erreur lors de la sauvegarde du token FCM", e);
        }
    }
}

