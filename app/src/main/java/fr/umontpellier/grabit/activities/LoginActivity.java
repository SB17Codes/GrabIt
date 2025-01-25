package fr.umontpellier.grabit.activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseUser;

import fr.umontpellier.grabit.R;
import fr.umontpellier.grabit.firebase.FirebaseManager;
import fr.umontpellier.grabit.utils.SessionManager;

public class LoginActivity extends AppCompatActivity {
    private TextInputEditText emailInput, passwordInput;
    private MaterialButton loginButton;
    private ProgressBar progressBar;
    private FirebaseManager firebaseManager;
    private SessionManager sessionManager;
    private boolean viewsInitialized = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        initializeManagers();

        if (sessionManager.isLoggedIn()) {
            System.out.println("User already logged in - redirecting");
            checkUserTypeAndRedirect();
            return;
        }

        initializeViews();
        setupClickListeners();
    }

    private void initializeManagers() {
        sessionManager = new SessionManager(this);
        firebaseManager = FirebaseManager.getInstance();
    }

    private void initializeViews() {
        emailInput = findViewById(R.id.emailInput);
        passwordInput = findViewById(R.id.passwordInput);
        loginButton = findViewById(R.id.loginButton);
        TextView registerLink = findViewById(R.id.registerLink);
        progressBar = findViewById(R.id.progressBar);
        viewsInitialized = true;
    }

    private void setupClickListeners() {
        if (!viewsInitialized)
            return;

        loginButton.setOnClickListener(v -> loginUser());
        findViewById(R.id.registerLink)
                .setOnClickListener(v -> startActivity(new Intent(this, RegisterActivity.class)));
    }

    private void loginUser() {
        if (!viewsInitialized)
            return;

        String email = emailInput.getText().toString().trim();
        String password = passwordInput.getText().toString().trim();

        if (!validateInputs(email, password)) {
            return;
        }

        showLoading();

        firebaseManager.getAuth().signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = firebaseManager.getAuth().getCurrentUser();
                        if (user != null) {
                            sessionManager.createLoginSession(user.getUid(), user.getEmail());
                            checkUserTypeAndRedirect();
                        } else {
                            hideLoading();
                            showError("User data not found");
                        }
                    } else {
                        hideLoading();
                        showError("Authentication failed: " +
                                (task.getException() != null ? task.getException().getMessage() : "Unknown error"));
                    }
                });
    }

    private boolean validateInputs(String email, String password) {
        if (!viewsInitialized)
            return false;

        if (email.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailInput.setError("Valid email is required");
            emailInput.requestFocus();
            return false;
        }

        if (password.isEmpty() || password.length() < 6) {
            passwordInput.setError("Password must be at least 6 characters");
            passwordInput.requestFocus();
            return false;
        }

        return true;
    }

    private void checkUserTypeAndRedirect() {
        if (firebaseManager.getAuth().getCurrentUser() == null) {
            showError("No authenticated user found");
            return;
        }

        String userId = firebaseManager.getAuth().getCurrentUser().getUid();

        firebaseManager.getDatabase().child("users").child(userId).get()
                .addOnSuccessListener(snapshot -> {
                    hideLoading();

                    if (snapshot.exists()) {
                        String userType = snapshot.child("userType").getValue(String.class);
                        Intent intent;

                        if ("manager".equals(userType)) {
                            intent = new Intent(this, ManagerDashboardActivity.class);
                        } else {
                            intent = new Intent(this, CustomerDashboardActivity.class);
                        }

                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK |
                                Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        finish();
                    } else {
                        showError("User data not found");
                        sessionManager.logout();
                    }
                })
                .addOnFailureListener(e -> {
                    hideLoading();
                    showError("Error getting user data: " + e.getMessage());
                    sessionManager.logout();
                });
    }

    private void showLoading() {
        if (!viewsInitialized)
            return;
        progressBar.setVisibility(View.VISIBLE);
        loginButton.setEnabled(false);
    }

    private void hideLoading() {
        if (!viewsInitialized)
            return;
        progressBar.setVisibility(View.GONE);
        loginButton.setEnabled(true);
    }

    private void showError(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    private void clearErrors() {
        if (viewsInitialized && emailInput != null && passwordInput != null) {
            emailInput.setError(null);
            passwordInput.setError(null);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        clearErrors();
    }

    @Override
    public void onBackPressed() {
        finishAffinity();
    }
}