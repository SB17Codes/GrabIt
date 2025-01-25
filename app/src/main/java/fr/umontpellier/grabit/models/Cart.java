// Cart.java
package fr.umontpellier.grabit.models;

import com.google.firebase.database.Exclude;
import java.util.HashMap;
import java.util.Map;

public class Cart {
    private String userId;
    private Map<String, CartItem> items;
    private double total;
    private long lastUpdated;

    public Cart() {
        this.items = new HashMap<>();
        this.total = 0.0;
        this.lastUpdated = System.currentTimeMillis();
    }

    public Cart(String userId) {
        this();
        this.userId = userId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public Map<String, CartItem> getItems() {
        return items != null ? items : new HashMap<>();
    }

    public void setItems(Map<String, CartItem> items) {
        this.items = items != null ? items : new HashMap<>();
        calculateTotal();
    }

    public double getTotal() {
        return total;
    }

    public void setTotal(double total) {
        this.total = total;
    }

    public long getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(long lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

    @Exclude
    public void addItem(Product product, int quantity) {
        String productId = product.getId();
        if (items.containsKey(productId)) {
            CartItem existing = items.get(productId);
            existing.setQuantity(existing.getQuantity() + quantity);
        } else {
            items.put(productId, new CartItem(product, quantity));
        }
        calculateTotal();
        lastUpdated = System.currentTimeMillis();
    }

    @Exclude
    public void updateItemQuantity(String productId, int quantity) {
        if (items.containsKey(productId)) {
            CartItem item = items.get(productId);
            item.setQuantity(quantity);
            if (quantity <= 0) {
                items.remove(productId);
            }
            calculateTotal();
            lastUpdated = System.currentTimeMillis();
        }
    }

    @Exclude
    public void removeItem(String productId) {
        items.remove(productId);
        calculateTotal();
        lastUpdated = System.currentTimeMillis();
    }

    @Exclude
    public void calculateTotal() {
        total = items.values().stream()
                .mapToDouble(item -> {
                    double price = item.getProduct().isDiscountActive()
                            ? item.getProduct().getDiscountedPrice()
                            : item.getProduct().getPrice();
                    return price * item.getQuantity();
                })
                .sum();
    }

    @Exclude
    public boolean isEmpty() {
        return items.isEmpty();
    }

    @Exclude
    public int getItemCount() {
        return items.values().stream()
                .mapToInt(CartItem::getQuantity)
                .sum();
    }
}