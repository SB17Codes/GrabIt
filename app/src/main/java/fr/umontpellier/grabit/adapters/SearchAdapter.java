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

import java.util.List;

import fr.umontpellier.grabit.R;
import fr.umontpellier.grabit.models.Product;

public class SearchAdapter extends RecyclerView.Adapter<SearchAdapter.SearchViewHolder> {
    private Context context;
    private List<Product> searchResults;
    private ProductAdapter.OnProductClickListener listener;

    public SearchAdapter(Context context, List<Product> searchResults,
                         ProductAdapter.OnProductClickListener listener) {
        this.context = context;
        this.searchResults = searchResults;
        this.listener = listener;
    }

    @NonNull
    @Override
    public SearchViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.search_item, parent, false);
        return new SearchViewHolder(view);
    }

    @SuppressLint("DefaultLocale")
    @Override
    public void onBindViewHolder(@NonNull SearchViewHolder holder, int position) {
        Product product = searchResults.get(position);
        holder.titleText.setText(product.getTitle());
        holder.priceText.setText(String.format("$%.2f", product.getPrice()));

        Glide.with(context)
                .load(product.getProduct_image())
                .centerCrop()
                .into(holder.productImage);

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onProductClick(product);
            }
        });
    }

    @Override
    public int getItemCount() {
        return searchResults.size();
    }

    static class SearchViewHolder extends RecyclerView.ViewHolder {
        ImageView productImage;
        TextView titleText;
        TextView priceText;

        SearchViewHolder(View itemView) {
            super(itemView);
            productImage = itemView.findViewById(R.id.search_product_image);
            titleText = itemView.findViewById(R.id.search_product_title);
            priceText = itemView.findViewById(R.id.search_product_price);
        }
    }
}
