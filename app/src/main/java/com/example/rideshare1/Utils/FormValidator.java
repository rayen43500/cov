package com.example.rideshare1.Utils;

import android.text.TextUtils;
import android.util.Patterns;
import android.widget.EditText;

public class FormValidator {
    
    public static boolean isValidEmail(String email) {
        return !TextUtils.isEmpty(email) && Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }
    
    public static boolean isValidPhone(String phone) {
        // Format tunisien: 9 chiffres commençant par 2, 5, 9
        return !TextUtils.isEmpty(phone) && phone.matches("^[259]\\d{8}$");
    }
    
    public static boolean isValidPassword(String password) {
        // Au moins 6 caractères
        return !TextUtils.isEmpty(password) && password.length() >= 6;
    }
    
    public static boolean isValidName(String name) {
        return !TextUtils.isEmpty(name) && name.length() >= 2;
    }
    
    public static boolean isValidPrice(String price) {
        try {
            double p = Double.parseDouble(price);
            return p > 0;
        } catch (NumberFormatException e) {
            return false;
        }
    }
    
    public static boolean isValidSeats(String seats) {
        try {
            int s = Integer.parseInt(seats);
            return s > 0 && s <= 8;
        } catch (NumberFormatException e) {
            return false;
        }
    }
    
    public static void setError(EditText editText, String error) {
        editText.setError(error);
        editText.requestFocus();
    }
}

