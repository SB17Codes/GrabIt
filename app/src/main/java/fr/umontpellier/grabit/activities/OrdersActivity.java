package fr.umontpellier.grabit.activities;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import java.util.ArrayList;
import java.util.List;

import fr.umontpellier.grabit.adapters.OrdersAdapter;
import fr.umontpellier.grabit.databinding.ActivityOrdersBinding;
import fr.umontpellier.grabit.firebase.FirebaseManager;
import fr.umontpellier.grabit.models.Order;

public class OrdersActivity extends AppCompatActivity {
    private ActivityOrdersBinding binding;
    private FirebaseManager firebaseManager;
    private OrdersAdapter ordersAdapter;
    private List<Order> orders;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityOrdersBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        initializeFirebase();
        setupRecyclerView();
        loadOrders();
    }

    private void initializeFirebase() {
        firebaseManager = FirebaseManager.getInstance();
        if (firebaseManager == null || firebaseManager.getAuth().getCurrentUser() == null) {
            redirectToLogin();
            return;
        }
    }

    private void setupRecyclerView() {
        orders = new ArrayList<>();
        ordersAdapter = new OrdersAdapter(this, orders);
        binding.ordersRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        binding.ordersRecyclerView.setAdapter(ordersAdapter);
    }

    @SuppressLint("NotifyDataSetChanged")
    private void loadOrders() {
        if (firebaseManager == null || firebaseManager.getAuth().getCurrentUser() == null) {
            Log.e("OrdersActivity", "Firebase or auth is null");
            return;
        }

        String userId = firebaseManager.getAuth().getCurrentUser().getUid();
        Log.d("OrdersActivity", "Loading orders for user: " + userId);

        firebaseManager.getUserOrders(userId)
                .addOnSuccessListener(orderList -> {
                    Log.d("OrdersActivity", "Loaded " + orderList.size() + " orders");
                    for (Order order : orderList) {
                        Log.d("OrdersActivity", "Order details: " + order.toString());
                    }
                    orders.clear();
                    orders.addAll(orderList);
                    ordersAdapter.notifyDataSetChanged();
                    updateEmptyState();
                })
                .addOnFailureListener(e -> {
                    Log.e("OrdersActivity", "Error loading orders", e);
                    Toast.makeText(this, "Error loading orders: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                    updateEmptyState();
                });
    }

    private void updateEmptyState() {
        binding.ordersRecyclerView.setVisibility(orders.isEmpty() ? View.GONE : View.VISIBLE);
        binding.emptyView.setVisibility(orders.isEmpty() ? View.VISIBLE : View.GONE);
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