package com.example.rideshare1.Adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.rideshare1.Models.Trip;
import com.example.rideshare1.R;
import com.google.android.material.button.MaterialButton;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class TripAdapter extends RecyclerView.Adapter<TripAdapter.TripViewHolder> {

    private List<Trip> tripList;
    private OnTripClickListener listener;
    private boolean isDriver;
    private String currentUserId;

    public interface OnTripClickListener {
        void onTripClick(Trip trip);
        void onStartTripClick(Trip trip);
        void onEndTripClick(Trip trip);
    }

    public TripAdapter(List<Trip> tripList, OnTripClickListener listener) {
        this.tripList = tripList;
        this.listener = listener;
        this.isDriver = false;
        this.currentUserId = null;
    }

    public TripAdapter(List<Trip> tripList, OnTripClickListener listener, boolean isDriver, String currentUserId) {
        this.tripList = tripList;
        this.listener = listener;
        this.isDriver = isDriver;
        this.currentUserId = currentUserId;
    }

    @NonNull
    @Override
    public TripViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_trip, parent, false);
        return new TripViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TripViewHolder holder, int position) {
        Trip trip = tripList.get(position);
        holder.bind(trip, listener, isDriver, currentUserId);
    }

    @Override
    public int getItemCount() {
        return tripList.size();
    }

    static class TripViewHolder extends RecyclerView.ViewHolder {
        private TextView tvOrigin, tvDestination, tvDate, tvTime, tvPrice, tvSeats, tvStatus;
        private LinearLayout llActionButtons;
        private MaterialButton btnStartTrip, btnEndTrip;

        public TripViewHolder(@NonNull View itemView) {
            super(itemView);
            tvOrigin = itemView.findViewById(R.id.tvOrigin);
            tvDestination = itemView.findViewById(R.id.tvDestination);
            tvDate = itemView.findViewById(R.id.tvDate);
            tvTime = itemView.findViewById(R.id.tvTime);
            tvPrice = itemView.findViewById(R.id.tvPrice);
            tvSeats = itemView.findViewById(R.id.tvSeats);
            tvStatus = itemView.findViewById(R.id.tvStatus);
            llActionButtons = itemView.findViewById(R.id.llActionButtons);
            btnStartTrip = itemView.findViewById(R.id.btnStartTrip);
            btnEndTrip = itemView.findViewById(R.id.btnEndTrip);
        }

        public void bind(Trip trip, OnTripClickListener listener, boolean isDriver, String currentUserId) {
            tvOrigin.setText(trip.getOrigin());
            tvDestination.setText(trip.getDestination());
            
            SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            tvDate.setText(dateFormat.format(trip.getDate()));
            tvTime.setText(trip.getTime());
            tvPrice.setText(String.format("%.2f TND", trip.getPrice()));
            
            // Afficher le statut du trajet
            String status = trip.getStatus() != null ? trip.getStatus() : "active";
            Date tripDate = trip.getDate();
            Date now = new Date();
            
            // Ne pas changer automatiquement le statut si c'est "in_progress" ou "completed"
            if (!"in_progress".equals(status) && !"completed".equals(status) && 
                !"cancelled".equals(status) && tripDate != null && tripDate.before(now)) {
                // Seulement pour les trajets actifs dont la date est passée
                // On ne change pas automatiquement, c'est le conducteur qui doit démarrer
            }
            
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
            
            // Afficher les places disponibles ou complet
            if (trip.getAvailableSeats() > 0) {
                tvSeats.setText(String.format("%d place(s) disponible(s)", trip.getAvailableSeats()));
                tvSeats.setTextColor(0xFF4CAF50);
            } else {
                tvSeats.setText("Complet");
                tvSeats.setTextColor(0xFFF44336);
            }

            // Afficher les boutons d'action pour le conducteur propriétaire
            if (isDriver && currentUserId != null && currentUserId.equals(trip.getDriverId())) {
                llActionButtons.setVisibility(View.VISIBLE);
                
                // Bouton Démarrer : visible seulement si le trajet est "active"
                if ("active".equals(status)) {
                    btnStartTrip.setVisibility(View.VISIBLE);
                    btnStartTrip.setOnClickListener(v -> listener.onStartTripClick(trip));
                    btnEndTrip.setVisibility(View.GONE);
                } 
                // Bouton Terminer : visible seulement si le trajet est "in_progress"
                else if ("in_progress".equals(status)) {
                    btnStartTrip.setVisibility(View.GONE);
                    btnEndTrip.setVisibility(View.VISIBLE);
                    btnEndTrip.setOnClickListener(v -> listener.onEndTripClick(trip));
                } 
                // Aucun bouton si terminé ou annulé
                else {
                    llActionButtons.setVisibility(View.GONE);
                }
            } else {
                llActionButtons.setVisibility(View.GONE);
            }

            itemView.setOnClickListener(v -> listener.onTripClick(trip));
        }
    }
}
