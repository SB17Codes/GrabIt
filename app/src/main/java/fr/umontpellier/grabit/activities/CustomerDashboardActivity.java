package fr.umontpellier.grabit.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.navigation.NavigationView;

import fr.umontpellier.grabit.R;
import fr.umontpellier.grabit.adapters.ProductAdapter;
import fr.umontpellier.grabit.adapters.SearchAdapter;
import fr.umontpellier.grabit.databinding.ActivityCustomerDashboardBinding;
import fr.umontpellier.grabit.firebase.FirebaseManager;
import fr.umontpellier.grabit.models.Product;
import fr.umontpellier.grabit.utils.SessionManager;

import java.util.ArrayList;
import java.util.List;

public class CustomerDashboardActivity extends AppCompatActivity
        implements ProductAdapter.OnProductClickListener {

    private ActivityCustomerDashboardBinding binding;
    private FirebaseManager firebaseManager;
    private ActionBarDrawerToggle drawerToggle;
    private ArrayList<Product> products;
    private ProductAdapter productAdapter;
    private ProgressBar progressBar;
    private SearchAdapter searchAdapter;
    private List<Product> searchResults;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityCustomerDashboardBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        SessionManager sessionManager = new SessionManager(this);

        firebaseManager = FirebaseManager.getInstance();

        // Check if user is logged in
        if (!sessionManager.isLoggedIn()) {
            System.out.println("User not logged in - From CustomerDashboard" );
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        // Initialize discounts
        setupProductsRecyclerView();
        loadProducts();
        setupSearch();
    }


    private void setupSearch() {
        searchResults = new ArrayList<>();
        searchAdapter = new SearchAdapter(this, searchResults, this);

        binding.searchResultsList.setLayoutManager(new LinearLayoutManager(this));
        binding.searchResultsList.setAdapter(searchAdapter);
        binding.searchResultsList.setVisibility(View.GONE);

        binding.searchInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterProducts(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        binding.searchInput.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus && !searchResults.isEmpty()) {
                showSearchResults();
            }
        });

        // Hide results when clicking outside
        binding.root.setOnClickListener(v -> {
            binding.searchInput.clearFocus();
            hideSearchResults();
        });
    }

    private void filterProducts(String query) {
        searchResults.clear();

        if (!query.isEmpty()) {
            String searchQuery = query.toLowerCase();
            for (Product product : products) {
                if (product.getTitle().toLowerCase().contains(searchQuery)) {
                    searchResults.add(product);
                }
            }
            showSearchResults();
        } else {
            hideSearchResults();
        }

        searchAdapter.notifyDataSetChanged();
    }

    private void showSearchResults() {
        if (!searchResults.isEmpty()) {
            binding.searchResultsList.setVisibility(View.VISIBLE);
        }
    }

    private void hideSearchResults() {
        binding.searchResultsList.setVisibility(View.GONE);
    }

    @Override
    public void onProductClick(Product product) {
        hideSearchResults();
        binding.searchInput.clearFocus();
        Intent intent = new Intent(this, ProductActivity.class);
        intent.putExtra("product_id", product.getId());
        startActivity(intent);
    }

    private void setupProductsRecyclerView() {
        products = new ArrayList<>();
        productAdapter = new ProductAdapter(this, products, this);
        binding.productsRecyclerView.setLayoutManager(new GridLayoutManager(this, 2));
        binding.productsRecyclerView.setAdapter(productAdapter);

        // Setup progress bar
        progressBar = binding.progressBar;

    }

    private void loadProducts() {
        progressBar.setVisibility(View.VISIBLE);
        binding.productsRecyclerView.setVisibility(View.GONE);

        firebaseManager.getProducts()
                .addOnSuccessListener(productList -> {
                    products.clear();
                    products.addAll(productList);
                    productAdapter.notifyDataSetChanged();
                    progressBar.setVisibility(View.GONE);
                    binding.productsRecyclerView.setVisibility(View.VISIBLE);
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "Error loading products: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
    }





    @Override
    public void onBackPressed() {
        if (binding.drawerLayout.isDrawerOpen(GravityCompat.START)) {
            binding.drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadProducts(); // Refresh products when returning to screen
    }
}