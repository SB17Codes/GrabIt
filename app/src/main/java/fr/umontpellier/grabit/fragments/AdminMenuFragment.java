package fr.umontpellier.grabit.fragments;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import fr.umontpellier.grabit.R;
import fr.umontpellier.grabit.activities.AdminOrdersActivity;
import fr.umontpellier.grabit.activities.AdminProductListActivity;
import fr.umontpellier.grabit.activities.DiscountManagerActivity;
import fr.umontpellier.grabit.activities.LoginActivity;
import fr.umontpellier.grabit.activities.ManagerDashboardActivity;
import fr.umontpellier.grabit.activities.OrdersActivity;
import fr.umontpellier.grabit.firebase.FirebaseManager;
import fr.umontpellier.grabit.utils.SessionManager;

public class AdminMenuFragment extends Fragment {
    private enum Screen {
        DASHBOARD, PRODUCTS, OFFERS, ORDERS
    }

    private FloatingActionButton fabDashboard, fabProducts, fabOffers, fabOrders, fabLogout;
    private FirebaseManager firebaseManager;
    private SessionManager sessionManager;
    private Screen currentScreen = Screen.DASHBOARD;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_admin_menu, container, false);
        initializeViews(view);
        initializeManagers();
        setupClickListeners();
        return view;
    }

    private void initializeViews(View view) {
        fabDashboard = view.findViewById(R.id.fab_dashboard);
        fabProducts = view.findViewById(R.id.fab_products);
        fabOffers = view.findViewById(R.id.fab_offers);
        fabOrders = view.findViewById(R.id.fab_orders);
        fabLogout = view.findViewById(R.id.fab_logout);
    }

    private void initializeManagers() {
        firebaseManager = FirebaseManager.getInstance();
        sessionManager = new SessionManager(requireContext());
    }

    private void setupClickListeners() {
        fabDashboard.setOnClickListener(v -> navigateToDashboard());
        fabProducts.setOnClickListener(v -> navigateToProducts());
        fabOffers.setOnClickListener(v -> navigateToOffers());
        fabOrders.setOnClickListener(v -> navigateToOrders());
        fabLogout.setOnClickListener(v -> handleLogout());
    }

    @Override
    public void onResume() {
        super.onResume();
        updateCurrentScreen();
        updateFabStates();
    }

    private void updateCurrentScreen() {
        if (getActivity() instanceof ManagerDashboardActivity) {
            currentScreen = Screen.DASHBOARD;
        } else if (getActivity() instanceof AdminProductListActivity) {
            currentScreen = Screen.PRODUCTS;
        }
        else if (getActivity() instanceof DiscountManagerActivity) {
            currentScreen = Screen.OFFERS;
        } else if (getActivity() instanceof OrdersActivity) {
            currentScreen = Screen.ORDERS;
        }
    }

    private void updateFabStates() {
        int onPrimaryColor = getResources().getColor(R.color.onPrimary);
        int backgroundColor = getResources().getColor(R.color.background);
        int accentColor = getResources().getColor(R.color.accent);

        // Reset all FABs
        for (FloatingActionButton fab : new FloatingActionButton[]{fabDashboard, fabProducts, fabOffers, fabOrders}) {
            if (fab != null) {
                fab.setBackgroundTintList(ColorStateList.valueOf(onPrimaryColor));
                fab.setImageTintList(ColorStateList.valueOf(backgroundColor));
                fab.setEnabled(true);
            }
        }

        // Highlight active FAB
        FloatingActionButton activeFab = null;
        switch (currentScreen) {
            case DASHBOARD:
                activeFab = fabDashboard;
                break;
            case PRODUCTS:
                activeFab = fabProducts;
                break;
            case OFFERS:
                activeFab = fabOffers;
                break;
            case ORDERS:
                activeFab = fabOrders;
                break;
        }

        if (activeFab != null) {
            activeFab.setBackgroundTintList(ColorStateList.valueOf(accentColor));
            activeFab.setImageTintList(ColorStateList.valueOf(onPrimaryColor));
            activeFab.setEnabled(false);
        }
    }

    private void navigateToDashboard() {
        startActivity(new Intent(requireContext(), ManagerDashboardActivity.class));
        requireActivity().finish();
    }

    private void navigateToProducts() {
        startActivity(new Intent(requireContext(), AdminProductListActivity.class));
        requireActivity().finish();
    }

    private void navigateToOffers() {
        startActivity(new Intent(requireContext(), DiscountManagerActivity.class));
        requireActivity().finish();
    }

    private void navigateToOrders() {
        startActivity(new Intent(requireContext(), AdminOrdersActivity.class));
        requireActivity().finish();
    }

    private void handleLogout() {
        firebaseManager.getAuth().signOut();
        sessionManager.logout();
        startActivity(new Intent(requireContext(), LoginActivity.class));
        requireActivity().finish();
    }
}