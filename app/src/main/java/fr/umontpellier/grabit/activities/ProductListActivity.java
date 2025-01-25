package fr.umontpellier.grabit.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

import fr.umontpellier.grabit.R;
import fr.umontpellier.grabit.adapters.ProductAdapter;
import fr.umontpellier.grabit.models.Product;

// ProductListActivity.java
public class ProductListActivity extends AppCompatActivity implements ProductAdapter.OnProductClickListener {
    private RecyclerView recyclerView;
    private ProductAdapter adapter;
    private List<Product> products;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_product_list);

        recyclerView = findViewById(R.id.products_recycler_view);
        recyclerView.setLayoutManager(new GridLayoutManager(this, 2));
        products = new ArrayList<>();
        adapter = new ProductAdapter(this, products, this);
        recyclerView.setAdapter(adapter);

        db = FirebaseFirestore.getInstance();
        loadProducts();
    }

    private void loadProducts() {
        db.collection("products")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    products.clear();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        Product product = document.toObject(Product.class);
                        product.setId(document.getId());
                        products.add(product);
                    }
                    adapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Error loading products", Toast.LENGTH_SHORT).show());
    }

    @Override
    public void onProductClick(Product product) {
        Intent intent = new Intent(this, ProductActivity.class);
        intent.putExtra("product_id", product.getId());
        startActivity(intent);
    }
}