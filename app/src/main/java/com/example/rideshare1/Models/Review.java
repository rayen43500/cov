package com.example.rideshare1.Models;

import java.io.Serializable;
import java.util.Date;

public class Review implements Serializable {
    private String reviewId;
    private String tripId;
    private String reviewerId; // User who wrote the review
    private String reviewedId; // User being reviewed
    private double rating; // 1-5
    private String comment;
    private Date createdAt;

    public Review() {
        // Default constructor required for Firestore
    }

    public Review(String reviewId, String tripId, String reviewerId, String reviewedId, double rating, String comment) {
        this.reviewId = reviewId;
        this.tripId = tripId;
        this.reviewerId = reviewerId;
        this.reviewedId = reviewedId;
        this.rating = rating;
        this.comment = comment;
        this.createdAt = new Date();
    }

    // Getters and Setters
    public String getReviewId() {
        return reviewId;
    }

    public void setReviewId(String reviewId) {
        this.reviewId = reviewId;
    }

    public String getTripId() {
        return tripId;
    }

    public void setTripId(String tripId) {
        this.tripId = tripId;
    }

    public String getReviewerId() {
        return reviewerId;
    }

    public void setReviewerId(String reviewerId) {
        this.reviewerId = reviewerId;
    }

    public String getReviewedId() {
        return reviewedId;
    }

    public void setReviewedId(String reviewedId) {
        this.reviewedId = reviewedId;
    }

    public double getRating() {
        return rating;
    }

    public void setRating(double rating) {
        this.rating = rating;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }
}

