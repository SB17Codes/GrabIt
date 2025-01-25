package fr.umontpellier.grabit.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

import fr.umontpellier.grabit.R;
import fr.umontpellier.grabit.adapters.AdminOrdersAdapter;
import fr.umontpellier.grabit.databinding.ActivityAdminOrdersBinding;
import fr.umontpellier.grabit.firebase.FirebaseManager;
import fr.umontpellier.grabit.models.Order;

public class AdminOrdersActivity extends AppCompatActivity implements AdminOrdersAdapter.OnOrderStatusChangeListener {
    private ActivityAdminOrdersBinding binding;
    private FirebaseManager firebaseManager;
    private AdminOrdersAdapter adapter;
    private List<Order> allOrders;
    private ValueEventListener ordersListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAdminOrdersBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        firebaseManager = FirebaseManager.getInstance();
        setupToolbar();
        setupRecyclerView();
        setupChipGroup();
        setupSwipeRefresh();
        setupOrdersListener();
    }

    private void setupToolbar() {
        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Gestion des Commandes");
        }
    }

    private void setupRecyclerView() {
        allOrders = new ArrayList<>();
        adapter = new AdminOrdersAdapter(this, allOrders, this);
        binding.ordersRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        binding.ordersRecyclerView.setAdapter(adapter);
    }

    private void setupChipGroup() {
        binding.statusFilterGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == View.NO_ID) return;
            
            String status = "ALL";
            if (checkedId == R.id.chip_pending) status = "pending";
            else if (checkedId == R.id.chip_confirmed) status = "confirmed";
            else if (checkedId == R.id.chip_preparing) status = "preparing";
            else if (checkedId == R.id.chip_ready) status = "ready";
            
            filterOrders(status);
        });
    }

    private void setupSwipeRefresh() {
        binding.swipeRefresh.setOnRefreshListener(this::refreshOrders);
    }

    private void setupOrdersListener() {
        showLoading();
        
        ordersListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                hideLoading();
                allOrders.clear();
                
                for (DataSnapshot orderSnapshot : snapshot.getChildren()) {
                    Order order = orderSnapshot.getValue(Order.class);
                    if (order != null) {
                        order.setId(orderSnapshot.getKey());
                        allOrders.add(order);
                    }
                }
                
                updateOrdersList();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                hideLoading();
                Toast.makeText(AdminOrdersActivity.this, 
                    "Erreur: " + error.getMessage(), 
                    Toast.LENGTH_SHORT).show();
            }
        };

        firebaseManager.addOrdersListener(ordersListener);
    }

    private void filterOrders(String status) {
        if (status.equals("ALL")) {
            updateOrdersList();
            return;
        }

        List<Order> filteredOrders = new ArrayList<>();
        for (Order order : allOrders) {
            if (order.getStatus().equals(status)) {
                filteredOrders.add(order);
            }
        }
        
        adapter.updateOrders(filteredOrders);
        updateEmptyView(filteredOrders.isEmpty());
    }

    private void refreshOrders() {
        firebaseManager.getAllOrders()
                .addOnSuccessListener(orders -> {
                    binding.swipeRefresh.setRefreshing(false);
                    allOrders = orders;
                    updateOrdersList();
                })
                .addOnFailureListener(e -> {
                    binding.swipeRefresh.setRefreshing(false);
                    Toast.makeText(this, "Erreur lors du rafraîchissement", Toast.LENGTH_SHORT).show();
                });
    }

    private void updateOrdersList() {
        adapter.updateOrders(allOrders);
        updateEmptyView(allOrders.isEmpty());
    }

    private void updateEmptyView(boolean isEmpty) {
        binding.emptyView.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        binding.ordersRecyclerView.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
    }

    private void showLoading() {
        binding.progressBar.setVisibility(View.VISIBLE);
        binding.ordersRecyclerView.setVisibility(View.GONE);
        binding.emptyView.setVisibility(View.GONE);
    }

    private void hideLoading() {
        binding.progressBar.setVisibility(View.GONE);
        binding.ordersRecyclerView.setVisibility(View.VISIBLE);
    }

    @Override
    public void onOrderStatusChanged(Order order, String newStatus) {
        firebaseManager.updateOrderStatus(order.getId(), newStatus)
                .addOnSuccessListener(aVoid -> 
                    Toast.makeText(this, "Statut mis à jour", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> 
                    Toast.makeText(this, "Erreur lors de la mise à jour", Toast.LENGTH_SHORT).show());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (ordersListener != null) {
            firebaseManager.removeOrdersListener(ordersListener);
        }
    }
} 