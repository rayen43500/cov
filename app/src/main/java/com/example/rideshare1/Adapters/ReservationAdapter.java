package com.example.rideshare1.Adapters;

import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.rideshare1.Activities.ChatActivity;
import com.example.rideshare1.Models.Reservation;
import com.example.rideshare1.R;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class ReservationAdapter extends RecyclerView.Adapter<ReservationAdapter.ReservationViewHolder> {

    private List<Reservation> reservationList;
    private boolean isDriver;
    private OnReservationClickListener listener;
    private String currentUserId;

    public interface OnReservationClickListener {
        void onReservationClick(Reservation reservation);
        void onAcceptClick(Reservation reservation);
        void onRejectClick(Reservation reservation);
        void onChatClick(Reservation reservation);
    }

    public ReservationAdapter(List<Reservation> reservationList, boolean isDriver, OnReservationClickListener listener) {
        this.reservationList = reservationList;
        this.isDriver = isDriver;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ReservationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_reservation, parent, false);
        return new ReservationViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ReservationViewHolder holder, int position) {
        Reservation reservation = reservationList.get(position);
        holder.bind(reservation, isDriver, listener);
    }

    @Override
    public int getItemCount() {
        return reservationList.size();
    }

    static class ReservationViewHolder extends RecyclerView.ViewHolder {
        private TextView tvTripId, tvStatus, tvDate;
        private Button btnAccept, btnReject, btnView, btnChat;

        public ReservationViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTripId = itemView.findViewById(R.id.tvTripId);
            tvStatus = itemView.findViewById(R.id.tvStatus);
            tvDate = itemView.findViewById(R.id.tvDate);
            btnAccept = itemView.findViewById(R.id.btnAccept);
            btnReject = itemView.findViewById(R.id.btnReject);
            btnView = itemView.findViewById(R.id.btnView);
            btnChat = itemView.findViewById(R.id.btnChat);
        }

        public void bind(Reservation reservation, boolean isDriver, OnReservationClickListener listener) {
            tvTripId.setText("Trajet #" + reservation.getTripId().substring(0, 8));
            
            String status = reservation.getStatus();
            String statusText = "";
            int statusColor = android.graphics.Color.GRAY;
            
            switch (status) {
                case "pending":
                    statusText = "En attente";
                    statusColor = 0xFFFF9800; // Orange
                    break;
                case "accepted":
                    statusText = "Acceptée";
                    statusColor = 0xFF4CAF50; // Vert
                    break;
                case "rejected":
                    statusText = "Refusée";
                    statusColor = 0xFFF44336; // Rouge
                    break;
                case "completed":
                    statusText = "Terminée";
                    statusColor = 0xFF2196F3; // Bleu
                    break;
                case "cancelled":
                    statusText = "Annulée";
                    statusColor = 0xFF757575; // Gris
                    break;
            }
            
            tvStatus.setText(statusText);
            tvStatus.setTextColor(statusColor);
            
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
            tvDate.setText(sdf.format(reservation.getCreatedAt()));
            
            // Afficher les boutons selon le rôle et le statut
            if (isDriver && "pending".equals(status)) {
                btnAccept.setVisibility(View.VISIBLE);
                btnReject.setVisibility(View.VISIBLE);
                btnView.setVisibility(View.GONE);
                btnChat.setVisibility(View.GONE);
            } else {
                btnAccept.setVisibility(View.GONE);
                btnReject.setVisibility(View.GONE);
                btnView.setVisibility(View.VISIBLE);
                // Afficher le bouton chat si la réservation est acceptée ou en attente
                if ("accepted".equals(status) || "pending".equals(status)) {
                    btnChat.setVisibility(View.VISIBLE);
                } else {
                    btnChat.setVisibility(View.GONE);
                }
            }
            
            btnAccept.setOnClickListener(v -> listener.onAcceptClick(reservation));
            btnReject.setOnClickListener(v -> listener.onRejectClick(reservation));
            btnView.setOnClickListener(v -> listener.onReservationClick(reservation));
            if (btnChat != null) {
                btnChat.setOnClickListener(v -> listener.onChatClick(reservation));
            }
            itemView.setOnClickListener(v -> listener.onReservationClick(reservation));
        }
    }
}
