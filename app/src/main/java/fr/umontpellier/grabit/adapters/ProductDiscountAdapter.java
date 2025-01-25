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
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.chip.Chip;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import fr.umontpellier.grabit.R;
import fr.umontpellier.grabit.models.Product;

public class ProductDiscountAdapter extends RecyclerView.Adapter<ProductDiscountAdapter.DiscountViewHolder> {
    private Context context;
    private List<Product> products;
    private OnProductClickListener listener;
    private SimpleDateFormat dateFormat;

    public interface OnProductClickListener {
        void onProductClick(Product product);
    }

    public ProductDiscountAdapter(Context context, List<Product> products, OnProductClickListener listener) {
        this.context = context;
        this.products = products;
        this.listener = listener;
        this.dateFormat = new SimpleDateFormat("MMM dd", Locale.getDefault());
    }

    @NonNull
    @Override
    public DiscountViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_product_discount, parent, false);
        return new DiscountViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull DiscountViewHolder holder, int position) {
        Product product = products.get(position);
        holder.bind(product);
    }

    @Override
    public int getItemCount() {
        return products.size();
    }

    class DiscountViewHolder extends RecyclerView.ViewHolder {
        private ImageView productImage;
        private TextView titleText, priceText, originalPriceText, discountPeriodText;
        private Chip discountChip;
        private MaterialCardView cardView;

        DiscountViewHolder(View itemView) {
            super(itemView);
            productImage = itemView.findViewById(R.id.product_image);
            titleText = itemView.findViewById(R.id.product_title);
            priceText = itemView.findViewById(R.id.product_price);
            originalPriceText = itemView.findViewById(R.id.original_price);
            discountChip = itemView.findViewById(R.id.discount_chip);
            discountPeriodText = itemView.findViewById(R.id.discount_period);
            cardView = itemView.findViewById(R.id.product_card);
        }

        void bind(Product product) {
            titleText.setText(product.getTitle());
            Glide.with(context)
                    .load(product.getProduct_image())
                    .placeholder(R.drawable.placeholder_image)
                    .into(productImage);

            if (product.isDiscountActive()) {
                priceText.setText(String.format("$%.2f", product.getDiscountedPrice()));
                originalPriceText.setText(String.format("$%.2f", product.getPrice()));
                originalPriceText.setVisibility(View.VISIBLE);
                discountChip.setText(product.getDiscountDisplay());
                discountChip.setVisibility(View.VISIBLE);

                String period = String.format("%s - %s",
                        dateFormat.format(new Date(product.getDiscountStartDate())),
                        dateFormat.format(new Date(product.getDiscountEndDate())));
                discountPeriodText.setText(period);
                discountPeriodText.setVisibility(View.VISIBLE);
            } else {
                priceText.setText(String.format("$%.2f", product.getPrice()));
                originalPriceText.setVisibility(View.GONE);
                discountChip.setVisibility(View.GONE);
                discountPeriodText.setVisibility(View.GONE);
            }

            cardView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onProductClick(product);
                }
            });
        }
    }
}