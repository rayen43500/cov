package com.example.rideshare1.ViewModels;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.rideshare1.Models.User;
import com.example.rideshare1.Repositories.UserRepository;

import java.util.Map;

public class UserViewModel extends ViewModel {
    private UserRepository userRepository;
    private MutableLiveData<User> userResult = new MutableLiveData<>();
    private MutableLiveData<String> errorMessage = new MutableLiveData<>();
    private MutableLiveData<Boolean> isLoading = new MutableLiveData<>();

    public UserViewModel() {
        userRepository = new UserRepository();
    }

    public LiveData<User> getUserResult() {
        return userResult;
    }

    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }

    public LiveData<Boolean> getIsLoading() {
        return isLoading;
    }

    public void getUserById(String userId) {
        isLoading.setValue(true);
        userRepository.getUserById(userId, new UserRepository.UserCallback() {
            @Override
            public void onSuccess(User user) {
                isLoading.setValue(false);
                userResult.setValue(user);
            }

            @Override
            public void onFailure(String error) {
                isLoading.setValue(false);
                errorMessage.setValue(error);
            }
        });
    }

    public void updateUser(String userId, Map<String, Object> updates) {
        isLoading.setValue(true);
        userRepository.updateUser(userId, updates, new UserRepository.UpdateCallback() {
            @Override
            public void onSuccess() {
                isLoading.setValue(false);
                errorMessage.setValue("User updated successfully");
            }

            @Override
            public void onFailure(String error) {
                isLoading.setValue(false);
                errorMessage.setValue(error);
            }
        });
    }

    public void updateProfilePhoto(String userId, String photoUrl) {
        isLoading.setValue(true);
        userRepository.updateProfilePhoto(userId, photoUrl, new UserRepository.UpdateCallback() {
            @Override
            public void onSuccess() {
                isLoading.setValue(false);
                errorMessage.setValue("Photo updated successfully");
            }

            @Override
            public void onFailure(String error) {
                isLoading.setValue(false);
                errorMessage.setValue(error);
            }
        });
    }

    public void deleteUser(String userId) {
        isLoading.setValue(true);
        userRepository.deleteUser(userId, new UserRepository.UpdateCallback() {
            @Override
            public void onSuccess() {
                isLoading.setValue(false);
                errorMessage.setValue("User deleted successfully");
            }

            @Override
            public void onFailure(String error) {
                isLoading.setValue(false);
                errorMessage.setValue(error);
            }
        });
    }
}

