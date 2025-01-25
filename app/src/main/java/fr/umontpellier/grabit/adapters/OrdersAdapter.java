package fr.umontpellier.grabit.adapters;

import android.content.Context;
import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.chip.Chip;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import fr.umontpellier.grabit.R;
import fr.umontpellier.grabit.models.Order;
import fr.umontpellier.grabit.models.OrderItem;

// OrdersAdapter.java
public class OrdersAdapter extends RecyclerView.Adapter<OrdersAdapter.OrderViewHolder> {
    private Context context;
    private List<Order> orders;
    private SimpleDateFormat dateFormat;

    public OrdersAdapter(Context context, List<Order> orders) {
        this.context = context;
        this.orders = orders;
        this.dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
    }

    @NonNull
    @Override
    public OrderViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Inflate the user order layout
        View view = LayoutInflater.from(context).inflate(R.layout.item_user_order, parent, false);
        return new OrderViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull OrderViewHolder holder, int position) {
        Order order = orders.get(position);
        holder.bind(order);
    }

    @Override
    public int getItemCount() {
        return orders.size();
    }

    class OrderViewHolder extends RecyclerView.ViewHolder {
        private TextView orderId, orderDate, orderTotal;
        private Chip statusChip;
        private RecyclerView itemsRecycler;

        OrderViewHolder(View itemView) {
            super(itemView);
            orderId = itemView.findViewById(R.id.order_id);
            orderDate = itemView.findViewById(R.id.order_date);
            statusChip = itemView.findViewById(R.id.status_chip);
            itemsRecycler = itemView.findViewById(R.id.order_items_recycler);
            orderTotal = itemView.findViewById(R.id.order_total);
        }

        void bind(Order order) {
            orderId.setText(String.format("Order #%s", order.getId()));
            orderDate.setText(dateFormat.format(order.getCreatedAt()));
            orderTotal.setText(String.format("Total: $%.2f", order.getTotal()));

            setupStatusChip(order.getStatus());
            setupOrderItems(order.getItems());
        }

        private void setupStatusChip(String status) {
            String capitalizedStatus = status.substring(0, 1).toUpperCase() + status.substring(1);
            statusChip.setText(capitalizedStatus);
            int chipColor = getStatusColor(status);
            statusChip.setChipBackgroundColor(ColorStateList.valueOf(chipColor));
        }

        private int getStatusColor(String status) {
            switch (status.toLowerCase()) {
                case "pending":
                    return context.getColor(R.color.primary);
                case "processing":
                    return context.getColor(R.color.accent);
                case "shipped":
                    return context.getColor(R.color.primaryDark);
                case "delivered":
                    return context.getColor(R.color.surface);
                default:
                    return context.getColor(R.color.surface);
            }
        }

        private void setupOrderItems(Map<String, OrderItem> items) {
            if (items != null && !items.isEmpty()) {
                OrderItemsAdapter adapter = new OrderItemsAdapter(context, new ArrayList<>(items.values()));
                itemsRecycler.setLayoutManager(new LinearLayoutManager(context));
                itemsRecycler.setAdapter(adapter);
            } else {
                // Handle empty items (e.g., hide RecyclerView or show a message)
                itemsRecycler.setVisibility(View.GONE);
            }
        }
    }
}