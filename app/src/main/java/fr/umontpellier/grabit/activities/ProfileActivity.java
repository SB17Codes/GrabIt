package fr.umontpellier.grabit.activities;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.Task;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import fr.umontpellier.grabit.R;
import fr.umontpellier.grabit.databinding.ActivityProfielBinding;
import fr.umontpellier.grabit.firebase.FirebaseManager;
import fr.umontpellier.grabit.models.User;

public class ProfileActivity extends AppCompatActivity {
    private static final int PICK_IMAGE_REQUEST = 1;
    private Uri selectedImageUri;
    private ActivityProfielBinding binding;
    private FirebaseManager firebaseManager;
    private User currentUser;
    private Set<String> selectedInterests = new HashSet<>();
    private AlertDialog interestsDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityProfielBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        initializeFirebase();
        loadUserData();
        setupClickListeners();
    }

    private void initializeFirebase() {
        firebaseManager = FirebaseManager.getInstance();
        if (firebaseManager == null || firebaseManager.getAuth().getCurrentUser() == null) {
            showError("Authentication required");
            redirectToLogin();
        }
    }

    private void loadUserData() {
        if (firebaseManager == null) return;

        showLoading();
        firebaseManager.getCurrentUser()
                .addOnSuccessListener(user -> {
                    hideLoading();
                    currentUser = user;
                    displayUserData();
                })
                .addOnFailureListener(e -> {
                    hideLoading();
                    showError("Error loading user: " + e.getMessage());
                    redirectToLogin();
                });
    }

    private void displayUserData() {
        if (currentUser == null) return;

        binding.nameInput.setText(currentUser.getName());
        binding.emailInput.setText(currentUser.getEmail());
        binding.locationInput.setText(currentUser.getLocation());

        selectedInterests.clear();
        selectedInterests.addAll(currentUser.getInterests());
        updateInterestsDisplay();

        if (currentUser.getImgurl() != null && !currentUser.getImgurl().isEmpty()) {
            Glide.with(this)
                    .load(currentUser.getImgurl())
                    .placeholder(R.drawable.placeholder_image)
                    .into(binding.profileImage);
        }
    }

    private void setupClickListeners() {
        binding.profileImage.setOnClickListener(v -> openImagePicker());
        binding.addInterestButton.setOnClickListener(v -> showInterestsDialog());
        binding.saveButton.setOnClickListener(v -> saveChanges());
    }

    private void showInterestsDialog() {
        String[] availableInterests = {"Sports", "Music", "Art", "Technology", "Food",
                "Travel", "Books", "Movies", "Fashion", "Gaming"};
        boolean[] checkedInterests = new boolean[availableInterests.length];

        for (int i = 0; i < availableInterests.length; i++) {
            checkedInterests[i] = selectedInterests.contains(availableInterests[i]);
        }

        interestsDialog = new AlertDialog.Builder(this)
                .setTitle("Select Interests")
                .setMultiChoiceItems(availableInterests, checkedInterests, (dialog, which, isChecked) -> {
                    if (isChecked) {
                        selectedInterests.add(availableInterests[which]);
                    } else {
                        selectedInterests.remove(availableInterests[which]);
                    }
                })
                .setPositiveButton("OK", (dialog, which) -> updateInterestsDisplay())
                .setNegativeButton("Cancel", null)
                .create();

        interestsDialog.show();
    }

    private void updateInterestsDisplay() {
        binding.interestsChipGroup.removeAllViews();
        for (String interest : selectedInterests) {
            Chip chip = new Chip(this);
            chip.setText(interest);
            chip.setCloseIconVisible(true);
            chip.setOnCloseIconClickListener(v -> {
                selectedInterests.remove(interest);
                binding.interestsChipGroup.removeView(chip);
            });
            binding.interestsChipGroup.addView(chip);
        }
    }

    private void openImagePicker() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Select Picture"), PICK_IMAGE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK
                && data != null && data.getData() != null) {
            selectedImageUri = data.getData();
            Glide.with(this)
                    .load(selectedImageUri)
                    .into(binding.profileImage);
        }
    }

    private void saveChanges() {
        if (currentUser == null) return;
        showLoading();

        Task<?> updateTask;
        if (selectedImageUri != null) {
            updateTask = firebaseManager.uploadProfileImage(selectedImageUri, currentUser.getUid())
                    .continueWithTask(task -> {
                        if (!task.isSuccessful()) {
                            throw task.getException();
                        }
                        updateUserData(task.getResult());
                        return firebaseManager.saveUserToDatabase(currentUser);
                    });
        } else {
            updateUserData(null);
            updateTask = firebaseManager.saveUserToDatabase(currentUser);
        }

        updateTask
                .addOnSuccessListener(unused -> {
                    hideLoading();
                    showSuccess("Profile updated successfully");
                })
                .addOnFailureListener(e -> {
                    hideLoading();
                    showError("Error updating profile: " + e.getMessage());
                });
    }

    private void updateUserData(String imageUrl) {
        currentUser.setName(binding.nameInput.getText().toString());
        currentUser.setLocation(binding.locationInput.getText().toString());
        currentUser.setInterests(new ArrayList<>(selectedInterests));
        if (imageUrl != null) {
            currentUser.setImgurl(imageUrl);
        }
    }

    private void redirectToLogin() {
        startActivity(new Intent(this, LoginActivity.class));
        finish();
    }

    private void showError(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    private void showSuccess(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    private void showLoading() {
        binding.progressIndicator.setVisibility(View.VISIBLE);
        binding.saveButton.setEnabled(false);
    }

    private void hideLoading() {
        binding.progressIndicator.setVisibility(View.GONE);
        binding.saveButton.setEnabled(true);
    }


}