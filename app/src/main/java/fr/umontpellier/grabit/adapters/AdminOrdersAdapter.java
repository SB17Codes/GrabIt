package fr.umontpellier.grabit.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButtonToggleGroup;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

import fr.umontpellier.grabit.R;
import fr.umontpellier.grabit.models.Order;

public class AdminOrdersAdapter extends RecyclerView.Adapter<AdminOrdersAdapter.OrderViewHolder> {

    private final Context context;
    private List<Order> orders;
    private final OnOrderStatusChangeListener statusChangeListener;
    private final SimpleDateFormat dateFormat;

    public interface OnOrderStatusChangeListener {
        void onOrderStatusChanged(Order order, String newStatus);
    }

    public AdminOrdersAdapter(Context context, List<Order> orders, OnOrderStatusChangeListener listener) {
        this.context = context;
        this.orders = orders;
        this.statusChangeListener = listener;
        this.dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
    }

    @NonNull
    @Override
    public OrderViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_admin_order, parent, false);
        return new OrderViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull OrderViewHolder holder, int position) {
        Order order = orders.get(position);

        holder.orderId.setText(String.format("Commande #%s", order.getId()));
        holder.orderDate.setText(dateFormat.format(order.getCreatedAt()));
        holder.customerId.setText(String.format("Client: %s", order.getUserId()));
        holder.orderTotal.setText(String.format(Locale.getDefault(), "%.2f€", order.getTotal()));

        // Configuration du groupe de boutons de statut
        setupStatusButtons(holder, order);
    }

    private void setupStatusButtons(OrderViewHolder holder, Order order) {
        // Réinitialiser le listener pour éviter les callbacks multiples
        holder.statusToggleGroup.removeOnButtonCheckedListener(holder.buttonCheckedListener);

        // Sélectionner le bon bouton selon le statut actuel
        int buttonId = getButtonIdForStatus(order.getStatus());
        if (buttonId != -1) {
            holder.statusToggleGroup.check(buttonId);
        }

        // Configurer le listener
        holder.buttonCheckedListener = (group, checkedId, isChecked) -> {
            if (isChecked) {
                String newStatus = getStatusForButtonId(checkedId);
                if (!newStatus.equals(order.getStatus())) {
                    statusChangeListener.onOrderStatusChanged(order, newStatus);
                }
            }
        };

        holder.statusToggleGroup.addOnButtonCheckedListener(holder.buttonCheckedListener);
    }

    private int getButtonIdForStatus(String status) {
        switch (status.toLowerCase()) {
            case "pending": return R.id.btn_pending;
            case "confirmed": return R.id.btn_confirmed;
            case "preparing": return R.id.btn_preparing;
            case "ready": return R.id.btn_ready;
            default: return -1;
        }
    }

    private String getStatusForButtonId(int buttonId) {
        if (buttonId == R.id.btn_pending) return "pending";
        if (buttonId == R.id.btn_confirmed) return "confirmed";
        if (buttonId == R.id.btn_preparing) return "preparing";
        if (buttonId == R.id.btn_ready) return "ready";
        return "pending";
    }

    @Override
    public int getItemCount() {
        return orders.size();
    }

    public void updateOrders(List<Order> newOrders) {
        this.orders = newOrders;
        notifyDataSetChanged();
    }

    static class OrderViewHolder extends RecyclerView.ViewHolder {
        TextView orderId;
        TextView orderDate;
        TextView customerId;
        TextView orderTotal;
        MaterialButtonToggleGroup statusToggleGroup;
        MaterialButtonToggleGroup.OnButtonCheckedListener buttonCheckedListener;

        OrderViewHolder(View itemView) {
            super(itemView);
            orderId = itemView.findViewById(R.id.order_id);
            orderDate = itemView.findViewById(R.id.order_date);
            customerId = itemView.findViewById(R.id.customer_id);
            orderTotal = itemView.findViewById(R.id.order_total);
            statusToggleGroup = itemView.findViewById(R.id.status_toggle_group);
        }
    }
}