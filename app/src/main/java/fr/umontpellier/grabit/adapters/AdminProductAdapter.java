package fr.umontpellier.grabit.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;

import java.util.List;

import fr.umontpellier.grabit.R;
import fr.umontpellier.grabit.models.Product;

// AdminProductAdapter.java
public class AdminProductAdapter extends RecyclerView.Adapter<AdminProductAdapter.ProductViewHolder> {
    private Context context;
    private List<Product> products;
    private OnProductClickListener listener;

    public interface OnProductClickListener {
        void onModifyClick(Product product);
    }

    public AdminProductAdapter(Context context, List<Product> products, OnProductClickListener listener) {
        this.context = context;
        this.products = products;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ProductViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.product_list_item_admin, parent, false);
        return new ProductViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ProductViewHolder holder, int position) {
        Product product = products.get(position);
        holder.bind(product);
    }

    @Override
    public int getItemCount() {
        return products.size();
    }

    class ProductViewHolder extends RecyclerView.ViewHolder {
        private ImageView productImage;
        private TextView titleText, priceText, inventoryText;
        private MaterialButton modifyButton;

        ProductViewHolder(View itemView) {
            super(itemView);
            productImage = itemView.findViewById(R.id.product_image);
            titleText = itemView.findViewById(R.id.product_title);
            priceText = itemView.findViewById(R.id.product_price);
            inventoryText = itemView.findViewById(R.id.product_inventory);
            modifyButton = itemView.findViewById(R.id.modify_button);
        }

        void bind(Product product) {
            titleText.setText(product.getTitle());
            priceText.setText(String.format("$%.2f", product.getPrice()));
            inventoryText.setText(String.format("In stock: %d", product.getInventory_quantity()));

            Glide.with(context)
                .load(product.getProduct_image())
                .placeholder(R.drawable.placeholder_image)
                .into(productImage);

            modifyButton.setOnClickListener(v -> listener.onModifyClick(product));
        }
    }
}