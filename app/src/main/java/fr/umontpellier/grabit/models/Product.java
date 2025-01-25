package fr.umontpellier.grabit.models;

import com.google.firebase.database.Exclude;
import java.io.Serializable;

public class Product implements Serializable {
    private String id;
    private String title;
    private double price;
    private String product_image;
    private int inventory_quantity;
    private String description;
    private int grams;
    private String weight;
    private String from;

    // Renamed to avoid overshadowing your method-based logic
    private boolean discountFlag;
    private double discountAmount;
    private String discountType; // "PERCENTAGE" or "FIXED"
    private long discountStartDate;
    private long discountEndDate;

    public Product() {
        // Default constructor required by Firebase
    }

    public Product(String title, double price, String product_image, int inventory_quantity,
                   String description, int grams, String weight, String from) {
        this();  // Ensure discount fields are initialized
        this.title = title;
        this.price = price;
        this.product_image = product_image;
        this.inventory_quantity = inventory_quantity;
        this.description = description;
        this.grams = grams;
        this.weight = weight;
        this.from = from;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public double getPrice() { return price; }
    public void setPrice(double price) { this.price = price; }

    public String getProduct_image() { return product_image; }
    public void setProduct_image(String product_image) { this.product_image = product_image; }

    public int getInventory_quantity() { return inventory_quantity; }
    public void setInventory_quantity(int inventory_quantity) { this.inventory_quantity = inventory_quantity; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public int getGrams() { return grams; }
    public void setGrams(int grams) { this.grams = grams; }

    public String getWeight() { return weight; }
    public void setWeight(String weight) { this.weight = weight; }

    public String getFrom() { return from; }
    public void setFrom(String from) { this.from = from; }

    // Boolean field stored in Firebase:
    public boolean isDiscountFlag() {
        return discountFlag;
    }
    public void setDiscountFlag(boolean discountFlag) {
        this.discountFlag = discountFlag;
    }



    public double getDiscountAmount() { return discountAmount; }
    public void setDiscountAmount(double discountAmount) { this.discountAmount = discountAmount; }

    public String getDiscountType() { return discountType; }
    public void setDiscountType(String discountType) { this.discountType = discountType; }

    public long getDiscountStartDate() { return discountStartDate; }
    public void setDiscountStartDate(long discountStartDate) { this.discountStartDate = discountStartDate; }

    public Long getDiscountEndDate() { return discountEndDate; }
    public void setDiscountEndDate(long discountEndDate) { this.discountEndDate = discountEndDate; }

    // Exclude from Firebase so it doesn't overwrite discountFlag
    @Exclude
    public boolean isDiscountValid() {
        long now = System.currentTimeMillis();
        return now >= discountStartDate && now <= discountEndDate;
    }

    // Combine discountFlag with date logic
    @Exclude
    public boolean isDiscountActive() {
        return discountFlag && isDiscountValid();
    }

    @Exclude
    public double getDiscountedPrice() {
        if (!isDiscountActive()) {
            return price;
        }
        if ("PERCENTAGE".equals(discountType)) {
            return price * (1 - (discountAmount / 100));
        } else { // FIXED
            return Math.max(0, price - discountAmount);
        }
    }

    @Exclude
    public double getTotalPrice(int quantity) {
        return getDiscountedPrice() * quantity;
    }

    @Exclude
    public String getDiscountDisplay() {
        if (!isDiscountActive()) {
            return null;
        }
        if ("PERCENTAGE".equals(discountType)) {
            return String.format("-%.0f%%", discountAmount);
        } else {
            return String.format("-$%.2f", discountAmount);
        }
    }



    @Override
    public String toString() {
        return "Product{" +
                "id='" + id + '\'' +
                ", title='" + title + '\'' +
                ", price=" + price +
                ", discountFlag=" + discountFlag +
                ", discountType=" + discountType +
                ", discountedPrice=" + getDiscountedPrice() +
                '}';
    }
}