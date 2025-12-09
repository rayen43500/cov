package com.example.rideshare1.Models;

public class Driver extends User {
    private String licenseNumber;
    private String vehiclePlate;
    private String vehicleModel;
    private String vehiclePhotoUrl;

    public Driver() {
        super();
    }

    public Driver(String userId, String firstName, String lastName, String email, String phoneNumber) {
        super(userId, firstName, lastName, email, phoneNumber, "driver");
    }

    public String getLicenseNumber() {
        return licenseNumber;
    }

    public void setLicenseNumber(String licenseNumber) {
        this.licenseNumber = licenseNumber;
    }

    public String getVehiclePlate() {
        return vehiclePlate;
    }

    public void setVehiclePlate(String vehiclePlate) {
        this.vehiclePlate = vehiclePlate;
    }

    public String getVehicleModel() {
        return vehicleModel;
    }

    public void setVehicleModel(String vehicleModel) {
        this.vehicleModel = vehicleModel;
    }

    public String getVehiclePhotoUrl() {
        return vehiclePhotoUrl;
    }

    public void setVehiclePhotoUrl(String vehiclePhotoUrl) {
        this.vehiclePhotoUrl = vehiclePhotoUrl;
    }
}

