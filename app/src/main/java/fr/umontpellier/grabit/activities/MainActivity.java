package fr.umontpellier.grabit.activities;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import fr.umontpellier.grabit.R;
import fr.umontpellier.grabit.firebase.FirebaseManager;
import fr.umontpellier.grabit.utils.SessionManager;

public class MainActivity extends AppCompatActivity {
    private Button loginButton, registerButton;
    private ProgressBar progressBar;
    private FirebaseManager firebaseManager;
    private SessionManager sessionManager;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initializeManagers();
        initializeViews();

        if (sessionManager.isLoggedIn()) {
            checkUserTypeAndRedirect();
            return;
        }

        setupClickListeners();
    }

    private void initializeManagers() {
        sessionManager = new SessionManager(this);
        firebaseManager = FirebaseManager.getInstance();
    }

    private void initializeViews() {
        loginButton = findViewById(R.id.loginButton);
        registerButton = findViewById(R.id.registerButton);
        progressBar = findViewById(R.id.progressBar);
    }

    private void setupClickListeners() {
        loginButton.setOnClickListener(v ->
                startActivity(new Intent(MainActivity.this, LoginActivity.class)));
        registerButton.setOnClickListener(v ->
                startActivity(new Intent(MainActivity.this, RegisterActivity.class)));
    }

    private void checkUserTypeAndRedirect() {
        showLoading();
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
        progressBar.setVisibility(View.VISIBLE);
        loginButton.setEnabled(false);
        registerButton.setEnabled(false);
    }

    private void hideLoading() {
        progressBar.setVisibility(View.GONE);
        loginButton.setEnabled(true);
        registerButton.setEnabled(true);
    }

    private void showError(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onBackPressed() {
        finishAffinity();
    }
}