package com.example.rideshare1.Utils;

import android.content.Context;
import android.location.Address;
import android.location.Geocoder;
import android.util.Log;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

public class GeocodingHelper {
    private static final String TAG = "GeocodingHelper";

    public static class LocationCoordinates {
        public double latitude;
        public double longitude;
        public boolean isValid;

        public LocationCoordinates(double lat, double lng) {
            this.latitude = lat;
            this.longitude = lng;
            this.isValid = true;
        }

        public LocationCoordinates() {
            this.isValid = false;
        }
    }

    public static LocationCoordinates getCoordinatesFromAddress(Context context, String address) {
        if (context == null || address == null || address.trim().isEmpty()) {
            return new LocationCoordinates();
        }

        Geocoder geocoder = new Geocoder(context, Locale.getDefault());
        try {
            List<Address> addresses = geocoder.getFromLocationName(address, 1);
            if (addresses != null && !addresses.isEmpty()) {
                Address location = addresses.get(0);
                return new LocationCoordinates(location.getLatitude(), location.getLongitude());
            }
        } catch (IOException e) {
            Log.e(TAG, "Error getting coordinates for address: " + address, e);
        }
        return new LocationCoordinates();
    }

    public static String getAddressFromCoordinates(Context context, double latitude, double longitude) {
        if (context == null) {
            return "";
        }

        Geocoder geocoder = new Geocoder(context, Locale.getDefault());
        try {
            List<Address> addresses = geocoder.getFromLocation(latitude, longitude, 1);
            if (addresses != null && !addresses.isEmpty()) {
                Address address = addresses.get(0);
                StringBuilder addressString = new StringBuilder();
                for (int i = 0; i <= address.getMaxAddressLineIndex(); i++) {
                    if (i > 0) addressString.append(", ");
                    addressString.append(address.getAddressLine(i));
                }
                return addressString.toString();
            }
        } catch (IOException e) {
            Log.e(TAG, "Error getting address from coordinates", e);
        }
        return "";
    }
}

