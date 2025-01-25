package fr.umontpellier.grabit.utils;

import android.util.Patterns;

public class InputValidator {
    public static ValidationResult validateRegistrationInput(String name, String email,
                                                             String password, String confirmPassword) {
        if (name.isEmpty()) {
            return new ValidationResult(false, "Name is required");
        }

        if (email.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            return new ValidationResult(false, "Valid email is required");
        }

        if (password.isEmpty() || password.length() < 8) {
            return new ValidationResult(false, "Password must be at least 8 characters");
        }

        if (!isPasswordStrong(password)) {
            return new ValidationResult(false,
                    "Password must contain uppercase, lowercase, number and special character");
        }

        if (!password.equals(confirmPassword)) {
            return new ValidationResult(false, "Passwords do not match");
        }

        return new ValidationResult(true, null);
    }

    private static boolean isPasswordStrong(String password) {
        return password.matches("^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=])(?=\\S+$).{8,}$");
    }
}