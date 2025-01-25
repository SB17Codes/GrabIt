package fr.umontpellier.grabit.adapters;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;

import java.util.List;

import fr.umontpellier.grabit.R;
import fr.umontpellier.grabit.models.CartItem;
import fr.umontpellier.grabit.models.Product;

public class CartAdapter extends RecyclerView.Adapter<CartAdapter.CartViewHolder> {
    private Context context;
    private List<CartItem> items;
    private CartItemListener listener;

    public interface CartItemListener {
        void onQuantityChanged(CartItem item, int quantity);

        void onRemoveItem(CartItem item);
    }

    public CartAdapter(Context context, List<CartItem> items, CartItemListener listener) {
        this.context = context;
        this.items = items;
        this.listener = listener;
    }

    @NonNull
    @Override
    public CartViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.cart_item, parent, false);
        return new CartViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CartViewHolder holder, int position) {
        CartItem item = items.get(position);
        holder.bind(item);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    class CartViewHolder extends RecyclerView.ViewHolder {
        private CartItem currentItem;

        private ImageView productImage;
        private TextView productTitle, productPrice, productWeight, quantityText;
        private MaterialButton decreaseButton, increaseButton, deleteButton; // Changed from ImageButton

        CartViewHolder(View itemView) {
            super(itemView);
            productImage = itemView.findViewById(R.id.product_image);
            productTitle = itemView.findViewById(R.id.product_title);
            productPrice = itemView.findViewById(R.id.product_price);
            productWeight = itemView.findViewById(R.id.product_weight);
            quantityText = itemView.findViewById(R.id.quantity_text);
            decreaseButton = itemView.findViewById(R.id.decrease_quantity);
            increaseButton = itemView.findViewById(R.id.increase_quantity);
            deleteButton = itemView.findViewById(R.id.delete_button);
        }

        void bind(CartItem item) {
            Product product = item.getProduct();

            productTitle.setText(product.getTitle());
            productPrice.setText(String.format("$%.2f", item.getItemTotal()));
            productWeight.setText(String.format("%dg", product.getGrams()));
            quantityText.setText(String.valueOf(item.getQuantity()));

            Glide.with(itemView.getContext())
                    .load(product.getProduct_image())
                    .placeholder(R.drawable.placeholder_image)
                    .into(productImage);

            decreaseButton.setOnClickListener(v -> {
                if (item.getQuantity() > 1) {
                    listener.onQuantityChanged(item, item.getQuantity() - 1);
                }
            });

            increaseButton.setOnClickListener(v -> {
                if (item.getQuantity() < product.getInventory_quantity()) {
                    listener.onQuantityChanged(item, item.getQuantity() + 1);
                }
            });

            deleteButton.setOnClickListener(v -> listener.onRemoveItem(item));
        }
    }
}