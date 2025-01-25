// src/main/java/fr/umontpellier/grabit/adapters/ProductAdapter.java
package fr.umontpellier.grabit.adapters;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Paint;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.chip.Chip;

import java.util.List;

import fr.umontpellier.grabit.R;
import fr.umontpellier.grabit.models.Product;

public class ProductAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    public interface OnProductClickListener {
        void onProductClick(Product product);
    }

    private static final int VIEW_TYPE_DISCOUNTED = 1;
    private static final int VIEW_TYPE_REGULAR = 0;

    private Context context;
    private List<Product> products;
    private OnProductClickListener listener;

    public ProductAdapter(Context context, List<Product> products, OnProductClickListener listener) {
        this.context = context;
        this.products = products;
        this.listener = listener;
    }

    @Override
    public int getItemViewType(int position) {
        Product product = products.get(position);
        return product.isDiscountFlag() && product.isDiscountActive() ? VIEW_TYPE_DISCOUNTED : VIEW_TYPE_REGULAR;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view;
        if(viewType == VIEW_TYPE_DISCOUNTED){
            view = LayoutInflater.from(context).inflate(R.layout.product_item_discount, parent, false);
            return new DiscountedProductViewHolder(view);
        } else {
            view = LayoutInflater.from(context).inflate(R.layout.product_item, parent, false);
            return new RegularProductViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Product product = products.get(position);
        if(holder instanceof DiscountedProductViewHolder){
            ((DiscountedProductViewHolder) holder).bind(product, listener);
        } else if(holder instanceof RegularProductViewHolder){
            ((RegularProductViewHolder) holder).bind(product, listener);
        }
    }

    @Override
    public int getItemCount() {
        return products.size();
    }

    // ViewHolder for Discounted Products
    class DiscountedProductViewHolder extends RecyclerView.ViewHolder {
        ImageView productImage;
        TextView productTitle, productOriginalPrice, productPrice;
        Chip discountChip;

        public DiscountedProductViewHolder(@NonNull View itemView) {
            super(itemView);
            productImage = itemView.findViewById(R.id.product_image);
            productTitle = itemView.findViewById(R.id.product_title);
            productOriginalPrice = itemView.findViewById(R.id.product_original_price);
            productPrice = itemView.findViewById(R.id.product_price);
            discountChip = itemView.findViewById(R.id.discount_chip);
        }

        @SuppressLint("DefaultLocale")
        void bind(Product product, OnProductClickListener listener) {
            productTitle.setText(product.getTitle());

            // Load product image using Glide
            Glide.with(context)
                    .load(product.getProduct_image())
                    .placeholder(R.drawable.placeholder_image)
                    .into(productImage);

            // Show discounted price
            productPrice.setText(String.format("$%.2f", product.getDiscountedPrice()));

            // Show original price with strike-through
            productOriginalPrice.setText(String.format("$%.2f", product.getPrice()));
            productOriginalPrice.setPaintFlags(productOriginalPrice.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);

            // Show discount chip
            discountChip.setText(product.getDiscountDisplay());

            // Handle item click
            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onProductClick(product);
                }
            });
        }
    }

    // ViewHolder for Regular Products
    class RegularProductViewHolder extends RecyclerView.ViewHolder {
        ImageView productImage;
        TextView productTitle, productPrice;

        public RegularProductViewHolder(@NonNull View itemView) {
            super(itemView);
            productImage = itemView.findViewById(R.id.product_image);
            productTitle = itemView.findViewById(R.id.product_title);
            productPrice = itemView.findViewById(R.id.product_price);
        }

        void bind(Product product, OnProductClickListener listener) {
            productTitle.setText(product.getTitle());

            // Load product image using Glide
            Glide.with(context)
                    .load(product.getProduct_image())
                    .placeholder(R.drawable.placeholder_image)
                    .into(productImage);

            // Show regular price
            productPrice.setText(String.format("$%.2f", product.getPrice()));

            // Handle item click
            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onProductClick(product);
                }
            });
        }
    }
}