package com.example.rideshare1.Activities;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.rideshare1.Adapters.ChatBotAdapter;
import com.example.rideshare1.Models.ChatBotMessage;
import com.example.rideshare1.R;
import com.example.rideshare1.Utils.NetworkUtils;
import com.example.rideshare1.BuildConfig;
import com.google.ai.client.generativeai.GenerativeModel;
import com.google.ai.client.generativeai.java.ChatFutures;
import com.google.ai.client.generativeai.java.GenerativeModelFutures;
import com.google.ai.client.generativeai.type.Content;
import com.google.ai.client.generativeai.type.GenerateContentResponse;
import com.google.android.material.appbar.MaterialToolbar;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class ChatBotActivity extends AppCompatActivity {

    private RecyclerView recyclerViewMessages;
    private EditText etMessage;
    private ImageButton btnSend;
    private ProgressBar progressBar;
    private ChatBotAdapter chatAdapter;
    private List<ChatBotMessage> messageList;
    private GenerativeModel generativeModel;
    private ChatFutures chat;
    private Executor executor = Executors.newSingleThreadExecutor();

    private static final String GEMINI_API_KEY = BuildConfig.GEMINI_API_KEY;
    private static final String MODEL_NAME = "gemini-2.5-flash"; // Gemini 2.5 Flash

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chatbot);

        initializeViews();
        setupRecyclerView();
        initializeGemini();
        setupListeners();
    }

    private void initializeViews() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Assistant IA - Trajets");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        recyclerViewMessages = findViewById(R.id.recyclerViewMessages);
        etMessage = findViewById(R.id.etMessage);
        btnSend = findViewById(R.id.btnSend);
        progressBar = findViewById(R.id.progressBar);

        messageList = new ArrayList<>();
        
        // Message de bienvenue avec instructions pour Gemini
        String welcomeText = 
            "Bonjour ! Je suis votre assistant IA pour les trajets de covoiturage. " +
            "Je peux vous aider à trouver des informations sur les places disponibles, " +
            "les trajets, les prix, les horaires, et les destinations. " +
            "Posez-moi vos questions et je ferai de mon mieux pour vous aider !";
        
        ChatBotMessage welcomeMessage = new ChatBotMessage(welcomeText, false);
        messageList.add(welcomeMessage);
    }

    private void setupRecyclerView() {
        chatAdapter = new ChatBotAdapter(messageList);
        recyclerViewMessages.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewMessages.setAdapter(chatAdapter);
    }

    private void initializeGemini() {
        try {
            // Vérifier que la clé API est configurée
            if (TextUtils.isEmpty(GEMINI_API_KEY)) {
                Toast.makeText(this, 
                    "Veuillez configurer votre clé API Gemini (variable d'environnement GEMINI_API_KEY ou propriété Gradle)", 
                    Toast.LENGTH_LONG).show();
                return;
            }

            generativeModel = new GenerativeModel(
                MODEL_NAME,
                GEMINI_API_KEY
            );

            GenerativeModelFutures model = GenerativeModelFutures.from(generativeModel);
            chat = model.startChat();

            // Note: Le prompt système sera inclus dans le premier message utilisateur
        } catch (Exception e) {
            Toast.makeText(this, "Erreur d'initialisation de Gemini: " + e.getMessage(), 
                Toast.LENGTH_LONG).show();
        }
    }

    private void setupListeners() {
        btnSend.setOnClickListener(v -> sendMessage());

        etMessage.setOnEditorActionListener((v, actionId, event) -> {
            sendMessage();
            return true;
        });

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
    }

    private void sendMessage() {
        String messageText = etMessage.getText().toString().trim();
        
        if (TextUtils.isEmpty(messageText)) {
            return;
        }

        if (!NetworkUtils.isNetworkAvailable(this)) {
            Toast.makeText(this, "Pas de connexion Internet", Toast.LENGTH_SHORT).show();
            return;
        }

        if (generativeModel == null || chat == null) {
            Toast.makeText(this, "L'assistant IA n'est pas disponible", Toast.LENGTH_SHORT).show();
            return;
        }

        // Ajouter le message de l'utilisateur
        ChatBotMessage userMessage = new ChatBotMessage(messageText, true);
        messageList.add(userMessage);
        chatAdapter.notifyItemInserted(messageList.size() - 1);
        recyclerViewMessages.scrollToPosition(messageList.size() - 1);

        // Effacer le champ de saisie
        etMessage.setText("");

        // Afficher le progress bar
        progressBar.setVisibility(View.VISIBLE);
        btnSend.setEnabled(false);

        // Préparer le message avec contexte pour Gemini
        String contextualMessage = 
            "Tu es un assistant IA spécialisé dans le covoiturage. " +
            "Tu aides les passagers à trouver des informations sur les trajets, " +
            "les places disponibles, les prix, les horaires, et les destinations. " +
            "Réponds de manière claire, concise et amicale en français. " +
            "Question de l'utilisateur: " + messageText;

        // Envoyer au chatbot Gemini
        executor.execute(() -> {
            try {
                Content userContent = new Content.Builder()
                        .addText(contextualMessage)
                        .build();

                GenerateContentResponse genResponse = chat.sendMessage(userContent)
                    .get();

                String response = genResponse.getText();
                
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    btnSend.setEnabled(true);

                    // Ajouter la réponse du chatbot
                    ChatBotMessage botMessage = new ChatBotMessage(response, false);
                    messageList.add(botMessage);
                    chatAdapter.notifyItemInserted(messageList.size() - 1);
                    recyclerViewMessages.scrollToPosition(messageList.size() - 1);
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    btnSend.setEnabled(true);
                    
                    String errorMessage = "Désolé, une erreur s'est produite. " +
                        "Veuillez réessayer plus tard.";
                    ChatBotMessage errorBotMessage = new ChatBotMessage(errorMessage, false);
                    messageList.add(errorBotMessage);
                    chatAdapter.notifyItemInserted(messageList.size() - 1);
                    recyclerViewMessages.scrollToPosition(messageList.size() - 1);
                    
                    Toast.makeText(this, "Erreur: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}

