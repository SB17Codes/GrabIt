package fr.umontpellier.grabit.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseUser;

import fr.umontpellier.grabit.databinding.ActivityRegisterBinding;
import fr.umontpellier.grabit.firebase.FirebaseManager;
import fr.umontpellier.grabit.models.User;
import fr.umontpellier.grabit.utils.InputValidator;
import fr.umontpellier.grabit.utils.ValidationResult;

public class RegisterActivity extends AppCompatActivity {
    private ActivityRegisterBinding binding;
    private FirebaseManager firebaseManager;
    private boolean isLoading = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityRegisterBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        firebaseManager = FirebaseManager.getInstance();
        setupClickListeners();
    }

    private void setupClickListeners() {
        binding.registerButton.setOnClickListener(v -> registerUser());
        binding.loginLink.setOnClickListener(v ->
                startActivity(new Intent(this, LoginActivity.class)));
    }

    private void registerUser() {
        if (isLoading) return;

        String name = binding.nameInput.getText().toString().trim();
        String email = binding.emailInput.getText().toString().trim();
        String password = binding.passwordInput.getText().toString().trim();
        String confirmPassword = binding.confirmPasswordInput.getText().toString().trim();

        ValidationResult validationResult =
                InputValidator.validateRegistrationInput(name, email, password, confirmPassword);

        if (!validationResult.isValid()) {
            showError(validationResult.getErrorMessage());
            return;
        }

        setLoading(true);
        createFirebaseUser(email, password, name);
    }

    private void createFirebaseUser(String email, String password, String name) {
        firebaseManager.getAuth().createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult().getUser() != null) {
                        createUserProfile(task.getResult().getUser(), name);
                    } else {
                        setLoading(false);
                        showError("Registration failed: " +
                                (task.getException() != null ? task.getException().getMessage() : ""));
                    }
                });
    }

    private void createUserProfile(FirebaseUser firebaseUser, String name) {
        User user = new User(firebaseUser.getUid(), firebaseUser.getEmail(), "customer", name);

        firebaseManager.createUserProfile(user)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        saveUserToDatabase(user);
                    } else {
                        setLoading(false);
                        showError("Failed to create profile: " +
                                (task.getException() != null ? task.getException().getMessage() : ""));
                    }
                });
    }

    private void saveUserToDatabase(User user) {
        firebaseManager.saveUserToDatabase(user)
                .addOnCompleteListener(task -> {
                    setLoading(false);
                    if (task.isSuccessful()) {
                        onRegistrationSuccess();
                    } else {
                        showError("Failed to save user data: " +
                                (task.getException() != null ? task.getException().getMessage() : ""));
                    }
                });
    }

    private void setLoading(boolean loading) {
        isLoading = loading;
        binding.progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        binding.registerButton.setEnabled(!loading);
    }

    private void showError(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    private void onRegistrationSuccess() {
        Toast.makeText(this, "Registration successful", Toast.LENGTH_SHORT).show();
        startActivity(new Intent(this, LoginActivity.class));
        finish();
    }
}