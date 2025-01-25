// Order.java
package fr.umontpellier.grabit.models;

import androidx.annotation.NonNull;

import com.google.firebase.database.Exclude;
import com.google.firebase.database.PropertyName;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class Order {
    private String id;
    private String userId;
    private Map<String, OrderItem> items;
    private double subtotal;
    private double shippingCost;
    private double total;
    private String status;  // "pending", "processing", "shipped", "delivered"
    private Date createdAt;
    private String shippingAddress;

    public Order() {
        this.items = new HashMap<>();
        this.createdAt = new Date();
        this.status = "pending";
    }

    // Getters and setters
    @PropertyName("id")
    public String getId() { return id; }
    @PropertyName("id")
    public void setId(String id) { this.id = id; }

    @PropertyName("userId")
    public String getUserId() { return userId; }
    @PropertyName("userId")
    public void setUserId(String userId) { this.userId = userId; }

    @PropertyName("items")
    public Map<String, OrderItem> getItems() { return items; }
    @PropertyName("items")

    public void setItems(Map<String, OrderItem> items) { this.items = items; }

    @PropertyName("subtotal")
    public double getSubtotal() { return subtotal; }
    @PropertyName("subtotal")
    public void setSubtotal(double subtotal) { this.subtotal = subtotal; }

    @PropertyName("shippingCost")
    public double getShippingCost() { return shippingCost; }
    @PropertyName("shippingCost")
    public void setShippingCost(double shippingCost) { this.shippingCost = shippingCost; }

    @PropertyName("total")
    public double getTotal() { return total; }
    @PropertyName("total")
    public void setTotal(double total) { this.total = total; }

    @PropertyName("status")
    public String getStatus() { return status; }
    @PropertyName("status")
    public void setStatus(String status) { this.status = status; }

    @PropertyName("createdAt")
    public Date getCreatedAt() { return createdAt; }
    @PropertyName("createdAt")
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }

    @PropertyName("shippingAddress")
    public String getShippingAddress() { return shippingAddress; }
    @PropertyName("shippingAddress")
    public void setShippingAddress(String shippingAddress) { this.shippingAddress = shippingAddress; }

    public void calculateTotals() {
        subtotal = items.values().stream()
                .mapToDouble(OrderItem::getItemTotal)
                .sum();
        total = subtotal + shippingCost;
    }

    @Exclude
    public boolean isValid() {
        return userId != null && items != null && !items.isEmpty();
    }

    // Add toString for debugging
    @NonNull
    @Override
    public String toString() {
        return "Order{" +
                "id='" + id + '\'' +
                ", userId='" + userId + '\'' +
                ", items=" + (items != null ? items.size() : 0) +
                ", total=" + total +
                '}';
    }

    public double getTotalPrice() {
        return total;
    }
}