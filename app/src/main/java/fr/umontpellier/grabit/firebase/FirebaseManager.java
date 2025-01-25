package fr.umontpellier.grabit.firebase;

import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Logger;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.AggregateSource;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.WriteBatch;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import org.xml.sax.ErrorHandler;

import fr.umontpellier.grabit.models.Cart;
import fr.umontpellier.grabit.models.CartItem;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicMarkableReference;

import fr.umontpellier.grabit.models.Order;
import fr.umontpellier.grabit.models.Product;
import fr.umontpellier.grabit.models.User;

public class FirebaseManager {
    private static final String USERS_PATH = "users";
    private static final String PRODUCTS_COLLECTION = "products";
    private static final String DATABASE_URL = "https://grabit-f7fd0-default-rtdb.europe-west1.firebasedatabase.app";
    private static final Object PROFILE_IMAGES_PATH = "/user_images";
    private static final Object PRODUCT_IMAGES_PATH = "/product_images";
    private static final String CARTS_PATH = "carts";
    private static final String ORDERS_PATH = "orders";

    private static volatile FirebaseManager instance;
    private static boolean persistenceEnabled = false;

    private final FirebaseAuth mAuth;
    private final DatabaseReference mDatabase;
    private final FirebaseFirestore mFirestore;
    private final CollectionReference mProductsCollection;
    private final StorageReference mStorage = FirebaseStorage.getInstance().getReference();

    private FirebaseManager() {
        // Initialize Realtime Database only once
        if (!persistenceEnabled) {
            FirebaseDatabase.getInstance().setPersistenceEnabled(true);
            persistenceEnabled = true;
        }
        mAuth = FirebaseAuth.getInstance();
        FirebaseDatabase database = FirebaseDatabase.getInstance(DATABASE_URL);
        mDatabase = database.getReference();
        FirebaseDatabase.getInstance().setLogLevel(Logger.Level.DEBUG);

        // Initialize Firestore
        mFirestore = FirebaseFirestore.getInstance();
        mProductsCollection = mFirestore.collection(PRODUCTS_COLLECTION);
    }

    public static FirebaseManager getInstance() {
        if (instance == null) {
            synchronized (FirebaseManager.class) {
                if (instance == null) {
                    instance = new FirebaseManager();
                }
            }
        }
        return instance;
    }

    // Auth methods
    @NonNull
    public FirebaseAuth getAuth() {
        return mAuth;
    }

    // Database methods
    @NonNull
    public DatabaseReference getDatabase() {
        return mDatabase;
    }

    // User methods
    public Task<Void> createUserProfile(@NonNull User user) {
        validateUser(user);
        return mAuth.getCurrentUser().getIdToken(true) // Ensure token is fresh
                .continueWithTask(task -> {
                    if (!task.isSuccessful()) {
                        throw task.getException();
                    }
                    return mDatabase.child(USERS_PATH)
                            .child(user.getUid())
                            .setValue(user);
                })
                .continueWithTask(task -> {
                    if (!task.isSuccessful()) {
                        throw task.getException();
                    }
                    return initializeUserCart(user.getUid());
                });
    }

    public Task<Void> saveUserToDatabase(@NonNull User user) {
        validateUser(user);
        return mDatabase.child(USERS_PATH)
                .child(user.getUid())
                .setValue(user);
    }

    public Task<User> getCurrentUser() {
        FirebaseUser firebaseUser = mAuth.getCurrentUser();
        if (firebaseUser == null) {
            return Tasks.forException(new Exception("No authenticated user"));
        }
        return getUserByUid(firebaseUser.getUid());
    }

    public Task<User> getUserByUid(String uid) {
        validateUid(uid);
        return mDatabase.child(USERS_PATH)
                .child(uid)
                .get()
                .continueWith(task -> {
                    if (!task.isSuccessful()) {
                        throw task.getException();
                    }

                    if (!task.getResult().exists()) {
                        throw new Exception("User not found");
                    }

                    User user = task.getResult().getValue(User.class);
                    if (user == null) {
                        throw new Exception("Invalid user data");
                    }

                    user.setUid(uid);
                    return user;
                });
    }

    public Task<String> uploadProfileImage(Uri imageUri, String userId) {
        StorageReference imageRef = mStorage.child((String) PROFILE_IMAGES_PATH)
                .child(userId + ".jpg");

        return imageRef.putFile(imageUri)
                .continueWithTask(task -> {
                    if (!task.isSuccessful()) {
                        throw task.getException();
                    }
                    return imageRef.getDownloadUrl();
                })
                .continueWith(task -> {
                    if (!task.isSuccessful()) {
                        throw task.getException();
                    }
                    return task.getResult().toString();
                });
    }

    // Product methods
    public Task<List<Product>> getProducts() {
        return mProductsCollection
                .orderBy("title")
                .get()
                .continueWith(task -> {
                    List<Product> products = new ArrayList<>();
                    if (task.isSuccessful() && task.getResult() != null) {
                        for (DocumentSnapshot doc : task.getResult()) {
                            // Populate product list
                            Product product = doc.toObject(Product.class);
                            if (product != null && doc.getId() != null) {
                                product.setId(doc.getId());
                                products.add(product);
                            }
                        }
                    }
                    return products;
                });
    }

    public Task<Void> updateProduct(Product product) {
        return mProductsCollection.document(product.getId()).set(product);
    }

    public Task<Void> deleteProduct(String productId) {
        return mProductsCollection.document(productId).delete();
    }

    public Task<Product> getProductById(String productId) {
        return mProductsCollection
                .document(productId)
                .get()
                .continueWith(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        Product product = task.getResult().toObject(Product.class);
                        if (product != null) {
                            product.setId(task.getResult().getId());
                        }
                        return product;
                    }
                    return null;
                });
    }

    public Task<DocumentReference> addProduct(Product product) {
        return mFirestore.collection(PRODUCTS_COLLECTION).add(product);
    }

    public Task<String> uploadProductImage(Uri imageUri, String productId) {
        StorageReference imageRef = mStorage.child((String) PRODUCT_IMAGES_PATH)
                .child(productId + ".jpg");

        return imageRef.putFile(imageUri)
                .continueWithTask(task -> {
                    if (!task.isSuccessful()) {
                        throw task.getException();
                    }
                    return imageRef.getDownloadUrl();
                })
                .continueWithTask(task -> {
                    if (!task.isSuccessful()) {
                        throw task.getException();
                    }
                    String imageUrl = task.getResult().toString();
                    return mProductsCollection.document(productId)
                            .update("product_image", imageUrl)
                            .continueWith(updateTask -> {
                                if (!updateTask.isSuccessful()) {
                                    throw updateTask.getException();
                                }
                                return imageUrl;
                            });
                });
    }

    public Task<Void> migrateProductsWithDiscountFields() {
        return mProductsCollection.get()
                .continueWithTask(task -> {
                    if (!task.isSuccessful()) {
                        throw task.getException();
                    }

                    WriteBatch batch = mFirestore.batch();

                    for (DocumentSnapshot doc : task.getResult()) {
                        Product product = doc.toObject(Product.class);
                        if (product != null && doc.getId() != null) {
                            // Example migration logic
                            product.setId(doc.getId());
                            // Possibly add discount fields or transformations here
                            batch.set(mProductsCollection.document(doc.getId()), product);
                        }
                    }

                    return batch.commit();
                });
    }

    // Reference getters
    @NonNull
    public DatabaseReference getUsersReference() {
        return mDatabase.child(USERS_PATH);
    }

    @NonNull
    public DatabaseReference getUserReference(@NonNull String uid) {
        validateUid(uid);
        return getUsersReference().child(uid);
    }

    // Validation helpers
    private void validateUser(@NonNull User user) {
        if (user.getUid().isEmpty()) {
            throw new IllegalArgumentException("User UID cannot be null or empty");
        }
    }

    private void validateUid(@NonNull String uid) {
        if (uid.isEmpty()) {
            throw new IllegalArgumentException("UID cannot be empty");
        }
    }

    public Task<Void> addToCart(String userId, Product product, int quantity) {
        validateUid(userId);
        CartItem cartItem = new CartItem(product, quantity);
        return mDatabase.child(CARTS_PATH)
                .child(userId)
                .child("items")
                .child(product.getId())
                .setValue(cartItem)
                .continueWithTask(task -> updateCartTotal(userId));
    }

    public Task<Void> updateCartItemQuantity(String userId, String productId, int quantity) {
        validateUid(userId);
        return mDatabase.child(CARTS_PATH)
                .child(userId)
                .child("items")
                .child(productId)
                .child("quantity")
                .setValue(quantity)
                .continueWithTask(task -> updateCartTotal(userId));
    }

    public Task<Void> removeFromCart(String userId, String productId) {
        validateUid(userId);
        return mDatabase.child(CARTS_PATH)
                .child(userId)
                .child("items")
                .child(productId)
                .removeValue()
                .continueWithTask(task -> updateCartTotal(userId));
    }

    private Task<Void> updateCartTotal(String userId) {
        return getUserCart(userId)
                .continueWithTask(task -> {
                    if (!task.isSuccessful()) {
                        throw task.getException();
                    }
                    Cart cart = task.getResult();
                    if (cart == null) {
                        throw new Exception("Cart is null");
                    }
                    cart.calculateTotal();
                    return mDatabase.child(CARTS_PATH)
                            .child(userId)
                            .child("total")
                            .setValue(cart.getTotal());
                });
    }

    public Task<Double> getCartTotal(String userId) {
        validateUid(userId);
        return mDatabase.child(CARTS_PATH)
                .child(userId)
                .child("total")
                .get()
                .continueWith(task -> {
                    if (!task.isSuccessful() || !task.getResult().exists()) {
                        throw new Exception("Failed to retrieve cart total");
                    }
                    return task.getResult().getValue(Double.class);
                });
    }

    public Task<Boolean> isProductInCart(String userId, String productId) {
        validateUid(userId);
        return mDatabase.child(CARTS_PATH)
                .child(userId)
                .child("items")
                .child(productId)
                .get()
                .continueWith(task -> task.isSuccessful() && task.getResult().exists());
    }

    public Task<Integer> getCartItemQuantity(String userId, String productId) {
        validateUid(userId);
        return mDatabase.child(CARTS_PATH)
                .child(userId)
                .child("items")
                .child(productId)
                .child("quantity")
                .get()
                .continueWith(task -> {
                    if (!task.isSuccessful() || !task.getResult().exists()) {
                        throw new Exception("Failed to retrieve cart item quantity");
                    }
                    return task.getResult().getValue(Integer.class);
                });
    }

    public Task<Void> initializeUserCart(String userId) {
        Cart newCart = new Cart(userId);
        return mDatabase.child(CARTS_PATH)
                .child(userId)
                .setValue(newCart)
                .addOnFailureListener(e -> {
                    Log.e("FirebaseManager", "Error initializing cart: " + e.getMessage());
                    // If permission denied, try again after a short delay
                    if (e.getMessage().contains("Permission denied")) {
                        new Handler(Looper.getMainLooper()).postDelayed(() -> {
                            mDatabase.child(CARTS_PATH).child(userId).setValue(newCart);
                        }, 1000);
                    }
                });
    }

    public void addCartListener(String userId, ValueEventListener listener) {
        mDatabase.child(CARTS_PATH)
                .child(userId)
                .addValueEventListener(listener);
    }

    public void removeCartListener(String userId, ValueEventListener listener) {
        mDatabase.child(CARTS_PATH)
                .child(userId)
                .removeEventListener(listener);
    }

    public Task<Cart> getUserCart(String userId) {
        return mDatabase.child(CARTS_PATH)
                .child(userId)
                .get()
                .continueWithTask(task -> {
                    if (!task.isSuccessful() || !task.getResult().exists()) {
                        throw new Exception("Failed to retrieve cart data");
                    }
                    return Tasks.forResult(task.getResult().getValue(Cart.class));
                });
    }

    public Task<String> createOrder(Order order) {
        String orderId = mDatabase.child(ORDERS_PATH).push().getKey();
        if (orderId == null) {
            return Tasks.forException(new Exception("Failed to generate order ID"));
        }

        order.setId(orderId);
        return mDatabase.child(ORDERS_PATH)
                .child(orderId)
                .setValue(order)
                .continueWithTask(task -> {
                    if (!task.isSuccessful()) {
                        throw task.getException();
                    }
                    return clearCart(order.getUserId());
                })
                .continueWith(task -> orderId);
    }

    private Task<Void> clearCart(String userId) {
        validateUid(userId);
        return mDatabase.child(CARTS_PATH)
                .child(userId)
                .removeValue()
                .continueWithTask(task -> {
                    if (!task.isSuccessful()) {
                        throw task.getException();
                    }
                    // Reinitialize empty cart
                    return initializeUserCart(userId);
                });
    }

    public Task<List<Order>> getUserOrders(String userId) {
        Log.d("FirebaseManager", "Getting orders for user: " + userId);
        return mDatabase.child(ORDERS_PATH)
                .orderByChild("userId")
                .equalTo(userId)
                .get()
                .continueWith(task -> {
                    List<Order> orders = new ArrayList<>();
                    if (task.isSuccessful() && task.getResult() != null) {
                        for (DataSnapshot orderSnapshot : task.getResult().getChildren()) {
                            Order order = orderSnapshot.getValue(Order.class);
                            if (order != null) {
                                orders.add(order);
                            }
                        }
                    }
                    Log.d("FirebaseManager", "Returning orders: " + orders.size());
                    return orders;
                });
    }

    public Task<Long> getProductCount() {
        return mProductsCollection
                .count()
                .get(AggregateSource.SERVER)
                .continueWith(task -> {
                    if (!task.isSuccessful()) {
                        throw task.getException();
                    }
                    return task.getResult().getCount();
                });
    }

    public Task<Long> getUserCount() {
        Log.d("FirebaseManager", "Getting user count");
        return mDatabase.child(USERS_PATH)
                .get()
                .continueWith(task -> {
                    if (!task.isSuccessful()) {
                        throw task.getException();
                    }
                    DataSnapshot snapshot = task.getResult();
                    long count = snapshot.getChildrenCount();
                    Log.d("FirebaseManager", "User count: " + count);
                    return count;
                });
    }

    public Task<Double> getTotalSales() {
        return mDatabase.child(ORDERS_PATH).get()
                .continueWith(task -> {
                    if (!task.isSuccessful()) {
                        throw task.getException();
                    }
                    double total = 0;
                    for (DataSnapshot orderSnapshot : task.getResult().getChildren()) {
                        Order o = orderSnapshot.getValue(Order.class);
                        if (o != null) {
                            // Summation logic
                            Double totalPrice = o.getTotalPrice();
                            total += totalPrice != null ? totalPrice : 0;
                        }
                    }
                    return total;
                });
    }

    public Task<Long> getOrderCount() {
        return mDatabase.child(ORDERS_PATH).get()
                .continueWith(task -> task.getResult().getChildrenCount());
    }

    public Task<List<Order>> getRecentOrders(int limit) {
        return mDatabase.child(ORDERS_PATH)
                .orderByChild("createdAt")
                .limitToLast(limit)
                .get()
                .continueWith(task -> {
                    if (!task.isSuccessful()) {
                        throw task.getException();
                    }
                    List<Order> orders = new ArrayList<>();
                    for (DataSnapshot orderSnapshot : task.getResult().getChildren()) {
                        Order order = orderSnapshot.getValue(Order.class);
                        if (order != null) {
                            orders.add(order);
                        }
                    }
                    orders.sort((o1, o2) -> o1.getCreatedAt().compareTo(o2.getCreatedAt()));
                    return orders;
                });
    }

    public Task<List<Order>> getAllOrders() {
        return mDatabase.child(ORDERS_PATH)
                .orderByChild("createdAt")
                .get()
                .continueWith(task -> {
                    if (!task.isSuccessful()) {
                        throw task.getException();
                    }
                    List<Order> orders = new ArrayList<>();
                    for (DataSnapshot orderSnapshot : task.getResult().getChildren()) {
                        Order order = orderSnapshot.getValue(Order.class);
                        if (order != null) {
                            orders.add(order);
                        }
                    }
                    orders.sort((o1, o2) -> o2.getCreatedAt().compareTo(o1.getCreatedAt()));
                    return orders;
                });
    }

    public Task<List<Order>> getOrdersByStatus(String status) {
        return mDatabase.child(ORDERS_PATH)
                .orderByChild("status")
                .equalTo(status)
                .get()
                .continueWith(task -> {
                    if (!task.isSuccessful()) {
                        throw task.getException();
                    }
                    List<Order> orders = new ArrayList<>();
                    for (DataSnapshot orderSnapshot : task.getResult().getChildren()) {
                        Order order = orderSnapshot.getValue(Order.class);
                        if (order != null) {
                            orders.add(order);
                        }
                    }
                    orders.sort((o1, o2) -> o2.getCreatedAt().compareTo(o1.getCreatedAt()));
                    return orders;
                });
    }

    public Task<Void> updateOrderStatus(String orderId, String newStatus) {
        Log.d("OrdersAdapter", "Updating order status for orderId: " + orderId + " to newStatus: " + newStatus);

        Task<Void> task = mDatabase.child(ORDERS_PATH)
                .child(orderId)
                .child("status")
                .setValue(newStatus);

        task.addOnSuccessListener(aVoid -> {
            Log.d("OrdersAdapter", "Order status updated successfully for orderId: " + orderId);
        }).addOnFailureListener(e -> {
            Log.e("OrdersAdapter", "Failed to update order status for orderId: " + orderId, e);
        });

        return task;
    }

    public void addOrdersListener(ValueEventListener listener) {
        mDatabase.child(ORDERS_PATH)
                .orderByChild("createdAt")
                .addValueEventListener(listener);
    }

    public void removeOrdersListener(ValueEventListener listener) {
        mDatabase.child(ORDERS_PATH).removeEventListener(listener);
    }
}