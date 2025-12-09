package com.example.rideshare1.ViewModels;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.rideshare1.Models.Trip;
import com.example.rideshare1.Repositories.TripRepository;

import java.util.Date;
import java.util.List;

public class TripViewModel extends ViewModel {
    private TripRepository tripRepository;
    private MutableLiveData<Trip> tripResult = new MutableLiveData<>();
    private MutableLiveData<List<Trip>> tripListResult = new MutableLiveData<>();
    private MutableLiveData<String> errorMessage = new MutableLiveData<>();
    private MutableLiveData<Boolean> isLoading = new MutableLiveData<>();

    public TripViewModel() {
        tripRepository = new TripRepository();
    }

    public LiveData<Trip> getTripResult() {
        return tripResult;
    }

    public LiveData<List<Trip>> getTripListResult() {
        return tripListResult;
    }

    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }

    public LiveData<Boolean> getIsLoading() {
        return isLoading;
    }

    public void getTripById(String tripId) {
        isLoading.setValue(true);
        tripRepository.getTripById(tripId, new TripRepository.TripCallback() {
            @Override
            public void onSuccess(Trip trip) {
                isLoading.setValue(false);
                tripResult.setValue(trip);
            }

            @Override
            public void onFailure(String error) {
                isLoading.setValue(false);
                errorMessage.setValue(error);
            }
        });
    }

    public void createTrip(Trip trip) {
        isLoading.setValue(true);
        tripRepository.createTrip(trip, new TripRepository.TripCallback() {
            @Override
            public void onSuccess(Trip trip) {
                isLoading.setValue(false);
                tripResult.setValue(trip);
            }

            @Override
            public void onFailure(String error) {
                isLoading.setValue(false);
                errorMessage.setValue(error);
            }
        });
    }

    public void searchTrips(String origin, String destination, Date date) {
        isLoading.setValue(true);
        tripRepository.searchTrips(origin, destination, date, new TripRepository.TripListCallback() {
            @Override
            public void onSuccess(List<Trip> trips) {
                isLoading.setValue(false);
                tripListResult.setValue(trips);
            }

            @Override
            public void onFailure(String error) {
                isLoading.setValue(false);
                errorMessage.setValue(error);
            }
        });
    }

    public void getAllActiveTrips() {
        isLoading.setValue(true);
        tripRepository.getAllActiveTrips(new TripRepository.TripListCallback() {
            @Override
            public void onSuccess(List<Trip> trips) {
                isLoading.setValue(false);
                tripListResult.setValue(trips);
            }

            @Override
            public void onFailure(String error) {
                isLoading.setValue(false);
                errorMessage.setValue(error);
            }
        });
    }

    public void getAllTrips() {
        isLoading.setValue(true);
        tripRepository.getAllTrips(new TripRepository.TripListCallback() {
            @Override
            public void onSuccess(List<Trip> trips) {
                isLoading.setValue(false);
                tripListResult.setValue(trips);
            }

            @Override
            public void onFailure(String error) {
                isLoading.setValue(false);
                errorMessage.setValue(error);
            }
        });
    }

    public void getTripsByDriver(String driverId) {
        isLoading.setValue(true);
        tripRepository.getTripsByDriver(driverId, new TripRepository.TripListCallback() {
            @Override
            public void onSuccess(List<Trip> trips) {
                isLoading.setValue(false);
                tripListResult.setValue(trips);
            }

            @Override
            public void onFailure(String error) {
                isLoading.setValue(false);
                errorMessage.setValue(error);
            }
        });
    }

    public void updateTrip(String tripId, java.util.Map<String, Object> updates) {
        isLoading.setValue(true);
        tripRepository.updateTrip(tripId, updates, new TripRepository.UpdateCallback() {
            @Override
            public void onSuccess() {
                isLoading.setValue(false);
                errorMessage.setValue("Trip updated successfully");
            }

            @Override
            public void onFailure(String error) {
                isLoading.setValue(false);
                errorMessage.setValue(error);
            }
        });
    }

    public void deleteTrip(String tripId) {
        isLoading.setValue(true);
        tripRepository.deleteTrip(tripId, new TripRepository.UpdateCallback() {
            @Override
            public void onSuccess() {
                isLoading.setValue(false);
                errorMessage.setValue("Trip deleted successfully");
            }

            @Override
            public void onFailure(String error) {
                isLoading.setValue(false);
                errorMessage.setValue(error);
            }
        });
    }
}
