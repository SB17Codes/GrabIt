// OrderItem.java
package fr.umontpellier.grabit.models;

public class OrderItem {
    private String productId;
    private Product product;
    private int quantity;
    private double price;
    private double itemTotal;

    public OrderItem() {
    }

    public OrderItem(CartItem cartItem) {
        this.product = cartItem.getProduct();
        this.productId = cartItem.getProductId();
        this.quantity = cartItem.getQuantity();
        this.price = product.getPrice();
        this.itemTotal = price * quantity;
    }

    // Getters and setters
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
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
        calculateTotal();
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
        calculateTotal();
    }

    public double getItemTotal() {
        return itemTotal;
    }

    public void setItemTotal(double itemTotal) {
        this.itemTotal = itemTotal;
    }

    private void calculateTotal() {
        this.itemTotal = price * quantity;
    }
}