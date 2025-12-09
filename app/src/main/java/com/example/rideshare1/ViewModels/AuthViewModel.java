package com.example.rideshare1.ViewModels;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.rideshare1.Repositories.AuthRepository;

public class AuthViewModel extends ViewModel {
    private AuthRepository authRepository;
    private MutableLiveData<String> authResult = new MutableLiveData<>();
    private MutableLiveData<String> loginResult = new MutableLiveData<>(); // Format: "SUCCESS:userId:userType" or "ERROR:message"
    private MutableLiveData<Boolean> isLoading = new MutableLiveData<>();

    public AuthViewModel() {
        authRepository = new AuthRepository();
    }

    public LiveData<String> getAuthResult() {
        return authResult;
    }
    
    public LiveData<String> getLoginResult() {
        return loginResult;
    }

    public LiveData<Boolean> getIsLoading() {
        return isLoading;
    }

    public void registerUser(String email, String password, String firstName, String lastName,
                            String phoneNumber, String userType) {
        isLoading.setValue(true);
        authRepository.registerUser(email, password, firstName, lastName, phoneNumber, userType,
                new AuthRepository.AuthCallback() {
                    @Override
                    public void onSuccess(String userId) {
                        isLoading.setValue(false);
                        authResult.setValue("SUCCESS:" + userId);
                    }

                    @Override
                    public void onFailure(String error) {
                        isLoading.setValue(false);
                        authResult.setValue("ERROR:" + error);
                    }
                });
    }

    public void registerDriver(String email, String password, String firstName, String lastName,
                              String phoneNumber, String licenseNumber, String vehiclePlate,
                              String vehicleModel) {
        isLoading.setValue(true);
        authRepository.registerDriver(email, password, firstName, lastName, phoneNumber,
                licenseNumber, vehiclePlate, vehicleModel, new AuthRepository.AuthCallback() {
                    @Override
                    public void onSuccess(String userId) {
                        isLoading.setValue(false);
                        authResult.setValue("SUCCESS:" + userId);
                    }

                    @Override
                    public void onFailure(String error) {
                        isLoading.setValue(false);
                        authResult.setValue("ERROR:" + error);
                    }
                });
    }

    public void loginUser(String email, String password) {
        isLoading.setValue(true);
        authRepository.loginUserWithType(email, password, new AuthRepository.LoginCallback() {
            @Override
            public void onSuccess(String userId, String userType) {
                isLoading.setValue(false);
                loginResult.setValue("SUCCESS:" + userId + ":" + userType);
            }

            @Override
            public void onFailure(String error) {
                isLoading.setValue(false);
                loginResult.setValue("ERROR:" + error);
            }
        });
    }

    public void resetPassword(String email) {
        isLoading.setValue(true);
        authRepository.resetPassword(email, new AuthRepository.AuthCallback() {
            @Override
            public void onSuccess(String userId) {
                isLoading.setValue(false);
                authResult.setValue("SUCCESS:" + userId);
            }

            @Override
            public void onFailure(String error) {
                isLoading.setValue(false);
                authResult.setValue("ERROR:" + error);
            }
        });
    }

    public void logout() {
        authRepository.logout();
    }
}

