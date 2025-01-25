package fr.umontpellier.grabit.utils;

public class ValidationResult {
    private final boolean isValid;
    private final String errorMessage;

    public ValidationResult(boolean isValid, String errorMessage) {
        this.isValid = isValid;
        this.errorMessage = errorMessage;
    }

    public boolean isValid() { return isValid; }
    public String getErrorMessage() { return errorMessage; }
}