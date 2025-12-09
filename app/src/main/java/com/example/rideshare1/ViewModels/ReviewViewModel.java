package com.example.rideshare1.ViewModels;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.rideshare1.Models.Review;
import com.example.rideshare1.Repositories.ReviewRepository;

import java.util.List;

public class ReviewViewModel extends ViewModel {
    private ReviewRepository reviewRepository;
    private MutableLiveData<Review> reviewResult = new MutableLiveData<>();
    private MutableLiveData<List<Review>> reviewListResult = new MutableLiveData<>();
    private MutableLiveData<String> errorMessage = new MutableLiveData<>();
    private MutableLiveData<Boolean> isLoading = new MutableLiveData<>();

    public ReviewViewModel() {
        reviewRepository = new ReviewRepository();
    }

    public LiveData<Review> getReviewResult() {
        return reviewResult;
    }

    public LiveData<List<Review>> getReviewListResult() {
        return reviewListResult;
    }

    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }

    public LiveData<Boolean> getIsLoading() {
        return isLoading;
    }

    public void createReview(Review review) {
        isLoading.setValue(true);
        reviewRepository.createReview(review, new ReviewRepository.ReviewCallback() {
            @Override
            public void onSuccess(Review review) {
                isLoading.setValue(false);
                reviewResult.setValue(review);
            }

            @Override
            public void onFailure(String error) {
                isLoading.setValue(false);
                errorMessage.setValue(error);
            }
        });
    }

    public void getReviewsByUser(String userId) {
        isLoading.setValue(true);
        reviewRepository.getReviewsByUser(userId, new ReviewRepository.ReviewListCallback() {
            @Override
            public void onSuccess(List<Review> reviews) {
                isLoading.setValue(false);
                reviewListResult.setValue(reviews);
            }

            @Override
            public void onFailure(String error) {
                isLoading.setValue(false);
                errorMessage.setValue(error);
            }
        });
    }
}

