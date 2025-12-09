package com.example.rideshare1.ViewModels;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.rideshare1.Models.Reservation;
import com.example.rideshare1.Repositories.ReservationRepository;

import java.util.List;

public class ReservationViewModel extends ViewModel {
    private ReservationRepository reservationRepository;
    private MutableLiveData<Reservation> reservationResult = new MutableLiveData<>();
    private MutableLiveData<List<Reservation>> reservationListResult = new MutableLiveData<>();
    private MutableLiveData<String> errorMessage = new MutableLiveData<>();
    private MutableLiveData<Boolean> isLoading = new MutableLiveData<>();

    public ReservationViewModel() {
        reservationRepository = new ReservationRepository();
    }

    public LiveData<Reservation> getReservationResult() {
        return reservationResult;
    }

    public LiveData<List<Reservation>> getReservationListResult() {
        return reservationListResult;
    }

    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }

    public LiveData<Boolean> getIsLoading() {
        return isLoading;
    }

    public void createReservation(Reservation reservation) {
        isLoading.setValue(true);
        reservationRepository.createReservation(reservation, new ReservationRepository.ReservationCallback() {
            @Override
            public void onSuccess(Reservation reservation) {
                isLoading.setValue(false);
                reservationResult.setValue(reservation);
            }

            @Override
            public void onFailure(String error) {
                isLoading.setValue(false);
                errorMessage.setValue(error);
            }
        });
    }

    public void getReservationsByPassenger(String passengerId) {
        isLoading.setValue(true);
        reservationRepository.getReservationsByPassenger(passengerId, new ReservationRepository.ReservationListCallback() {
            @Override
            public void onSuccess(List<Reservation> reservations) {
                isLoading.setValue(false);
                reservationListResult.setValue(reservations);
            }

            @Override
            public void onFailure(String error) {
                isLoading.setValue(false);
                errorMessage.setValue(error);
            }
        });
    }

    public void getAcceptedReservationsByPassenger(String passengerId) {
        isLoading.setValue(true);
        reservationRepository.getAcceptedReservationsByPassenger(passengerId, new ReservationRepository.ReservationListCallback() {
            @Override
            public void onSuccess(List<Reservation> reservations) {
                isLoading.setValue(false);
                reservationListResult.setValue(reservations);
            }

            @Override
            public void onFailure(String error) {
                isLoading.setValue(false);
                errorMessage.setValue(error);
            }
        });
    }

    public void getReservationsByDriver(String driverId) {
        isLoading.setValue(true);
        reservationRepository.getReservationsByDriver(driverId, new ReservationRepository.ReservationListCallback() {
            @Override
            public void onSuccess(List<Reservation> reservations) {
                isLoading.setValue(false);
                reservationListResult.setValue(reservations);
            }

            @Override
            public void onFailure(String error) {
                isLoading.setValue(false);
                errorMessage.setValue(error);
            }
        });
    }

    public void getPendingReservationsByDriver(String driverId) {
        isLoading.setValue(true);
        reservationRepository.getPendingReservationsByDriver(driverId, new ReservationRepository.ReservationListCallback() {
            @Override
            public void onSuccess(List<Reservation> reservations) {
                isLoading.setValue(false);
                reservationListResult.setValue(reservations);
            }

            @Override
            public void onFailure(String error) {
                isLoading.setValue(false);
                errorMessage.setValue(error);
            }
        });
    }

    public void acceptReservation(String reservationId, String tripId, int numberOfSeats) {
        isLoading.setValue(true);
        reservationRepository.acceptReservation(reservationId, tripId, numberOfSeats, new ReservationRepository.UpdateCallback() {
            @Override
            public void onSuccess() {
                isLoading.setValue(false);
                errorMessage.setValue("Reservation accepted");
            }

            @Override
            public void onFailure(String error) {
                isLoading.setValue(false);
                errorMessage.setValue(error);
            }
        });
    }

    public void rejectReservation(String reservationId) {
        isLoading.setValue(true);
        reservationRepository.updateReservationStatus(reservationId, "rejected", new ReservationRepository.UpdateCallback() {
            @Override
            public void onSuccess() {
                isLoading.setValue(false);
                errorMessage.setValue("Reservation rejected");
            }

            @Override
            public void onFailure(String error) {
                isLoading.setValue(false);
                errorMessage.setValue(error);
            }
        });
    }

    public void cancelReservation(String reservationId) {
        isLoading.setValue(true);
        reservationRepository.cancelReservation(reservationId, new ReservationRepository.UpdateCallback() {
            @Override
            public void onSuccess() {
                isLoading.setValue(false);
                errorMessage.setValue("Reservation cancelled");
            }

            @Override
            public void onFailure(String error) {
                isLoading.setValue(false);
                errorMessage.setValue(error);
            }
        });
    }
}
