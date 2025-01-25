// src/main/java/fr/umontpellier/grabit/activities/ProductActivity.java
package fr.umontpellier.grabit.activities;

import static java.lang.String.format;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Paint;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;

import fr.umontpellier.grabit.R;
import fr.umontpellier.grabit.databinding.ActivityProductBinding;
import fr.umontpellier.grabit.databinding.ActivityProductDiscountBinding;
import fr.umontpellier.grabit.firebase.FirebaseManager;
import fr.umontpellier.grabit.models.Product;

public class ProductActivity extends AppCompatActivity {
    private FirebaseManager firebaseManager;
    private Product currentProduct;
    private int selectedQuantity = 1;

    // View Binding
    private ActivityProductBinding binding;
    private ActivityProductDiscountBinding discountBinding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Layout will be set after loading the product

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        initializeFirebase();
        String productId = getIntent().getStringExtra("product_id");
        loadProduct(productId);
    }

    private void initializeFirebase() {
        firebaseManager = FirebaseManager.getInstance();
        if (firebaseManager == null || firebaseManager.getAuth().getCurrentUser() == null) {
            redirectToLogin();
        }
    }

    private void loadProduct(String productId) {
        if (productId == null) {
            showError("Invalid product");
            finish();
            return;
        }

        firebaseManager.getProductById(productId)
                .addOnSuccessListener(product -> {
                    if (product != null) {
                        currentProduct = product;
                        setAppropriateLayout(product.isDiscountActive());
                        setupQuantityControls();
                        displayProduct(currentProduct);
                    } else {
                        showError("Product not found");
                        finish();
                    }
                })
                .addOnFailureListener(e -> {
                    showError("Error loading product: " + e.getMessage());
                    finish();
                });
    }

    private void setAppropriateLayout(boolean hasDiscount) {
        if (hasDiscount) {
            discountBinding = ActivityProductDiscountBinding.inflate(getLayoutInflater());
            setContentView(discountBinding.getRoot());
            initializeDiscountedViews();
            Log.d("ProductActivity", "Loaded discounted layout with View Binding.");
        } else {
            binding = ActivityProductBinding.inflate(getLayoutInflater());
            setContentView(binding.getRoot());
            initializeRegularViews();
            Log.d("ProductActivity", "Loaded regular layout with View Binding.");
        }
    }


    private void initializeDiscountedViews() {
        // All views are accessible via discountBinding
        discountBinding.addToCartButton.setOnClickListener(v -> handleAddToCart());
        discountBinding.decreaseQuantity.setOnClickListener(v -> {
            if (selectedQuantity > 1) {
                selectedQuantity--;
                updateQuantityDisplay();
            }
        });
        discountBinding.increaseQuantity.setOnClickListener(v -> {
            if (currentProduct != null && selectedQuantity < currentProduct.getInventory_quantity()) {
                selectedQuantity++;
                updateQuantityDisplay();
            }
        });
        Log.d("ProductActivity", "Discounted layout views initialized via View Binding.");
    }

    private void initializeRegularViews() {
        // All views are accessible via binding
        binding.addToCartButton.setOnClickListener(v -> handleAddToCart());
        binding.decreaseQuantity.setOnClickListener(v -> {
            if (selectedQuantity > 1) {
                selectedQuantity--;
                updateQuantityDisplay();
            }
        });
        binding.increaseQuantity.setOnClickListener(v -> {
            if (currentProduct != null && selectedQuantity < currentProduct.getInventory_quantity()) {
                selectedQuantity++;
                updateQuantityDisplay();
            }
        });
        Log.d("ProductActivity", "Regular layout views initialized via View Binding.");
    }

    private void setupQuantityControls() {
        updateQuantityDisplay();
        Log.d("ProductActivity", "Quantity controls set up.");
    }

    private void updateQuantityDisplay() {
        if (binding != null) {
            binding.quantityText.setText(String.valueOf(selectedQuantity));
            binding.decreaseQuantity.setEnabled(selectedQuantity > 1);
            binding.increaseQuantity.setEnabled(currentProduct != null && selectedQuantity < currentProduct.getInventory_quantity());
        }

        if (discountBinding != null) {
            discountBinding.quantityText.setText(String.valueOf(selectedQuantity));
            discountBinding.decreaseQuantity.setEnabled(selectedQuantity > 1);
            discountBinding.increaseQuantity.setEnabled(currentProduct != null && selectedQuantity < currentProduct.getInventory_quantity());
        }
    }

    @SuppressLint("DefaultLocale")
    private void displayProduct(Product product) {
        if (binding != null) {
            // Regular Layout
            binding.productDetailTitle.setText(product.getTitle());
            binding.productDetailDescription.setText(product.getDescription());

            // Set Weight and Origin
            binding.productDetailWeight.setText(
                    product.getWeight() != null && !product.getWeight().isEmpty()
                            ? format("Weight: %s", product.getWeight())
                            : "Weight: N/A"
            );
            binding.productDetailFrom.setText(
                    product.getFrom() != null && !product.getFrom().isEmpty()
                            ? format("Origin: %s", product.getFrom())
                            : "Origin: N/A"
            );

            // Stock Status
            boolean hasInventory = product.getInventory_quantity() > 0;
            binding.availabilityChip.setText(hasInventory ? "In Stock" : "Out of Stock");
            binding.availabilityChip.setChipBackgroundColor(ColorStateList.valueOf(
                    getColor(hasInventory ? R.color.primary : R.color.error)
            ));
            binding.availabilityChip.setTextColor(getColor(R.color.onPrimary));

            // Load Image
            Glide.with(this)
                    .load(product.getProduct_image())
                    .placeholder(R.drawable.placeholder_image)
                    .into(binding.productDetailImage);

            // Set Price
            binding.productDetailPrice.setText(format("$%.2f", product.getPrice()));
            binding.productDetailPrice.setVisibility(View.VISIBLE);
        }

        if (discountBinding != null) {
            // Discounted Layout
            discountBinding.productDetailTitle.setText(product.getTitle());
            discountBinding.productDetailDescription.setText(product.getDescription());

            // Set Weight and Origin
            discountBinding.productDetailWeight.setText(
                    product.getWeight() != null && !product.getWeight().isEmpty()
                            ? format("Weight: %s", product.getWeight())
                            : "Weight: N/A"
            );
            discountBinding.productDetailFrom.setText(
                    product.getFrom() != null && !product.getFrom().isEmpty()
                            ? format("Origin: %s", product.getFrom())
                            : "Origin: N/A"
            );

            // Stock Status
            boolean hasInventory = product.getInventory_quantity() > 0;
            discountBinding.availabilityChip.setText(hasInventory ? "In Stock" : "Out of Stock");
            discountBinding.availabilityChip.setChipBackgroundColor(ColorStateList.valueOf(
                    getColor(hasInventory ? R.color.primary : R.color.error)
            ));
            discountBinding.availabilityChip.setTextColor(getColor(R.color.onPrimary));

            // Load Image
            Glide.with(this)
                    .load(product.getProduct_image())
                    .placeholder(R.drawable.placeholder_image)
                    .into(discountBinding.productDetailImage);

            // Set Discounted Price
            discountBinding.productDetailPrice.setText(format("$%.2f", product.getDiscountedPrice()));
            discountBinding.productDetailPrice.setVisibility(View.VISIBLE);

            // Set Original Price
            discountBinding.originalPrice.setText(format("$%.2f", product.getPrice()));
            discountBinding.originalPrice.setPaintFlags(discountBinding.originalPrice.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
            discountBinding.originalPrice.setVisibility(View.VISIBLE);

            // Set Discount Percentage
            discountBinding.discountText.setText(product.getDiscountDisplay());
            discountBinding.discountText.setVisibility(View.VISIBLE);

            // Set Discount Period with Null Check
            String discountEnd = product.getDiscountEndDate() != null ?
                    format("Ends on %1$td %1$tB %1$tY", product.getDiscountEndDate()) : "Discount End Date N/A";
            discountBinding.discountPeriod.setText(discountEnd);            discountBinding.discountPeriod.setText(format("Ends on %s", discountEnd));
            discountBinding.discountPeriod.setVisibility(View.VISIBLE);
        }

        Log.d("ProductActivity", "Product details populated.");
    }

    private void handleAddToCart() {
        if (currentProduct == null) return;

        if (binding != null) binding.addToCartButton.setEnabled(false);
        if (discountBinding != null) discountBinding.addToCartButton.setEnabled(false);

        String userId = firebaseManager.getAuth().getCurrentUser().getUid();

        firebaseManager.addToCart(userId, currentProduct, selectedQuantity)
                .addOnSuccessListener(unused -> {
                    Toast.makeText(this, "Added to cart", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    showError("Failed to add to cart: " + e.getMessage());
                    if (binding != null) binding.addToCartButton.setEnabled(true);
                    if (discountBinding != null) discountBinding.addToCartButton.setEnabled(true);
                });
    }

    private void showError(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
        Log.e("ProductActivity", message);
    }

    private void redirectToLogin() {
        startActivity(new Intent(this, LoginActivity.class));
        finish();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}