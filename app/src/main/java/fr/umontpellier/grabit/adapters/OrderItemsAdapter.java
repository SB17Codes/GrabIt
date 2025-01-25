// OrderItemsAdapter.java
package fr.umontpellier.grabit.adapters;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;

import java.util.List;

import fr.umontpellier.grabit.R;
import fr.umontpellier.grabit.models.OrderItem;
import fr.umontpellier.grabit.models.Product;

public class OrderItemsAdapter extends RecyclerView.Adapter<OrderItemsAdapter.OrderItemViewHolder> {
    private Context context;
    private List<OrderItem> items;
    private RequestOptions imageOptions;

    public OrderItemsAdapter(Context context, List<OrderItem> items) {
        this.context = context;
        this.items = items;
        this.imageOptions = new RequestOptions()
                .placeholder(R.drawable.placeholder_image)
                .centerCrop();
    }

    @NonNull
    @Override
    public OrderItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.order_item, parent, false);
        return new OrderItemViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull OrderItemViewHolder holder, int position) {
        OrderItem item = items.get(position);
        holder.bind(item);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    class OrderItemViewHolder extends RecyclerView.ViewHolder {
        private ImageView productImage;
        private TextView titleText, quantityText, priceText;

        OrderItemViewHolder(View itemView) {
            super(itemView);
            productImage = itemView.findViewById(R.id.product_image);
            titleText = itemView.findViewById(R.id.product_title);
            quantityText = itemView.findViewById(R.id.quantity_text);
            priceText = itemView.findViewById(R.id.product_price);
        }

        @SuppressLint("DefaultLocale")
        void bind(OrderItem item) {
            Product product = item.getProduct();
            titleText.setText(product.getTitle());
            quantityText.setText(String.format("x%d", item.getQuantity()));
            priceText.setText(String.format("$%.2f", item.getItemTotal()));

            Glide.with(context)
                    .load(product.getProduct_image())
                    .apply(imageOptions)
                    .into(productImage);
        }
    }
}