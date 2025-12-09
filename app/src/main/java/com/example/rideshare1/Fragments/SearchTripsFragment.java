package com.example.rideshare1.Fragments;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.rideshare1.Activities.TripDetailsActivity;
import com.example.rideshare1.Adapters.TripAdapter;
import com.example.rideshare1.Models.Trip;
import com.example.rideshare1.R;
import com.example.rideshare1.Utils.NetworkUtils;
import com.example.rideshare1.ViewModels.TripViewModel;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.switchmaterial.SwitchMaterial;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class SearchTripsFragment extends Fragment {

    private static final String TAG = "SearchTripsFragment";
    private EditText etOrigin, etDestination, etDate, etMaxPrice;
    private MaterialButton btnSearch, btnFilter, btnToggleSearchForm;
    private SwitchMaterial switchShowCompleted;
    private RecyclerView recyclerView;
    private TripAdapter tripAdapter;
    private TripViewModel tripViewModel;
    private List<Trip> tripList;
    private SwipeRefreshLayout swipeRefreshLayout;
    private ProgressBar progressBar;
    private TextView tvEmptyState, tvResultsCount;
    private View filterCard, searchFormCard;
    private boolean filtersVisible = false;
    private boolean showCompletedTrips = false;
    private boolean searchFormVisible = true;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_search_trips, container, false);

        initializeViews(view);
        setupRecyclerView();
        setupDatePicker();
        setupListeners();
        observeTripList();

        // Charger tous les trajets disponibles au démarrage
        loadAllTrips();

        return view;
    }

    private void initializeViews(View view) {
        etOrigin = view.findViewById(R.id.etOrigin);
        etDestination = view.findViewById(R.id.etDestination);
        etDate = view.findViewById(R.id.etDate);
        etMaxPrice = view.findViewById(R.id.etMaxPrice);
        btnSearch = view.findViewById(R.id.btnSearch);
        btnFilter = view.findViewById(R.id.btnFilter);
        btnToggleSearchForm = view.findViewById(R.id.btnToggleSearchForm);
        switchShowCompleted = view.findViewById(R.id.switchShowCompleted);
        recyclerView = view.findViewById(R.id.recyclerViewTrips);
        swipeRefreshLayout = view.findViewById(R.id.swipeRefreshLayout);
        progressBar = view.findViewById(R.id.progressBar);
        tvEmptyState = view.findViewById(R.id.tvEmptyState);
        tvResultsCount = view.findViewById(R.id.tvResultsCount);
        filterCard = view.findViewById(R.id.filterCard);
        searchFormCard = view.findViewById(R.id.searchFormCard);
        
        tripList = new ArrayList<>();
        tripViewModel = new ViewModelProvider(this).get(TripViewModel.class);
    }

    private void setupRecyclerView() {
        tripAdapter = new TripAdapter(tripList, new TripAdapter.OnTripClickListener() {
            @Override
            public void onTripClick(Trip trip) {
                Intent intent = new Intent(getActivity(), TripDetailsActivity.class);
                intent.putExtra("tripId", trip.getTripId());
                startActivity(intent);
            }

            @Override
            public void onStartTripClick(Trip trip) {
                // Non applicable pour les passagers
            }

            @Override
            public void onEndTripClick(Trip trip) {
                // Non applicable pour les passagers
            }
        });

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(tripAdapter);
    }

    private void setupDatePicker() {
        etDate.setFocusable(false);
        etDate.setOnClickListener(v -> showDatePicker());
    }

    private void setupListeners() {
        btnSearch.setOnClickListener(v -> searchTrips());
        
        btnToggleSearchForm.setOnClickListener(v -> {
            searchFormVisible = !searchFormVisible;
            if (searchFormCard != null) {
                if (searchFormVisible) {
                    searchFormCard.setVisibility(View.VISIBLE);
                    btnToggleSearchForm.setText("Masquer");
                    btnToggleSearchForm.setIconResource(android.R.drawable.ic_menu_view);
                } else {
                    searchFormCard.setVisibility(View.GONE);
                    btnToggleSearchForm.setText("Afficher");
                    btnToggleSearchForm.setIconResource(android.R.drawable.ic_menu_revert);
                }
            }
        });
        
        btnFilter.setOnClickListener(v -> {
            filtersVisible = !filtersVisible;
            if (filtersVisible) {
                filterCard.setVisibility(View.VISIBLE);
                btnFilter.setText("Masquer filtres");
                btnFilter.setIconResource(android.R.drawable.ic_menu_close_clear_cancel);
            } else {
                filterCard.setVisibility(View.GONE);
                btnFilter.setText("Afficher filtres");
                btnFilter.setIconResource(android.R.drawable.ic_menu_sort_by_size);
            }
        });

        switchShowCompleted.setOnCheckedChangeListener((buttonView, isChecked) -> {
            showCompletedTrips = isChecked;
            // Recharger les trajets avec le nouveau filtre
            if (hasSearchCriteria()) {
                searchTrips();
            } else {
                loadAllTrips();
            }
        });

        swipeRefreshLayout.setOnRefreshListener(() -> {
            if (!NetworkUtils.isNetworkAvailable(getContext())) {
                Toast.makeText(getContext(), "Pas de connexion Internet", Toast.LENGTH_SHORT).show();
                swipeRefreshLayout.setRefreshing(false);
                return;
            }
            // Si des critères de recherche sont remplis, rechercher, sinon charger tous
            if (hasSearchCriteria()) {
                searchTrips();
            } else {
                loadAllTrips();
            }
        });
    }

    private boolean hasSearchCriteria() {
        return (etOrigin.getText().toString().trim().length() > 0 ||
                etDestination.getText().toString().trim().length() > 0 ||
                etDate.getText().toString().trim().length() > 0);
    }

    private void loadAllTrips() {
        if (!NetworkUtils.isNetworkAvailable(getContext())) {
            Toast.makeText(getContext(), "Pas de connexion Internet", Toast.LENGTH_SHORT).show();
            return;
        }

        Log.d(TAG, "Loading all trips (showCompleted: " + showCompletedTrips + ")");
        progressBar.setVisibility(View.VISIBLE);
        tvEmptyState.setVisibility(View.GONE);
        
        if (showCompletedTrips) {
            // Charger tous les trajets (actifs et terminés)
            tripViewModel.getAllTrips();
        } else {
            // Charger seulement les trajets actifs
            tripViewModel.getAllActiveTrips();
        }
    }

    private void searchTrips() {
        if (!NetworkUtils.isNetworkAvailable(getContext())) {
            Toast.makeText(getContext(), "Pas de connexion Internet", Toast.LENGTH_SHORT).show();
            return;
        }

        String origin = etOrigin.getText().toString().trim();
        String destination = etDestination.getText().toString().trim();
        String dateStr = etDate.getText().toString().trim();
        String maxPriceStr = etMaxPrice.getText().toString().trim();

        Date date = null;
        if (!dateStr.isEmpty()) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                date = sdf.parse(dateStr);
            } catch (Exception e) {
                Toast.makeText(getContext(), "Format de date invalide. Utilisez DD/MM/YYYY", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        Log.d(TAG, "Searching trips - Origin: " + origin + ", Destination: " + destination + ", Date: " + date + ", ShowCompleted: " + showCompletedTrips);
        progressBar.setVisibility(View.VISIBLE);
        tvEmptyState.setVisibility(View.GONE);
        
        // Si aucun critère n'est rempli, charger tous les trajets
        if (origin.isEmpty() && destination.isEmpty() && date == null) {
            loadAllTrips();
        } else {
            // Si le filtre pour afficher les trajets terminés est activé, utiliser getAllTrips
            // Sinon, utiliser searchTrips qui ne retourne que les trajets actifs
            if (showCompletedTrips) {
                // Pour inclure les trajets terminés, on doit récupérer tous les trajets puis filtrer
                tripViewModel.getAllTrips();
            } else {
                tripViewModel.searchTrips(origin, destination, date);
            }
        }
    }

    private void showDatePicker() {
        Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);
        
        DatePickerDialog datePickerDialog = new DatePickerDialog(
            getContext(),
            (view, selectedYear, selectedMonth, selectedDay) -> {
                calendar.set(selectedYear, selectedMonth, selectedDay);
                SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                etDate.setText(sdf.format(calendar.getTime()));
            },
            year, month, day
        );
        
        datePickerDialog.getDatePicker().setMinDate(System.currentTimeMillis() - 1000);
        datePickerDialog.show();
    }
    
    private void observeTripList() {
        tripViewModel.getTripListResult().observe(getViewLifecycleOwner(), trips -> {
            progressBar.setVisibility(View.GONE);
            swipeRefreshLayout.setRefreshing(false);
            
            if (trips != null) {
                Log.d(TAG, "Received " + trips.size() + " trips (showCompleted: " + showCompletedTrips + ")");
                
                Date now = new Date();
                List<Trip> filteredTrips = new ArrayList<>();
                
                // Récupérer les critères de recherche
                String origin = etOrigin.getText().toString().trim();
                String destination = etDestination.getText().toString().trim();
                String dateStr = etDate.getText().toString().trim();
                Date searchDate = null;
                if (!dateStr.isEmpty()) {
                    try {
                        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                        searchDate = sdf.parse(dateStr);
                    } catch (Exception e) {
                        // Ignorer si la date n'est pas valide
                    }
                }
                
                // Filtrer selon le statut, la date et les critères de recherche
                for (Trip trip : trips) {
                    boolean shouldInclude = false;
                    
                    // Vérifier si le trajet est terminé (date passée)
                    boolean isCompleted = trip.getDate() != null && trip.getDate().before(now);
                    
                    if (isCompleted) {
                        // Marquer automatiquement comme terminé si ce n'est pas déjà fait
                        if (!"completed".equals(trip.getStatus()) && !"cancelled".equals(trip.getStatus())) {
                            trip.setStatus("completed");
                            // Mettre à jour dans Firestore (en arrière-plan)
                            markTripAsCompleted(trip.getTripId());
                        }
                    }
                    
                    // Appliquer le filtre des trajets terminés
                    if (showCompletedTrips) {
                        // Afficher tous les trajets (actifs et terminés)
                        shouldInclude = true;
                    } else {
                        // Afficher seulement les trajets actifs et non terminés
                        if (!isCompleted && trip.getAvailableSeats() >= 1) {
                            shouldInclude = true;
                        }
                    }
                    
                    // Si on a des critères de recherche, appliquer les filtres
                    if (shouldInclude && hasSearchCriteria()) {
                        // Filtrer par origine
                        if (origin != null && !origin.trim().isEmpty()) {
                            String tripOrigin = trip.getOrigin() != null ? trip.getOrigin().toLowerCase().trim() : "";
                            String searchOrigin = origin.toLowerCase().trim();
                            if (!tripOrigin.contains(searchOrigin) && !searchOrigin.contains(tripOrigin)) {
                                shouldInclude = false;
                            }
                        }
                        
                        // Filtrer par destination
                        if (shouldInclude && destination != null && !destination.trim().isEmpty()) {
                            String tripDestination = trip.getDestination() != null ? trip.getDestination().toLowerCase().trim() : "";
                            String searchDestination = destination.toLowerCase().trim();
                            if (!tripDestination.contains(searchDestination) && !searchDestination.contains(tripDestination)) {
                                shouldInclude = false;
                            }
                        }
                        
                        // Filtrer par date
                        if (shouldInclude && searchDate != null && trip.getDate() != null) {
                            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                            String tripDateStr = dateFormat.format(trip.getDate());
                            String searchDateStr = dateFormat.format(searchDate);
                            if (!tripDateStr.equals(searchDateStr)) {
                                shouldInclude = false;
                            }
                        }
                    }
                    
                    if (shouldInclude) {
                        filteredTrips.add(trip);
                    }
                }
                
                // Filtrer par prix si spécifié
                String maxPriceStr = etMaxPrice.getText().toString().trim();
                if (!maxPriceStr.isEmpty()) {
                    try {
                        double maxPrice = Double.parseDouble(maxPriceStr);
                        filteredTrips = filterByPrice(filteredTrips, maxPrice);
                    } catch (NumberFormatException e) {
                        // Ignorer si le prix n'est pas valide
                    }
                }
                
                tripList.clear();
                tripList.addAll(filteredTrips);
                tripAdapter.notifyDataSetChanged();
                
                // Afficher le nombre de résultats
                if (tvResultsCount != null) {
                    tvResultsCount.setText(String.format("%d trajet(s) trouvé(s)", filteredTrips.size()));
                }
                
                // Afficher/masquer l'état vide
                if (filteredTrips.isEmpty()) {
                    tvEmptyState.setVisibility(View.VISIBLE);
                    tvEmptyState.setText("Aucun trajet trouvé.\nEssayez de modifier vos critères de recherche.");
                } else {
                    tvEmptyState.setVisibility(View.GONE);
                }
            } else {
                Log.w(TAG, "Trips list is null");
                tvEmptyState.setVisibility(View.VISIBLE);
                tvEmptyState.setText("Aucun trajet disponible pour le moment.");
            }
        });

        tripViewModel.getErrorMessage().observe(getViewLifecycleOwner(), error -> {
            progressBar.setVisibility(View.GONE);
            swipeRefreshLayout.setRefreshing(false);
            if (error != null && !error.isEmpty()) {
                Log.e(TAG, "Error: " + error);
                Toast.makeText(getContext(), "Erreur: " + error, Toast.LENGTH_SHORT).show();
            }
        });
        
        tripViewModel.getIsLoading().observe(getViewLifecycleOwner(), isLoading -> {
            if (!isLoading) {
                progressBar.setVisibility(View.GONE);
                swipeRefreshLayout.setRefreshing(false);
            }
        });
    }
    
    private void markTripAsCompleted(String tripId) {
        // Mettre à jour le statut dans Firestore en arrière-plan
        com.example.rideshare1.Repositories.TripRepository tripRepository = 
            new com.example.rideshare1.Repositories.TripRepository();
        
        java.util.Map<String, Object> updates = new java.util.HashMap<>();
        updates.put("status", "completed");
        
        tripRepository.updateTrip(tripId, updates, new com.example.rideshare1.Repositories.TripRepository.UpdateCallback() {
            @Override
            public void onSuccess() {
                Log.d(TAG, "Trip " + tripId + " marked as completed");
            }

            @Override
            public void onFailure(String error) {
                Log.e(TAG, "Error marking trip as completed: " + error);
            }
        });
    }
    
    private List<Trip> filterByPrice(List<Trip> trips, double maxPrice) {
        List<Trip> filtered = new ArrayList<>();
        for (Trip trip : trips) {
            if (trip.getPrice() <= maxPrice) {
                filtered.add(trip);
            }
        }
        return filtered;
    }
}
