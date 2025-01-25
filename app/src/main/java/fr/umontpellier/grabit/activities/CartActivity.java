package fr.umontpellier.grabit.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import fr.umontpellier.grabit.R;
import fr.umontpellier.grabit.adapters.CartAdapter;
import fr.umontpellier.grabit.databinding.ActivityCartBinding;
import fr.umontpellier.grabit.firebase.FirebaseManager;
import fr.umontpellier.grabit.models.Cart;
import fr.umontpellier.grabit.models.CartItem;
import fr.umontpellier.grabit.models.Order;
import fr.umontpellier.grabit.models.OrderItem;

public class CartActivity extends AppCompatActivity {
    private static final double SHIPPING_COST = 4.99;

    private ActivityCartBinding binding;
    private FirebaseManager firebaseManager;
    private CartAdapter cartAdapter;
    private List<CartItem> cartItems;
    private ValueEventListener cartListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityCartBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setupToolbar();
        initializeFirebase();
        setupRecyclerView();
        setupCartListener();
    }


    private void initializeFirebase() {
        firebaseManager = FirebaseManager.getInstance();
        if (firebaseManager == null || firebaseManager.getAuth().getCurrentUser() == null) {
            redirectToLogin();
        }
    }

    private void setupRecyclerView() {
        cartItems = new ArrayList<>();
        cartAdapter = new CartAdapter(this, cartItems, new CartAdapter.CartItemListener() {
            @Override
            public void onQuantityChanged(CartItem item, int quantity) {
                updateCartItemQuantity(item, quantity);
            }

            @Override
            public void onRemoveItem(CartItem item) {
                removeFromCart(item);
            }
        });

        binding.cartRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        binding.cartRecyclerView.setAdapter(cartAdapter);
    }

    private void setupCartListener() {
        String userId = firebaseManager.getAuth().getCurrentUser().getUid();
        cartListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Cart cart = snapshot.getValue(Cart.class);
                if (cart != null && cart.getItems() != null) {
                    updateCartUI(cart);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                showError("Error loading cart: " + error.getMessage());
            }
        };
        firebaseManager.addCartListener(userId, cartListener);

        binding.checkoutButton.setOnClickListener(v -> proceedToCheckout());
    }

    private void setupToolbar() {
        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setHomeButtonEnabled(true);
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    private void updateCartUI(Cart cart) {
        cartItems.clear();
        cartItems.addAll(cart.getItems().values());
        cartAdapter.notifyDataSetChanged();

        double subtotal = cart.getTotal();
        double total = subtotal + SHIPPING_COST;

        binding.subtotalText.setText(String.format("$%.2f", subtotal));
        binding.shippingText.setText(String.format("$%.2f", SHIPPING_COST));
        binding.totalText.setText(String.format("$%.2f", total));

        binding.checkoutButton.setEnabled(subtotal > 0);
    }

    private void updateCartItemQuantity(CartItem item, int quantity) {
        String userId = firebaseManager.getAuth().getCurrentUser().getUid();
        firebaseManager.updateCartItemQuantity(userId, item.getProductId(), quantity)
                .addOnFailureListener(e -> showError("Failed to update quantity"));
    }

    private void removeFromCart(CartItem item) {
        String userId = firebaseManager.getAuth().getCurrentUser().getUid();
        firebaseManager.removeFromCart(userId, item.getProductId())
                .addOnFailureListener(e -> showError("Failed to remove item"));
    }

    private void proceedToCheckout() {
        if (firebaseManager == null || firebaseManager.getAuth().getCurrentUser() == null) {
            showError("Authentication required");
            return;
        }

        String userId = firebaseManager.getAuth().getCurrentUser().getUid();

        // Get current cart
        firebaseManager.getUserCart(userId)
                .addOnSuccessListener(cart -> {
                    if (cart.isEmpty()) {
                        showError("Cart is empty");
                        return;
                    }

                    // Create new order from cart
                    Order order = new Order();
                    order.setUserId(userId);
                    order.setShippingCost(SHIPPING_COST);

                    // Convert cart items to order items
                    Map<String, OrderItem> orderItems = new HashMap<>();
                    for (CartItem cartItem : cart.getItems().values()) {
                        OrderItem orderItem = new OrderItem(cartItem);
                        orderItems.put(cartItem.getProductId(), orderItem);
                    }
                    order.setItems(orderItems);
                    order.calculateTotals();

                    // Create order in database
                    firebaseManager.createOrder(order)
                            .addOnSuccessListener(orderId -> {
                                Toast.makeText(this, "Order placed successfully", Toast.LENGTH_SHORT).show();
                                // Navigate to orders screen
                                startActivity(new Intent(this, OrdersActivity.class));
                                finish();
                            })
                            .addOnFailureListener(e -> {
                                showError("Failed to place order: " + e.getMessage());
                            });
                })
                .addOnFailureListener(e -> {
                    showError("Error loading cart: " + e.getMessage());
                });
    }
    private void showError(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (cartListener != null && firebaseManager != null &&
                firebaseManager.getAuth().getCurrentUser() != null) {
            firebaseManager.removeCartListener(
                    firebaseManager.getAuth().getCurrentUser().getUid(),
                    cartListener
            );
        }
    }
}