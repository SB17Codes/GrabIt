package fr.umontpellier.grabit.activities;

import android.os.Bundle;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.formatter.ValueFormatter;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import java.util.ArrayList;

import fr.umontpellier.grabit.R;
import fr.umontpellier.grabit.databinding.ActivityManagerDashboardBinding;
import fr.umontpellier.grabit.firebase.FirebaseManager;
import fr.umontpellier.grabit.models.Order;

public class ManagerDashboardActivity extends AppCompatActivity {
    private ActivityManagerDashboardBinding binding;
    private FirebaseManager firebaseManager;
    private LineChart salesChart;
    private BarChart ordersChart;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityManagerDashboardBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());


        firebaseManager = FirebaseManager.getInstance();
        initializeViews();
        loadStats();
        setupCharts();
    }

    private void initializeViews() {
        salesChart = findViewById(R.id.sales_chart);
        ordersChart = findViewById(R.id.orders_chart);
    }



    private void loadStats() {

        firebaseManager.getProductCount()
                .addOnSuccessListener(count -> {
                    Log.d("ManagerDashboard", "Product count: " + count);
                    binding.productsCount.setText(String.valueOf(count));
                })
                .addOnFailureListener(e ->
                        Log.e("ManagerDashboard", "Error loading product count", e));

        firebaseManager.getUserCount()
                .addOnSuccessListener(count -> {
                    Log.d("ManagerDashboard", "User count: " + count);
                    binding.usersCount.setText(String.valueOf(count));
                })
                .addOnFailureListener(e ->
                        Log.e("ManagerDashboard", "Error loading user count", e));

        firebaseManager.getTotalSales()
                .addOnSuccessListener(total ->
                        binding.salesAmount.setText(String.format("$%.2f", total)));

        firebaseManager.getOrderCount()
                .addOnSuccessListener(count ->
                        binding.ordersCount.setText(String.valueOf(count)));
    }

    private void setupCharts() {
        // Sales Chart setup
        salesChart.getDescription().setEnabled(false);
        salesChart.setTouchEnabled(true);
        salesChart.setDragEnabled(true);
        salesChart.setScaleEnabled(true);
        salesChart.setNoDataText("Loading sales data...");
        salesChart.setNoDataTextColor(getColor(R.color.onBackground));

        // Orders Chart setup
        ordersChart.getDescription().setEnabled(false);
        ordersChart.setTouchEnabled(true);
        ordersChart.setDragEnabled(true);
        ordersChart.setScaleEnabled(true);
        ordersChart.setNoDataText("Loading orders data...");
        ordersChart.setNoDataTextColor(getColor(R.color.onBackground));

        loadChartData();
    }



    private void loadChartData() {
        Log.d("ManagerDashboard", "Loading chart data");

        firebaseManager.getRecentOrders(7)
                .addOnSuccessListener(orders -> {
                    Log.d("ManagerDashboard", "Received orders: " + orders.size());

                    if (orders.isEmpty()) {
                        salesChart.setNoDataText("No sales data available");
                        ordersChart.setNoDataText("No orders data available");
                        return;
                    }

                    setupSalesChart(orders);
                    setupOrdersChart(orders);
                })
                .addOnFailureListener(e -> {
                    Log.e("ManagerDashboard", "Error loading chart data", e);
                    salesChart.setNoDataText("Error loading sales data");
                    ordersChart.setNoDataText("Error loading orders data");
                });
    }

    private void setupSalesChart(List<Order> orders) {
        ArrayList<Entry> salesEntries = new ArrayList<>();
        ArrayList<String> xLabels = new ArrayList<>();
        SimpleDateFormat dateFormat = new SimpleDateFormat("MM/dd", Locale.getDefault());

        for (int i = 0; i < orders.size(); i++) {
            Order order = orders.get(i);
            salesEntries.add(new Entry(i, (float) order.getTotal()));
            xLabels.add(dateFormat.format(order.getCreatedAt()));
        }

        LineDataSet salesDataSet = new LineDataSet(salesEntries, "Daily Sales");
        salesDataSet.setColor(getColor(R.color.primary));
        salesDataSet.setValueTextColor(getColor(R.color.onPrimary));
        salesDataSet.setLineWidth(2f);
        salesDataSet.setCircleRadius(4f);
        salesDataSet.setCircleColor(getColor(R.color.primary));
        salesDataSet.setDrawValues(true);
        salesDataSet.setValueTextSize(10f);
        salesDataSet.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return String.format("$%.0f", value);
            }
        });

        LineData lineData = new LineData(salesDataSet);

        XAxis xAxis = salesChart.getXAxis();
        xAxis.setTextColor(getColor(R.color.onPrimary));
        xAxis.setValueFormatter(new IndexAxisValueFormatter(xLabels));
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setGranularity(1f);
        xAxis.setLabelRotationAngle(-45);

        YAxis leftAxis = salesChart.getAxisLeft();
        leftAxis.setTextColor(getColor(R.color.onPrimary));
        leftAxis.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return String.format("$%.0f", value);
            }
        });

        salesChart.getAxisRight().setEnabled(false);
        salesChart.getDescription().setEnabled(false);
        salesChart.setData(lineData);
        salesChart.animateX(1000);
    }

    private void setupOrdersChart(List<Order> orders) {
        ArrayList<BarEntry> orderEntries = new ArrayList<>();
        ArrayList<String> xLabels = new ArrayList<>();
        SimpleDateFormat dateFormat = new SimpleDateFormat("MM/dd", Locale.getDefault());

        Map<String, Integer> dailyOrderCounts = new HashMap<>();

        // Count orders per day
        for (Order order : orders) {
            String date = dateFormat.format(order.getCreatedAt());
            dailyOrderCounts.put(date, dailyOrderCounts.getOrDefault(date, 0) + 1);
        }

        int i = 0;
        for (Map.Entry<String, Integer> entry : dailyOrderCounts.entrySet()) {
            orderEntries.add(new BarEntry(i, entry.getValue()));
            xLabels.add(entry.getKey());
            i++;
        }

        BarDataSet orderDataSet = new BarDataSet(orderEntries, "Daily Orders");
        orderDataSet.setColor(getColor(R.color.primary));
        orderDataSet.setValueTextColor(getColor(R.color.onPrimary));
        orderDataSet.setValueTextSize(10f);

        orderDataSet.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return String.format("%.0f", value);
            }
        });

        BarData barData = new BarData(orderDataSet);

        XAxis xAxis = ordersChart.getXAxis();
        xAxis.setValueFormatter(new IndexAxisValueFormatter(xLabels));
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setGranularity(1f);
        xAxis.setLabelRotationAngle(-45);

        YAxis leftAxis = ordersChart.getAxisLeft();
        leftAxis.setGranularity(1f);
        leftAxis.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return String.format("%.0f", value);
            }
        });

        ordersChart.getAxisRight().setEnabled(false);
        ordersChart.getDescription().setEnabled(false);
        ordersChart.setData(barData);
        ordersChart.animateY(1000);
    }
}