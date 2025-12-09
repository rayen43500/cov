package com.example.rideshare1.Fragments;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.rideshare1.Activities.ChatActivity;
import com.example.rideshare1.Adapters.ConversationAdapter;
import com.example.rideshare1.Models.Conversation;
import com.example.rideshare1.R;
import com.example.rideshare1.Repositories.MessageRepository;
import com.example.rideshare1.Repositories.UserRepository;
import com.example.rideshare1.Utils.NetworkUtils;
import com.example.rideshare1.Utils.SessionManager;

import java.util.ArrayList;
import java.util.List;

public class ConversationsFragment extends Fragment {

    private static final String TAG = "ConversationsFragment";
    private RecyclerView recyclerView;
    private ConversationAdapter conversationAdapter;
    private MessageRepository messageRepository;
    private UserRepository userRepository;
    private List<Conversation> conversationList;
    private TextView tvEmptyState;
    private SessionManager sessionManager;
    private SwipeRefreshLayout swipeRefreshLayout;
    private ProgressBar progressBar;
    private String currentUserId;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_conversations, container, false);

        initializeViews(view);
        setupRecyclerView();
        setupListeners();
        loadConversations();

        return view;
    }

    private void initializeViews(View view) {
        recyclerView = view.findViewById(R.id.recyclerViewConversations);
        tvEmptyState = view.findViewById(R.id.tvEmptyState);
        swipeRefreshLayout = view.findViewById(R.id.swipeRefreshLayout);
        progressBar = view.findViewById(R.id.progressBar);
        sessionManager = new SessionManager(getContext());
        currentUserId = sessionManager.getUserId();

        conversationList = new ArrayList<>();
        messageRepository = new MessageRepository();
        userRepository = new UserRepository();
    }

    private void setupRecyclerView() {
        conversationAdapter = new ConversationAdapter(conversationList, new ConversationAdapter.OnConversationClickListener() {
            @Override
            public void onConversationClick(Conversation conversation) {
                Intent intent = new Intent(getActivity(), ChatActivity.class);
                intent.putExtra("otherUserId", conversation.getOtherUserId());
                startActivity(intent);
            }
        });

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(conversationAdapter);
    }

    private void setupListeners() {
        swipeRefreshLayout.setOnRefreshListener(() -> {
            if (!NetworkUtils.isNetworkAvailable(getContext())) {
                Toast.makeText(getContext(), "Pas de connexion Internet", Toast.LENGTH_SHORT).show();
                swipeRefreshLayout.setRefreshing(false);
                return;
            }
            loadConversations();
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        loadConversations();
    }

    public void refreshConversations() {
        loadConversations();
    }

    private void loadConversations() {
        if (currentUserId == null || currentUserId.isEmpty()) {
            Log.e(TAG, "User ID is null or empty");
            if (progressBar != null) {
                progressBar.setVisibility(View.GONE);
            }
            swipeRefreshLayout.setRefreshing(false);
            return;
        }

        if (progressBar != null) {
            progressBar.setVisibility(View.VISIBLE);
        }

        Log.d(TAG, "Loading conversations for user: " + currentUserId);

        messageRepository.getAllConversations(currentUserId, new MessageRepository.ConversationListCallback() {
            @Override
            public void onSuccess(List<Conversation> conversations) {
                if (progressBar != null) {
                    progressBar.setVisibility(View.GONE);
                }
                swipeRefreshLayout.setRefreshing(false);

                Log.d(TAG, "Loaded " + conversations.size() + " conversations");

                // Charger les noms des utilisateurs pour chaque conversation
                loadUserNames(conversations);
            }

            @Override
            public void onFailure(String error) {
                if (progressBar != null) {
                    progressBar.setVisibility(View.GONE);
                }
                swipeRefreshLayout.setRefreshing(false);
                Log.e(TAG, "Error loading conversations: " + error);
                Toast.makeText(getContext(), "Erreur: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadUserNames(List<Conversation> conversations) {
        if (conversations.isEmpty()) {
            conversationList.clear();
            conversationAdapter.notifyDataSetChanged();
            if (tvEmptyState != null) {
                tvEmptyState.setVisibility(View.VISIBLE);
                tvEmptyState.setText("Aucune conversation");
            }
            return;
        }

        final int[] loadedCount = {0};
        final int totalCount = conversations.size();

        for (Conversation conversation : conversations) {
            userRepository.getUserById(conversation.getOtherUserId(), new UserRepository.UserCallback() {
                @Override
                public void onSuccess(com.example.rideshare1.Models.User user) {
                    conversation.setOtherUserName(user.getFirstName() + " " + user.getLastName());
                    loadedCount[0]++;
                    
                    if (loadedCount[0] == totalCount) {
                        // Tous les noms sont chargés, mettre à jour la liste
                        conversationList.clear();
                        conversationList.addAll(conversations);
                        conversationAdapter.notifyDataSetChanged();
                        
                        if (tvEmptyState != null) {
                            tvEmptyState.setVisibility(View.GONE);
                        }
                    }
                }

                @Override
                public void onFailure(String error) {
                    conversation.setOtherUserName("Utilisateur");
                    loadedCount[0]++;
                    
                    if (loadedCount[0] == totalCount) {
                        conversationList.clear();
                        conversationList.addAll(conversations);
                        conversationAdapter.notifyDataSetChanged();
                        
                        if (tvEmptyState != null) {
                            tvEmptyState.setVisibility(View.GONE);
                        }
                    }
                }
            });
        }
    }
}

