package fr.umontpellier.grabit.activities;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.bumptech.glide.Glide;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.List;

import fr.umontpellier.grabit.R;
import fr.umontpellier.grabit.adapters.AdminProductAdapter;
import fr.umontpellier.grabit.databinding.ActivityAdminProductsBinding;
import fr.umontpellier.grabit.firebase.FirebaseManager;
import fr.umontpellier.grabit.models.Product;

public class AdminProductListActivity extends AppCompatActivity {
    private ActivityAdminProductsBinding binding;
    private FirebaseManager firebaseManager;
    private AdminProductAdapter adapter;
    private List<Product> products;
    private Uri selectedImageUri;
    private ActivityResultLauncher<String> imagePickerLauncher;
    private View currentDialogView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAdminProductsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        initializeFirebase();
        setupImagePicker();
        setupRecyclerView();
        setupFab();
        loadProducts();
    }

    private void initializeFirebase() {
        firebaseManager = FirebaseManager.getInstance();
    }

    private void setupImagePicker() {
        imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null && currentDialogView != null) {
                        selectedImageUri = uri;
                        ImageView imageView = currentDialogView.findViewById(R.id.product_image);
                        if (imageView != null) {
                            Glide.with(this)
                                    .load(uri)
                                    .centerCrop()
                                    .into(imageView);
                        }
                    }
                });
    }

    private void setupRecyclerView() {
        products = new ArrayList<>();
        adapter = new AdminProductAdapter(this, products, this::showModifyDialog);
        binding.productsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        binding.productsRecyclerView.setAdapter(adapter);
    }

    private void setupFab() {
        binding.addProductFab.setOnClickListener(v -> showAddDialog());
    }

    private void loadProducts() {
        showLoading();
        firebaseManager.getProducts()
                .addOnSuccessListener(productList -> {
                    hideLoading();
                    products.clear();
                    products.addAll(productList);
                    adapter.notifyDataSetChanged();
                    updateEmptyState();
                })
                .addOnFailureListener(e -> {
                    hideLoading();
                    showError("Error loading products: " + e.getMessage());
                    updateEmptyState();
                });
    }

    private void showAddDialog() {
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this);
        currentDialogView = getLayoutInflater().inflate(R.layout.dialog_add_product, null);
        selectedImageUri = null;

        setupProductDialogViews(currentDialogView, null);

        builder.setView(currentDialogView)
                .setTitle("Add Product")
                .setPositiveButton("Add", null)
                .setNegativeButton("Cancel", null);

        var dialog = builder.create();
        dialog.show();

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            if (validateProductInput(currentDialogView)) {
                addProduct(currentDialogView);
                dialog.dismiss();
            }
        });
    }

    private void showModifyDialog(Product product) {
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this);
        currentDialogView = getLayoutInflater().inflate(R.layout.dialog_modify_product, null);
        selectedImageUri = null;

        setupProductDialogViews(currentDialogView, product);

        builder.setView(currentDialogView)
                .setTitle("Modify Product")
                .setPositiveButton("Save", null)
                .setNegativeButton("Cancel", null)
                .setNeutralButton("Delete", null); // Set to null to prevent auto-dismiss

        var dialog = builder.create();
        dialog.show();

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            if (validateProductInput(currentDialogView)) {
                updateProduct(currentDialogView, product);
                dialog.dismiss();
            }
        });

        dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener(v -> {
            new MaterialAlertDialogBuilder(this)
                    .setTitle("Delete Product")
                    .setMessage("Are you sure you want to delete this product?")
                    .setPositiveButton("Delete", (confirmDialog, which) -> {
                        showLoading();
                        deleteProduct(product);
                        dialog.dismiss();
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        });

        // Style delete button
        dialog.getButton(AlertDialog.BUTTON_NEUTRAL)
                .setTextColor(getColor(R.color.error));
    }

    private void setupProductDialogViews(View dialogView, Product product) {
        ImageView productImage = dialogView.findViewById(R.id.product_image);
        dialogView.findViewById(R.id.select_image_button)
                .setOnClickListener(v -> imagePickerLauncher.launch("image/*"));

        if (product != null) {
            ((TextInputEditText) dialogView.findViewById(R.id.title_input)).setText(product.getTitle());
            ((TextInputEditText) dialogView.findViewById(R.id.price_input))
                    .setText(String.valueOf(product.getPrice()));
            ((TextInputEditText) dialogView.findViewById(R.id.description_input))
                    .setText(product.getDescription());
            ((TextInputEditText) dialogView.findViewById(R.id.quantity_input))
                    .setText(String.valueOf(product.getInventory_quantity()));
            ((TextInputEditText) dialogView.findViewById(R.id.grams_input))
                    .setText(String.valueOf(product.getGrams()));
            ((TextInputEditText) dialogView.findViewById(R.id.from_input)).setText(product.getFrom());

            if (product.getProduct_image() != null && !product.getProduct_image().isEmpty()) {
                Glide.with(this)
                        .load(product.getProduct_image())
                        .into(productImage);
            }
        }
    }

    private boolean validateProductInput(View dialogView) {
        TextInputEditText titleInput = dialogView.findViewById(R.id.title_input);
        TextInputEditText priceInput = dialogView.findViewById(R.id.price_input);
        TextInputEditText quantityInput = dialogView.findViewById(R.id.quantity_input);
        TextInputEditText gramsInput = dialogView.findViewById(R.id.grams_input);

        if (TextUtils.isEmpty(titleInput.getText()) ||
                TextUtils.isEmpty(priceInput.getText()) ||
                TextUtils.isEmpty(quantityInput.getText()) ||
                TextUtils.isEmpty(gramsInput.getText())) {
            showError("All fields marked with * are required");
            return false;
        }

        try {
            Double.parseDouble(priceInput.getText().toString());
            Integer.parseInt(quantityInput.getText().toString());
            Integer.parseInt(gramsInput.getText().toString());
        } catch (NumberFormatException e) {
            showError("Invalid number format");
            return false;
        }

        return true;
    }

    private void addProduct(View dialogView) {
        Product product = createProductFromDialog(dialogView);
        showLoading();

        if (selectedImageUri != null) {
            uploadProductWithImage(product);
        } else {
            addProductWithoutImage(product);
        }
    }

    private void updateProduct(View dialogView, Product product) {
        Product updatedProduct = createProductFromDialog(dialogView);
        updatedProduct.setId(product.getId());
        showLoading();

        if (selectedImageUri != null) {
            firebaseManager.uploadProductImage(selectedImageUri, product.getId())
                    .addOnSuccessListener(imageUrl -> {
                        updatedProduct.setProduct_image(imageUrl);
                        updateProductInDatabase(updatedProduct);
                    })
                    .addOnFailureListener(e -> handleError(e));
        } else {
            updatedProduct.setProduct_image(product.getProduct_image());
            updateProductInDatabase(updatedProduct);
        }
    }

    private Product createProductFromDialog(View dialogView) {
        String title = ((TextInputEditText) dialogView.findViewById(R.id.title_input))
                .getText().toString();
        double price = Double.parseDouble(((TextInputEditText) dialogView.findViewById(R.id.price_input))
                .getText().toString());
        String description = ((TextInputEditText) dialogView.findViewById(R.id.description_input))
                .getText().toString();
        int quantity = Integer.parseInt(((TextInputEditText) dialogView.findViewById(R.id.quantity_input))
                .getText().toString());
        int grams = Integer.parseInt(((TextInputEditText) dialogView.findViewById(R.id.grams_input))
                .getText().toString());
        String from = ((TextInputEditText) dialogView.findViewById(R.id.from_input))
                .getText().toString();

        return new Product(title, price, "", quantity, description, grams, "g", from);
    }

    private void uploadProductWithImage(Product product) {
        firebaseManager.addProduct(product)
                .addOnSuccessListener(documentReference -> {
                    String productId = documentReference.getId();
                    firebaseManager.uploadProductImage(selectedImageUri, productId)
                            .addOnSuccessListener(imageUrl -> {
                                hideLoading();
                                showSuccess("Product added successfully");
                                loadProducts();
                            })
                            .addOnFailureListener(e -> handleError(e));
                })
                .addOnFailureListener(e -> handleError(e));
    }

    private void addProductWithoutImage(Product product) {
        firebaseManager.addProduct(product)
                .addOnSuccessListener(documentReference -> {
                    hideLoading();
                    showSuccess("Product added successfully");
                    loadProducts();
                })
                .addOnFailureListener(e -> handleError(e));
    }

    private void updateProductInDatabase(Product product) {
        firebaseManager.updateProduct(product)
                .addOnSuccessListener(aVoid -> {
                    hideLoading();
                    showSuccess("Product updated successfully");
                    loadProducts();
                })
                .addOnFailureListener(e -> handleError(e));
    }

    private void deleteProduct(Product product) {
        firebaseManager.deleteProduct(product.getId())
                .addOnSuccessListener(aVoid -> {
                    hideLoading();
                    showSuccess("Product deleted successfully");
                    loadProducts(); // Refresh list
                })
                .addOnFailureListener(e -> {
                    hideLoading();
                    showError("Error deleting product: " + e.getMessage());
                });
    }

    private void handleError(Exception e) {
        hideLoading();
        showError("Error: " + e.getMessage());
    }

    private void showError(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    private void showSuccess(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    private void showLoading() {
        binding.progressIndicator.setVisibility(View.VISIBLE);
    }

    private void hideLoading() {
        binding.progressIndicator.setVisibility(View.GONE);
    }

    private void updateEmptyState() {
        binding.emptyView.setVisibility(products.isEmpty() ? View.VISIBLE : View.GONE);
        binding.productsRecyclerView.setVisibility(products.isEmpty() ? View.GONE : View.VISIBLE);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        currentDialogView = null;
    }
}