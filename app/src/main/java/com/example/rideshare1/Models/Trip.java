package com.example.rideshare1.Models;

import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class Trip implements Serializable {
    private String tripId;
    private String driverId;
    private String origin;
    private String destination;
    private double originLat;
    private double originLng;
    private double destLat;
    private double destLng;
    private Date date;
    private String time;
    private double price;
    private int availableSeats;
    private int totalSeats;
    private String status; // "active", "completed", "cancelled"
    private Date createdAt;

    public Trip() {
        // Default constructor required for Firestore
    }

    public Trip(String tripId, String driverId, String origin, String destination, 
                Date date, String time, double price, int availableSeats) {
        this.tripId = tripId;
        this.driverId = driverId;
        this.origin = origin;
        this.destination = destination;
        this.date = date;
        this.time = time;
        this.price = price;
        this.availableSeats = availableSeats;
        this.totalSeats = availableSeats;
        this.status = "active";
        this.createdAt = new Date();
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("tripId", tripId);
        map.put("driverId", driverId);
        map.put("origin", origin);
        map.put("destination", destination);
        map.put("originLat", originLat);
        map.put("originLng", originLng);
        map.put("destLat", destLat);
        map.put("destLng", destLng);
        map.put("date", date);
        map.put("time", time);
        map.put("price", price);
        map.put("availableSeats", availableSeats);
        map.put("totalSeats", totalSeats);
        map.put("status", status);
        map.put("createdAt", createdAt);
        return map;
    }

    // Getters and Setters
    public String getTripId() {
        return tripId;
    }

    public void setTripId(String tripId) {
        this.tripId = tripId;
    }

    public String getDriverId() {
        return driverId;
    }

    public void setDriverId(String driverId) {
        this.driverId = driverId;
    }

    public String getOrigin() {
        return origin;
    }

    public void setOrigin(String origin) {
        this.origin = origin;
    }

    public String getDestination() {
        return destination;
    }

    public void setDestination(String destination) {
        this.destination = destination;
    }

    public double getOriginLat() {
        return originLat;
    }

    public void setOriginLat(double originLat) {
        this.originLat = originLat;
    }

    public double getOriginLng() {
        return originLng;
    }

    public void setOriginLng(double originLng) {
        this.originLng = originLng;
    }

    public double getDestLat() {
        return destLat;
    }

    public void setDestLat(double destLat) {
        this.destLat = destLat;
    }

    public double getDestLng() {
        return destLng;
    }

    public void setDestLng(double destLng) {
        this.destLng = destLng;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public int getAvailableSeats() {
        return availableSeats;
    }

    public void setAvailableSeats(int availableSeats) {
        this.availableSeats = availableSeats;
    }

    public int getTotalSeats() {
        return totalSeats;
    }

    public void setTotalSeats(int totalSeats) {
        this.totalSeats = totalSeats;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }
}
