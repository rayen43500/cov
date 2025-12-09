package com.example.rideshare1.Adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.rideshare1.Models.Conversation;
import com.example.rideshare1.R;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class ConversationAdapter extends RecyclerView.Adapter<ConversationAdapter.ConversationViewHolder> {

    private List<Conversation> conversationList;
    private OnConversationClickListener listener;

    public interface OnConversationClickListener {
        void onConversationClick(Conversation conversation);
    }

    public ConversationAdapter(List<Conversation> conversationList, OnConversationClickListener listener) {
        this.conversationList = conversationList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ConversationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_conversation, parent, false);
        return new ConversationViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ConversationViewHolder holder, int position) {
        Conversation conversation = conversationList.get(position);
        holder.bind(conversation, listener);
    }

    @Override
    public int getItemCount() {
        return conversationList.size();
    }

    static class ConversationViewHolder extends RecyclerView.ViewHolder {
        private TextView tvUserName, tvLastMessage, tvTime, tvUnreadCount;

        public ConversationViewHolder(@NonNull View itemView) {
            super(itemView);
            tvUserName = itemView.findViewById(R.id.tvUserName);
            tvLastMessage = itemView.findViewById(R.id.tvLastMessage);
            tvTime = itemView.findViewById(R.id.tvTime);
            tvUnreadCount = itemView.findViewById(R.id.tvUnreadCount);
        }

        public void bind(Conversation conversation, OnConversationClickListener listener) {
            tvUserName.setText(conversation.getOtherUserName() != null ? conversation.getOtherUserName() : "Utilisateur");
            
            String lastMessage = conversation.getLastMessage();
            if (lastMessage != null && lastMessage.length() > 50) {
                lastMessage = lastMessage.substring(0, 50) + "...";
            }
            tvLastMessage.setText(lastMessage != null ? lastMessage : "Aucun message");
            
            if (conversation.getLastMessageTime() != null) {
                SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
                tvTime.setText(sdf.format(conversation.getLastMessageTime()));
            } else {
                tvTime.setText("");
            }
            
            if (conversation.isHasUnreadMessages() && conversation.getUnreadCount() > 0) {
                tvUnreadCount.setVisibility(View.VISIBLE);
                tvUnreadCount.setText(String.valueOf(conversation.getUnreadCount()));
            } else {
                tvUnreadCount.setVisibility(View.GONE);
            }
            
            itemView.setOnClickListener(v -> listener.onConversationClick(conversation));
        }
    }
}

