package fr.umontpellier.grabit.activities;

import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import fr.umontpellier.grabit.R;
import fr.umontpellier.grabit.adapters.ProductDiscountAdapter;
import fr.umontpellier.grabit.databinding.ActivityDiscountManagerBinding;
import fr.umontpellier.grabit.firebase.FirebaseManager;
import fr.umontpellier.grabit.models.Product;

public class DiscountManagerActivity extends AppCompatActivity {
    private ActivityDiscountManagerBinding binding;
    private FirebaseManager firebaseManager;
    private ProductDiscountAdapter adapter;
    private List<Product> allProducts;          // All products from Firebase
    private List<Product> discountedProducts;   // Only products with active discounts

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityDiscountManagerBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        firebaseManager = FirebaseManager.getInstance();
        setupRecyclerView();
        setupFab();
        loadProducts();
    }

    /** Sets up the toolbar with back navigation and title */

    /** Initializes RecyclerView with the adapter */
    private void setupRecyclerView() {
        allProducts = new ArrayList<>();
        discountedProducts = new ArrayList<>();
        adapter = new ProductDiscountAdapter(this, discountedProducts, this::showDiscountDialog);

        binding.discountsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        binding.discountsRecyclerView.setAdapter(adapter);
    }

    /** Sets up the FloatingActionButton to add new discounts */
    private void setupFab() {
        binding.addDiscountFab.setOnClickListener(v -> showProductSelector());
    }

    /** Loads all products from Firebase and filters discounted products */
    private void loadProducts() {
        firebaseManager.getProducts()
                .addOnSuccessListener(productList -> {

                    // Clear existing lists
                    allProducts.clear();
                    discountedProducts.clear();

                    // Populate allProducts and discountedProducts
                    allProducts.addAll(productList);
                    for (Product product : productList) {
                        if (product.isDiscountActive()) {  // Use isDiscountActive() for filtering
                            discountedProducts.add(product);
                        }
                    }

                    // Notify adapter and update UI
                    adapter.notifyDataSetChanged();
                    updateEmptyState();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Error loading products: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show());
    }

    /** Updates the UI based on whether there are discounted products */
    private void updateEmptyState() {
        if (discountedProducts.isEmpty()) {
            binding.discountsRecyclerView.setVisibility(View.GONE);
            binding.emptyView.setVisibility(View.VISIBLE);
        } else {
            binding.discountsRecyclerView.setVisibility(View.VISIBLE);
            binding.emptyView.setVisibility(View.GONE);
        }
    }

    /** Displays a dialog to select a product to apply a discount */
    private void showProductSelector() {
        List<String> productTitles = new ArrayList<>();
        List<Product> availableProducts = new ArrayList<>();

        for (Product product : allProducts) {
            if (!product.isDiscountFlag()) {  // Check discountFlag instead of hasDiscount
                productTitles.add(product.getTitle());
                availableProducts.add(product);
            }
        }

        if (availableProducts.isEmpty()) {
            Toast.makeText(this, "All products have discounts", Toast.LENGTH_SHORT).show();
            return;
        }

        String[] items = productTitles.toArray(new String[0]);
        new MaterialAlertDialogBuilder(this)
                .setTitle("Select Product")
                .setItems(items, (dialog, which) -> showDiscountDialog(availableProducts.get(which)))
                .show();
    }

    /** Shows the discount dialog for adding/editing a discount */
    private void showDiscountDialog(Product product) {
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_discount, null);

        setupDiscountDialogViews(dialogView, product);

        builder.setView(dialogView)
                .setTitle(product.isDiscountActive() ? "Edit Discount" : "Add Discount")
                .setPositiveButton("Save", null)
                .setNegativeButton("Cancel", null);

        if (product.isDiscountActive()) {
            builder.setNeutralButton("Remove", null);
        }

        AlertDialog dialog = builder.create();
        dialog.show();

        // Handle Save button click
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            if (validateDiscountInput(dialogView)) {
                saveDiscount(product, dialogView);
                dialog.dismiss();
            }
        });

        // Handle Remove button click if applicable
        if (product.isDiscountActive()) {
            dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener(v -> {
                removeDiscount(product);
                dialog.dismiss();
            });
        }
    }

    /** Initializes the discount dialog views with existing discount data if any */
    private void setupDiscountDialogViews(View dialogView, Product product) {
        TextInputEditText amountInput = dialogView.findViewById(R.id.discount_amount_input);
        RadioGroup typeGroup = dialogView.findViewById(R.id.discount_type_group);
        TextInputEditText startDateInput = dialogView.findViewById(R.id.start_date_input);
        TextInputEditText endDateInput = dialogView.findViewById(R.id.end_date_input);

        if (product.isDiscountActive()) {
            amountInput.setText(String.valueOf(product.getDiscountAmount()));
            if ("FIXED".equals(product.getDiscountType())) {
                typeGroup.check(R.id.fixed_radio);
            } else {
                typeGroup.check(R.id.percentage_radio);
            }
            if (product.getDiscountStartDate() > 0) {
                startDateInput.setText(formatDate(product.getDiscountStartDate()));
            }
            if (product.getDiscountEndDate() > 0) {
                endDateInput.setText(formatDate(product.getDiscountEndDate()));
            }
        }

        setupDatePickers(startDateInput, endDateInput);
    }

    /** Validates the input fields in the discount dialog */
    private boolean validateDiscountInput(View dialogView) {
        TextInputEditText amountInput = dialogView.findViewById(R.id.discount_amount_input);
        TextInputEditText startDateInput = dialogView.findViewById(R.id.start_date_input);
        TextInputEditText endDateInput = dialogView.findViewById(R.id.end_date_input);

        String amount = amountInput.getText().toString().trim();
        if (amount.isEmpty()) {
            amountInput.setError("Required");
            return false;
        }

        try {
            double value = Double.parseDouble(amount);
            RadioGroup typeGroup = dialogView.findViewById(R.id.discount_type_group);
            boolean isPercentage = typeGroup.getCheckedRadioButtonId() == R.id.percentage_radio;

            if (isPercentage && (value <= 0 || value >= 100)) {
                amountInput.setError("Percentage must be between 0 and 100");
                return false;
            }
            if (!isPercentage && value <= 0) {
                amountInput.setError("Amount must be greater than 0");
                return false;
            }
        } catch (NumberFormatException e) {
            amountInput.setError("Invalid number");
            return false;
        }

        if (startDateInput.getText().toString().trim().isEmpty()) {
            startDateInput.setError("Required");
            return false;
        }

        if (endDateInput.getText().toString().trim().isEmpty()) {
            endDateInput.setError("Required");
            return false;
        }

        return true;
    }

    /** Sets up date pickers for start and end date inputs */
    private void setupDatePickers(TextInputEditText startInput, TextInputEditText endInput) {
        MaterialDatePicker<Long> startPicker = MaterialDatePicker.Builder.datePicker()
                .setTitleText("Select Start Date")
                .build();

        MaterialDatePicker<Long> endPicker = MaterialDatePicker.Builder.datePicker()
                .setTitleText("Select End Date")
                .build();

        startInput.setOnClickListener(v -> startPicker.show(getSupportFragmentManager(), "START_DATE"));
        endInput.setOnClickListener(v -> endPicker.show(getSupportFragmentManager(), "END_DATE"));

        startPicker.addOnPositiveButtonClickListener(selection ->
                startInput.setText(formatDate(selection)));

        endPicker.addOnPositiveButtonClickListener(selection ->
                endInput.setText(formatDate(selection)));
    }

    /** Formats milliseconds to a readable date string */
    private String formatDate(long timestamp) {
        return new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                .format(new Date(timestamp));
    }

    /** Saves the discount to Firebase and updates the UI */
    private void saveDiscount(Product product, View dialogView) {
        TextInputEditText amountInput = dialogView.findViewById(R.id.discount_amount_input);
        RadioGroup typeGroup = dialogView.findViewById(R.id.discount_type_group);
        TextInputEditText startDateInput = dialogView.findViewById(R.id.start_date_input);
        TextInputEditText endDateInput = dialogView.findViewById(R.id.end_date_input);

        double amount = Double.parseDouble(amountInput.getText().toString().trim());
        String type = (typeGroup.getCheckedRadioButtonId() == R.id.percentage_radio)
                ? "PERCENTAGE" : "FIXED";

        try {
            SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
            long startDate = dateFormat.parse(startDateInput.getText().toString().trim()).getTime();
            long endDate = dateFormat.parse(endDateInput.getText().toString().trim()).getTime();

            // Update product fields
            product.setDiscountFlag(true);  // Set discountFlag to true
            product.setDiscountAmount(amount);
            product.setDiscountType(type);
            product.setDiscountStartDate(startDate);
            product.setDiscountEndDate(endDate);

            // Debug: Log the discount details before updating

            // Update product in Firebase
            firebaseManager.updateProduct(product)
                    .addOnSuccessListener(unused -> {
                        Toast.makeText(this, "Discount updated", Toast.LENGTH_SHORT).show();
                        loadProducts();
                    })
                    .addOnFailureListener(e ->
                            Toast.makeText(this, "Error updating discount: " + e.getMessage(),
                                    Toast.LENGTH_SHORT).show());

        } catch (Exception e) {
            Toast.makeText(this, "Error parsing dates", Toast.LENGTH_SHORT).show();
        }
    }

    /** Removes the discount from a product */
    private void removeDiscount(Product product) {
        // Update product fields to remove discount
        product.setDiscountFlag(false);  // Set discountFlag to false
        product.setDiscountAmount(0.0);
        product.setDiscountType("PERCENTAGE");
        product.setDiscountStartDate(0);
        product.setDiscountEndDate(0);


        // Update product in Firebase
        firebaseManager.updateProduct(product)
                .addOnSuccessListener(unused -> {
                    Toast.makeText(this, "Discount removed", Toast.LENGTH_SHORT).show();
                    loadProducts();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Error removing discount: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show());
    }

    /** Handles toolbar back navigation */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}