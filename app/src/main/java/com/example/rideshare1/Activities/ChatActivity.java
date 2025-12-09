package com.example.rideshare1.Activities;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.rideshare1.Adapters.MessageAdapter;
import com.example.rideshare1.Models.Message;
import com.example.rideshare1.R;
import com.example.rideshare1.Repositories.MessageRepository;
import com.example.rideshare1.Repositories.UserRepository;
import com.example.rideshare1.Utils.NetworkUtils;
import com.example.rideshare1.Utils.SessionManager;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

public class ChatActivity extends AppCompatActivity {

    private static final String TAG = "ChatActivity";
    private RecyclerView recyclerView;
    private TextInputEditText etMessage;
    private FloatingActionButton btnSend;
    private MessageAdapter messageAdapter;
    private MessageRepository messageRepository;
    private UserRepository userRepository;
    private SessionManager sessionManager;
    private String otherUserId;
    private String currentUserId;
    private List<Message> messageList;
    private ListenerRegistration messageListener;
    private TextView tvUserName, tvUserStatus;
    private ImageView ivBack;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Vérifier l'authentification
        if (!com.example.rideshare1.Utils.AuthGuard.requireAuth(this)) {
            finish();
            return;
        }
        
        setContentView(R.layout.activity_chat);

        otherUserId = getIntent().getStringExtra("otherUserId");
        if (otherUserId == null) {
            Log.e(TAG, "otherUserId is null");
            finish();
            return;
        }

        initializeViews();
        messageRepository = new MessageRepository();
        userRepository = new UserRepository();
        sessionManager = new SessionManager(this);
        currentUserId = sessionManager.getUserId();
        messageList = new ArrayList<>();

        Log.d(TAG, "ChatActivity initialized - currentUserId: " + currentUserId + ", otherUserId: " + otherUserId);

        setupRecyclerView();
        setupListeners();
        loadUserInfo();
        loadMessages();
        setupMessageListener();
    }

    private void initializeViews() {
        recyclerView = findViewById(R.id.recyclerViewMessages);
        etMessage = findViewById(R.id.etMessage);
        btnSend = findViewById(R.id.btnSend);
        tvUserName = findViewById(R.id.tvUserName);
        tvUserStatus = findViewById(R.id.tvUserStatus);
        ivBack = findViewById(R.id.ivBack);
        progressBar = findViewById(R.id.progressBar);
    }

    private void setupRecyclerView() {
        messageAdapter = new MessageAdapter(messageList, currentUserId);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setAdapter(messageAdapter);
    }

    private void setupListeners() {
        ivBack.setOnClickListener(v -> finish());

        btnSend.setOnClickListener(v -> {
            if (!NetworkUtils.isNetworkAvailable(this)) {
                Toast.makeText(this, "Pas de connexion Internet", Toast.LENGTH_SHORT).show();
                return;
            }
            sendMessage();
        });

        etMessage.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                btnSend.setEnabled(!TextUtils.isEmpty(s.toString().trim()));
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        etMessage.setOnEditorActionListener((v, actionId, event) -> {
            if (event != null && event.getAction() == android.view.KeyEvent.ACTION_DOWN && 
                event.getKeyCode() == android.view.KeyEvent.KEYCODE_ENTER && 
                !event.isShiftPressed()) {
                if (!TextUtils.isEmpty(etMessage.getText().toString().trim())) {
                    sendMessage();
                    return true;
                }
            }
            return false;
        });
    }

    private void loadUserInfo() {
        userRepository.getUserById(otherUserId, new UserRepository.UserCallback() {
            @Override
            public void onSuccess(com.example.rideshare1.Models.User user) {
                if (tvUserName != null) {
                    tvUserName.setText(user.getFirstName() + " " + user.getLastName());
                }
                if (tvUserStatus != null) {
                    tvUserStatus.setText("En ligne");
                }
            }

            @Override
            public void onFailure(String error) {
                if (tvUserName != null) {
                    tvUserName.setText("Utilisateur");
                }
            }
        });
    }

    private void loadMessages() {
        if (progressBar != null) {
            progressBar.setVisibility(View.VISIBLE);
        }
        
        Log.d(TAG, "Loading messages between " + currentUserId + " and " + otherUserId);
        
        messageRepository.getMessagesBetweenUsers(currentUserId, otherUserId, 
                new MessageRepository.MessageListCallback() {
                    @Override
                    public void onSuccess(List<Message> messages) {
                        if (progressBar != null) {
                            progressBar.setVisibility(View.GONE);
                        }
                        Log.d(TAG, "Loaded " + messages.size() + " messages");
                        messageList.clear();
                        messageList.addAll(messages);
                        messageAdapter.notifyDataSetChanged();
                        scrollToBottom();
                    }

                    @Override
                    public void onFailure(String error) {
                        if (progressBar != null) {
                            progressBar.setVisibility(View.GONE);
                        }
                        Log.e(TAG, "Error loading messages: " + error);
                        Toast.makeText(ChatActivity.this, "Erreur: " + error, Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void setupMessageListener() {
        Log.d(TAG, "Setting up message listener between " + currentUserId + " and " + otherUserId);
        
        messageListener = messageRepository.listenToMessages(currentUserId, otherUserId,
                new MessageRepository.MessageListener() {
                    @Override
                    public void onMessageReceived(Message message) {
                        Log.d(TAG, "New message received: " + message.getMessageId() + " from " + message.getSenderId() + " to " + message.getReceiverId());
                        
                        // Vérifier si le message n'existe pas déjà
                        boolean exists = false;
                        for (Message m : messageList) {
                            if (m.getMessageId().equals(message.getMessageId())) {
                                exists = true;
                                break;
                            }
                        }
                        
                        if (!exists) {
                            Log.d(TAG, "Adding new message to list");
                            messageList.add(message);
                            // Trier la liste après ajout
                            messageList.sort((m1, m2) -> {
                                if (m1.getTimestamp() == null || m2.getTimestamp() == null) {
                                    return 0;
                                }
                                return m1.getTimestamp().compareTo(m2.getTimestamp());
                            });
                            messageAdapter.notifyDataSetChanged();
                            scrollToBottom();
                            
                            // Marquer comme lu si c'est un message reçu
                            if (message.getReceiverId().equals(currentUserId)) {
                                messageRepository.markMessageAsRead(message.getMessageId());
                            }
                        } else {
                            Log.d(TAG, "Message already exists, skipping");
                        }
                    }
                });
    }

    private void sendMessage() {
        String content = etMessage.getText().toString().trim();
        if (TextUtils.isEmpty(content)) {
            return;
        }

        if (otherUserId == null || otherUserId.isEmpty()) {
            Toast.makeText(this, "Erreur: Destinataire invalide", Toast.LENGTH_SHORT).show();
            return;
        }

        String messageId = UUID.randomUUID().toString();
        Message message = new Message(messageId, currentUserId, otherUserId, content);
        message.setTimestamp(new Date());

        Log.d(TAG, "Sending message: " + content + " from " + currentUserId + " to " + otherUserId);

        btnSend.setEnabled(false);

        messageRepository.sendMessage(message, new MessageRepository.MessageCallback() {
            @Override
            public void onSuccess(Message message) {
                Log.d(TAG, "Message sent successfully");
                etMessage.setText("");
                btnSend.setEnabled(true);
                
                // Vérifier si le message n'existe pas déjà
                boolean exists = false;
                for (Message m : messageList) {
                    if (m.getMessageId().equals(message.getMessageId())) {
                        exists = true;
                        break;
                    }
                }
                
                if (!exists) {
                    messageList.add(message);
                    // Trier la liste après ajout
                    messageList.sort((m1, m2) -> {
                        if (m1.getTimestamp() == null || m2.getTimestamp() == null) {
                            return 0;
                        }
                        return m1.getTimestamp().compareTo(m2.getTimestamp());
                    });
                    messageAdapter.notifyDataSetChanged();
                    scrollToBottom();
                }
            }

            @Override
            public void onFailure(String error) {
                Log.e(TAG, "Error sending message: " + error);
                btnSend.setEnabled(true);
                Toast.makeText(ChatActivity.this, "Erreur: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void scrollToBottom() {
        if (messageList.size() > 0) {
            recyclerView.post(() -> {
                recyclerView.smoothScrollToPosition(messageList.size() - 1);
            });
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (messageListener != null) {
            messageListener.remove();
        }
    }
}
