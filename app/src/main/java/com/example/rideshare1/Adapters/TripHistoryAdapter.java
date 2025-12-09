package com.example.rideshare1.Adapters;

import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.rideshare1.Activities.ReviewActivity;
import com.example.rideshare1.Activities.TripDetailsActivity;
import com.example.rideshare1.Models.Trip;
import com.example.rideshare1.R;
import com.google.android.material.button.MaterialButton;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class TripHistoryAdapter extends RecyclerView.Adapter<TripHistoryAdapter.TripHistoryViewHolder> {

    private List<Trip> tripList;
    private OnTripHistoryClickListener listener;
    private boolean isDriver;
    private String currentUserId;

    public interface OnTripHistoryClickListener {
        void onTripClick(Trip trip);
        void onReviewClick(Trip trip);
    }

    public TripHistoryAdapter(List<Trip> tripList, boolean isDriver, String currentUserId, OnTripHistoryClickListener listener) {
        this.tripList = tripList;
        this.isDriver = isDriver;
        this.currentUserId = currentUserId;
        this.listener = listener;
    }

    @NonNull
    @Override
    public TripHistoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_trip_history, parent, false);
        return new TripHistoryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TripHistoryViewHolder holder, int position) {
        Trip trip = tripList.get(position);
        holder.bind(trip, isDriver, listener);
    }

    @Override
    public int getItemCount() {
        return tripList.size();
    }

    static class TripHistoryViewHolder extends RecyclerView.ViewHolder {
        private TextView tvOrigin, tvDestination, tvDate, tvTime, tvPrice, tvStatus;
        private MaterialButton btnReview, btnView;

        public TripHistoryViewHolder(@NonNull View itemView) {
            super(itemView);
            tvOrigin = itemView.findViewById(R.id.tvOrigin);
            tvDestination = itemView.findViewById(R.id.tvDestination);
            tvDate = itemView.findViewById(R.id.tvDate);
            tvTime = itemView.findViewById(R.id.tvTime);
            tvPrice = itemView.findViewById(R.id.tvPrice);
            tvStatus = itemView.findViewById(R.id.tvStatus);
            btnReview = itemView.findViewById(R.id.btnReview);
            btnView = itemView.findViewById(R.id.btnView);
        }

        public void bind(Trip trip, boolean isDriver, OnTripHistoryClickListener listener) {
            tvOrigin.setText(trip.getOrigin());
            tvDestination.setText(trip.getDestination());
            
            SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            tvDate.setText(dateFormat.format(trip.getDate()));
            tvTime.setText(trip.getTime());
            tvPrice.setText(String.format("%.2f TND", trip.getPrice()));
            
            // Afficher le statut du trajet
            String status = trip.getStatus() != null ? trip.getStatus() : "active";
            
            if (tvStatus != null) {
                String statusText = "";
                int statusColor = 0xFF757575;
                
                switch (status) {
                    case "active":
                        statusText = "Actif";
                        statusColor = 0xFF4CAF50; // Vert
                        break;
                    case "in_progress":
                        statusText = "En cours";
                        statusColor = 0xFFFF9800; // Orange
                        break;
                    case "completed":
                        statusText = "Terminé";
                        statusColor = 0xFF2196F3; // Bleu
                        break;
                    case "cancelled":
                        statusText = "Annulé";
                        statusColor = 0xFFF44336; // Rouge
                        break;
                    default:
                        statusText = status;
                }
                
                tvStatus.setText(statusText);
                tvStatus.setTextColor(statusColor);
            }
            
            // Afficher le bouton "Noter" UNIQUEMENT pour les passagers et les trajets TERMINÉS
            // Les règles métier sont vérifiées dans TripHistoryFragment
            if (!isDriver && "completed".equals(status)) {
                btnReview.setVisibility(View.VISIBLE);
                btnReview.setOnClickListener(v -> listener.onReviewClick(trip));
            } else {
                btnReview.setVisibility(View.GONE);
            }
            
            btnView.setVisibility(View.VISIBLE);
            btnView.setOnClickListener(v -> listener.onTripClick(trip));
            
            itemView.setOnClickListener(v -> listener.onTripClick(trip));
        }
    }
}
