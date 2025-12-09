package com.example.rideshare1.Models;

import java.io.Serializable;
import java.util.Date;

public class Reservation implements Serializable {
    private String reservationId;
    private String tripId;
    private String passengerId;
    private String driverId;
    private String status; // "pending", "accepted", "rejected", "completed", "cancelled"
    private Date createdAt;
    private Date updatedAt;
    private int numberOfSeats;

    public Reservation() {
        // Default constructor required for Firestore
    }

    public Reservation(String reservationId, String tripId, String passengerId, String driverId, int numberOfSeats) {
        this.reservationId = reservationId;
        this.tripId = tripId;
        this.passengerId = passengerId;
        this.driverId = driverId;
        this.numberOfSeats = numberOfSeats;
        this.status = "pending";
        this.createdAt = new Date();
        this.updatedAt = new Date();
    }

    // Getters and Setters
    public String getReservationId() {
        return reservationId;
    }

    public void setReservationId(String reservationId) {
        this.reservationId = reservationId;
    }

    public String getTripId() {
        return tripId;
    }

    public void setTripId(String tripId) {
        this.tripId = tripId;
    }

    public String getPassengerId() {
        return passengerId;
    }

    public void setPassengerId(String passengerId) {
        this.passengerId = passengerId;
    }

    public String getDriverId() {
        return driverId;
    }

    public void setDriverId(String driverId) {
        this.driverId = driverId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
        this.updatedAt = new Date();
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public Date getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Date updatedAt) {
        this.updatedAt = updatedAt;
    }

    public int getNumberOfSeats() {
        return numberOfSeats;
    }

    public void setNumberOfSeats(int numberOfSeats) {
        this.numberOfSeats = numberOfSeats;
    }
}

