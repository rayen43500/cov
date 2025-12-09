package com.example.rideshare1.Repositories;

import android.util.Log;

import androidx.annotation.NonNull;

import com.example.rideshare1.Models.Conversation;
import com.example.rideshare1.Models.Message;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MessageRepository {
    private FirebaseFirestore firestore;
    private static final String TAG = "MessageRepository";
    private static final String COLLECTION_MESSAGES = "messages";

    public MessageRepository() {
        firestore = FirebaseFirestore.getInstance();
    }

    public interface MessageCallback {
        void onSuccess(Message message);
        void onFailure(String error);
    }

    public interface MessageListCallback {
        void onSuccess(List<Message> messages);
        void onFailure(String error);
    }

    public interface MessageListener {
        void onMessageReceived(Message message);
    }

    public interface ConversationListCallback {
        void onSuccess(List<Conversation> conversations);
        void onFailure(String error);
    }

    public void sendMessage(Message message, MessageCallback callback) {
        Map<String, Object> messageMap = new HashMap<>();
        messageMap.put("messageId", message.getMessageId());
        messageMap.put("senderId", message.getSenderId());
        messageMap.put("receiverId", message.getReceiverId());
        messageMap.put("content", message.getContent());
        messageMap.put("timestamp", message.getTimestamp());
        messageMap.put("isRead", message.isRead());

        Log.d(TAG, "Sending message from " + message.getSenderId() + " to " + message.getReceiverId());

        firestore.collection(COLLECTION_MESSAGES).document(message.getMessageId())
                .set(messageMap)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Log.d(TAG, "Message sent successfully from " + message.getSenderId() + " to " + message.getReceiverId());
                        callback.onSuccess(message);
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.e(TAG, "Error sending message", e);
                        callback.onFailure(e.getMessage());
                    }
                });
    }

    public void getMessagesBetweenUsers(String userId1, String userId2, MessageListCallback callback) {
        Log.d(TAG, "Getting messages between " + userId1 + " and " + userId2);
        
        // Récupérer les messages où userId1 est l'expéditeur et userId2 le destinataire
        Query query1 = firestore.collection(COLLECTION_MESSAGES)
                .whereEqualTo("senderId", userId1)
                .whereEqualTo("receiverId", userId2);
        
        // Récupérer les messages où userId2 est l'expéditeur et userId1 le destinataire
        Query query2 = firestore.collection(COLLECTION_MESSAGES)
                .whereEqualTo("senderId", userId2)
                .whereEqualTo("receiverId", userId1);

        // Exécuter les deux requêtes en parallèle
        Task<QuerySnapshot> task1 = query1.get();
        Task<QuerySnapshot> task2 = query2.get();

        Task<List<QuerySnapshot>> combinedTask = com.google.android.gms.tasks.Tasks.whenAllSuccess(task1, task2);

        combinedTask.addOnSuccessListener(new OnSuccessListener<List<QuerySnapshot>>() {
            @Override
            public void onSuccess(List<QuerySnapshot> querySnapshots) {
                List<Message> messages = new ArrayList<>();
                
                // Traiter les résultats de la première requête
                if (querySnapshots.size() > 0 && querySnapshots.get(0) != null) {
                    for (DocumentSnapshot document : querySnapshots.get(0)) {
                        Message message = document.toObject(Message.class);
                        if (message != null) {
                            messages.add(message);
                            Log.d(TAG, "Message found: " + message.getMessageId() + " from " + message.getSenderId() + " to " + message.getReceiverId());
                        }
                    }
                }
                
                // Traiter les résultats de la deuxième requête
                if (querySnapshots.size() > 1 && querySnapshots.get(1) != null) {
                    for (DocumentSnapshot document : querySnapshots.get(1)) {
                        Message message = document.toObject(Message.class);
                        if (message != null) {
                            messages.add(message);
                            Log.d(TAG, "Message found: " + message.getMessageId() + " from " + message.getSenderId() + " to " + message.getReceiverId());
                        }
                    }
                }
                
                // Trier par timestamp croissant
                messages.sort((m1, m2) -> {
                    if (m1.getTimestamp() == null || m2.getTimestamp() == null) {
                        return 0;
                    }
                    return m1.getTimestamp().compareTo(m2.getTimestamp());
                });
                
                Log.d(TAG, "Found " + messages.size() + " messages total");
                callback.onSuccess(messages);
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Log.e(TAG, "Error getting messages", e);
                callback.onFailure(e.getMessage());
            }
        });
    }

    public void getAllConversations(String userId, ConversationListCallback callback) {
        Log.d(TAG, "Getting all conversations for user: " + userId);
        
        // Récupérer tous les messages où l'utilisateur est soit l'expéditeur soit le destinataire
        Query query1 = firestore.collection(COLLECTION_MESSAGES)
                .whereEqualTo("senderId", userId);
        
        Query query2 = firestore.collection(COLLECTION_MESSAGES)
                .whereEqualTo("receiverId", userId);

        Task<QuerySnapshot> task1 = query1.get();
        Task<QuerySnapshot> task2 = query2.get();

        Task<List<QuerySnapshot>> combinedTask = com.google.android.gms.tasks.Tasks.whenAllSuccess(task1, task2);

        combinedTask.addOnSuccessListener(new OnSuccessListener<List<QuerySnapshot>>() {
            @Override
            public void onSuccess(List<QuerySnapshot> querySnapshots) {
                Map<String, Conversation> conversationMap = new HashMap<>();
                
                // Traiter les messages où l'utilisateur est l'expéditeur
                if (querySnapshots.size() > 0 && querySnapshots.get(0) != null) {
                    for (DocumentSnapshot document : querySnapshots.get(0)) {
                        Message message = document.toObject(Message.class);
                        if (message != null) {
                            String otherUserId = message.getReceiverId();
                            if (!conversationMap.containsKey(otherUserId)) {
                                Conversation conversation = new Conversation();
                                conversation.setOtherUserId(otherUserId);
                                conversation.setLastMessage(message.getContent());
                                conversation.setLastMessageTime(message.getTimestamp());
                                conversation.setHasUnreadMessages(false);
                                conversation.setUnreadCount(0);
                                conversationMap.put(otherUserId, conversation);
                            } else {
                                Conversation conversation = conversationMap.get(otherUserId);
                                if (message.getTimestamp() != null && 
                                    (conversation.getLastMessageTime() == null || 
                                     message.getTimestamp().after(conversation.getLastMessageTime()))) {
                                    conversation.setLastMessage(message.getContent());
                                    conversation.setLastMessageTime(message.getTimestamp());
                                }
                            }
                        }
                    }
                }
                
                // Traiter les messages où l'utilisateur est le destinataire
                if (querySnapshots.size() > 1 && querySnapshots.get(1) != null) {
                    for (DocumentSnapshot document : querySnapshots.get(1)) {
                        Message message = document.toObject(Message.class);
                        if (message != null) {
                            String otherUserId = message.getSenderId();
                            if (!conversationMap.containsKey(otherUserId)) {
                                Conversation conversation = new Conversation();
                                conversation.setOtherUserId(otherUserId);
                                conversation.setLastMessage(message.getContent());
                                conversation.setLastMessageTime(message.getTimestamp());
                                conversation.setHasUnreadMessages(!message.isRead());
                                conversation.setUnreadCount(message.isRead() ? 0 : 1);
                                conversationMap.put(otherUserId, conversation);
                            } else {
                                Conversation conversation = conversationMap.get(otherUserId);
                                if (message.getTimestamp() != null && 
                                    (conversation.getLastMessageTime() == null || 
                                     message.getTimestamp().after(conversation.getLastMessageTime()))) {
                                    conversation.setLastMessage(message.getContent());
                                    conversation.setLastMessageTime(message.getTimestamp());
                                    // Si c'est le dernier message et qu'il n'est pas lu, mettre à jour le statut
                                    if (!message.isRead()) {
                                        conversation.setHasUnreadMessages(true);
                                    }
                                }
                                // Compter tous les messages non lus
                                if (!message.isRead()) {
                                    conversation.setHasUnreadMessages(true);
                                }
                            }
                        }
                    }
                }
                
                // Recompter les messages non lus pour chaque conversation
                for (Conversation conversation : conversationMap.values()) {
                    int unreadCount = 0;
                    String otherUserId = conversation.getOtherUserId();
                    
                    // Compter les messages non lus où l'utilisateur est le destinataire
                    if (querySnapshots.size() > 1 && querySnapshots.get(1) != null) {
                        for (DocumentSnapshot document : querySnapshots.get(1)) {
                            Message message = document.toObject(Message.class);
                            if (message != null && 
                                message.getSenderId().equals(otherUserId) && 
                                !message.isRead()) {
                                unreadCount++;
                            }
                        }
                    }
                    
                    conversation.setUnreadCount(unreadCount);
                    conversation.setHasUnreadMessages(unreadCount > 0);
                }
                
                // Convertir la map en liste et trier par date décroissante
                List<Conversation> conversations = new ArrayList<>(conversationMap.values());
                conversations.sort((c1, c2) -> {
                    if (c1.getLastMessageTime() == null || c2.getLastMessageTime() == null) {
                        return 0;
                    }
                    return c2.getLastMessageTime().compareTo(c1.getLastMessageTime());
                });
                
                Log.d(TAG, "Found " + conversations.size() + " conversations");
                callback.onSuccess(conversations);
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Log.e(TAG, "Error getting conversations", e);
                callback.onFailure(e.getMessage());
            }
        });
    }

    public ListenerRegistration listenToMessages(String userId1, String userId2, MessageListener listener) {
        Log.d(TAG, "Setting up message listener between " + userId1 + " and " + userId2);
        
        // Créer deux listeners pour les deux directions
        ListenerRegistration listener1 = firestore.collection(COLLECTION_MESSAGES)
                .whereEqualTo("senderId", userId1)
                .whereEqualTo("receiverId", userId2)
                .addSnapshotListener(new EventListener<QuerySnapshot>() {
                    @Override
                    public void onEvent(QuerySnapshot snapshots, FirebaseFirestoreException e) {
                        if (e != null) {
                            Log.e(TAG, "Error listening to messages (direction 1)", e);
                            return;
                        }

                        if (snapshots != null && !snapshots.isEmpty()) {
                            for (DocumentChange change : snapshots.getDocumentChanges()) {
                                if (change.getType() == DocumentChange.Type.ADDED) {
                                    Message message = change.getDocument().toObject(Message.class);
                                    if (message != null) {
                                        Log.d(TAG, "New message received (direction 1): " + message.getMessageId());
                                        listener.onMessageReceived(message);
                                    }
                                }
                            }
                        }
                    }
                });

        ListenerRegistration listener2 = firestore.collection(COLLECTION_MESSAGES)
                .whereEqualTo("senderId", userId2)
                .whereEqualTo("receiverId", userId1)
                .addSnapshotListener(new EventListener<QuerySnapshot>() {
                    @Override
                    public void onEvent(QuerySnapshot snapshots, FirebaseFirestoreException e) {
                        if (e != null) {
                            Log.e(TAG, "Error listening to messages (direction 2)", e);
                            return;
                        }

                        if (snapshots != null && !snapshots.isEmpty()) {
                            for (DocumentChange change : snapshots.getDocumentChanges()) {
                                if (change.getType() == DocumentChange.Type.ADDED) {
                                    Message message = change.getDocument().toObject(Message.class);
                                    if (message != null) {
                                        Log.d(TAG, "New message received (direction 2): " + message.getMessageId());
                                        listener.onMessageReceived(message);
                                    }
                                }
                            }
                        }
                    }
                });

        // Retourner un listener composite qui peut supprimer les deux
        return new ListenerRegistration() {
            @Override
            public void remove() {
                listener1.remove();
                listener2.remove();
            }
        };
    }

    public void markMessageAsRead(String messageId) {
        firestore.collection(COLLECTION_MESSAGES).document(messageId)
                .update("isRead", true)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Log.d(TAG, "Message marked as read");
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.e(TAG, "Error marking message as read", e);
                    }
                });
    }
}
