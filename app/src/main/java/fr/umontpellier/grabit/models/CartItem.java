package fr.umontpellier.grabit.models;

import com.google.firebase.database.Exclude;

public class CartItem {
    private String productId;
    private Product product;
    private int quantity;
    private double itemTotal;

    public CartItem() {
        this.quantity = 0;
        this.itemTotal = 0.0;
    }

    public CartItem(Product product, int quantity) {
        this.product = product;
        this.productId = product.getId();
        this.quantity = quantity;
        calculateItemTotal();
    }

    public String getProductId() {
        return productId;
    }

    public void setProductId(String productId) {
        this.productId = productId;
    }

    public Product getProduct() {
        return product;
    }

    public void setProduct(Product product) {
        this.product = product;
        if (product != null) {
            this.productId = product.getId();
            calculateItemTotal();
        }
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = Math.max(0, quantity);
        calculateItemTotal();
    }

    public double getItemTotal() {
        return itemTotal;
    }

    public void setItemTotal(double itemTotal) {
        this.itemTotal = itemTotal;
    }

    @Exclude
    private void calculateItemTotal() {
        if (product != null) {
            double price = product.isDiscountActive()
                    ? product.getDiscountedPrice()
                    : product.getPrice();
            itemTotal = price * quantity;
        }
    }

    @Exclude
    public boolean isValid() {
        return product != null && productId != null && quantity > 0;
    }
}