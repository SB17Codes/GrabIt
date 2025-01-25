package fr.umontpellier.grabit.fragments;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.OptIn;
import androidx.fragment.app.Fragment;

import com.google.android.material.badge.ExperimentalBadgeUtils;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import fr.umontpellier.grabit.R;
import fr.umontpellier.grabit.activities.CartActivity;
import fr.umontpellier.grabit.activities.CustomerDashboardActivity;
import fr.umontpellier.grabit.activities.LoginActivity;
import fr.umontpellier.grabit.activities.OrdersActivity;
import fr.umontpellier.grabit.activities.ProfileActivity;
import fr.umontpellier.grabit.firebase.FirebaseManager;
import fr.umontpellier.grabit.models.Cart;
import fr.umontpellier.grabit.utils.SessionManager;
import com.google.android.material.badge.BadgeDrawable;
import com.google.android.material.badge.BadgeUtils;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;
import androidx.core.content.ContextCompat;

public class MenuFragment extends Fragment {
    private FloatingActionButton fabHome, fabProfile, fabCommands, fabLogout, fabCart;
    private FirebaseManager firebaseManager;
    private SessionManager sessionManager;

    private enum Screen {
        HOME, PROFILE, CART, ORDERS
    }

    private Screen currentScreen = Screen.HOME;

    @Override
    public void onResume() {
        super.onResume();
        updateCurrentScreen();
        updateFabStates();
    }

    private void updateCurrentScreen() {
        if (getActivity() instanceof CustomerDashboardActivity) {
            currentScreen = Screen.HOME;
        } else if (getActivity() instanceof CartActivity) {
            currentScreen = Screen.CART;
        } else if (getActivity() instanceof ProfileActivity) {
            currentScreen = Screen.PROFILE;
        } else if (getActivity() instanceof OrdersActivity) {
            currentScreen = Screen.ORDERS;
        }
    }

    private void updateFabStates() {
        // Reset all FABs
        fabHome.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.onPrimary)));
        fabCart.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.onPrimary)));
        fabProfile.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.onPrimary)));
        fabCommands.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.onPrimary)));
        fabCart.setImageTintList(ColorStateList.valueOf(getResources().getColor(R.color.background)));
        fabCommands.setImageTintList(ColorStateList.valueOf(getResources().getColor(R.color.background)));
        fabHome.setImageTintList(ColorStateList.valueOf(getResources().getColor(R.color.background)));
        fabProfile.setImageTintList(ColorStateList.valueOf(getResources().getColor(R.color.background)));

        // Disable and highlight current screen's FAB
        FloatingActionButton activeFab = null;
        switch (currentScreen) {
            case HOME:
                activeFab = fabHome;
                break;
            case CART:
                activeFab = fabCart;
                break;
            case PROFILE:
                activeFab = fabProfile;
                break;
            case ORDERS:
                activeFab = fabCommands;
                break;
        }

        if (activeFab != null) {
            activeFab.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.accent)));
            activeFab.setImageTintList(ColorStateList.valueOf(getResources().getColor(R.color.onPrimary)));
            activeFab.setEnabled(false);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_menu, container, false);

        initializeManagers();
        initializeViews(view);
        setupClickListeners();
        setupCartBadge();

        return view;
    }

    private void setupCartBadge() {
        String userId = firebaseManager.getAuth().getCurrentUser().getUid();
        ValueEventListener cartListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Cart cart = snapshot.getValue(Cart.class);
                if (cart != null) {
                    updateCartBadge(cart.getItems().size());
                } else {
                    updateCartBadge(0);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                updateCartBadge(0);
            }
        };
        firebaseManager.addCartListener(userId, cartListener);
    }

    @OptIn(markerClass = ExperimentalBadgeUtils.class)
    private void updateCartBadge(int itemCount) {
        if (getActivity() == null)
            return;

        BadgeDrawable badge = BadgeDrawable.create(requireContext());
        badge.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.primary));
        badge.setBadgeGravity(BadgeDrawable.TOP_END);
        badge.setNumber(itemCount);

        BadgeUtils.attachBadgeDrawable(badge, fabCart, null);
        badge.setVisible(itemCount > 0);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        ValueEventListener cartListener = null;
        if (cartListener != null && firebaseManager != null &&
                firebaseManager.getAuth().getCurrentUser() != null) {
            firebaseManager.removeCartListener(
                    firebaseManager.getAuth().getCurrentUser().getUid(),
                    cartListener);
        }
    }

    private void initializeManagers() {
        firebaseManager = FirebaseManager.getInstance();
        sessionManager = new SessionManager(requireContext());
    }

    private void initializeViews(View view) {
        fabHome = view.findViewById(R.id.fab_home);
        fabProfile = view.findViewById(R.id.fab_profile);
        fabCommands = view.findViewById(R.id.fab_commands);
        fabLogout = view.findViewById(R.id.fab_logout);
        fabCart = view.findViewById(R.id.fab_cart);
    }

    private void setupClickListeners() {
        fabHome.setOnClickListener(v -> navigateToHome());
        fabProfile.setOnClickListener(v -> navigateToProfile());
        fabCommands.setOnClickListener(v -> navigateToOrders());
        fabLogout.setOnClickListener(v -> handleLogout());
        fabCart.setOnClickListener(v -> navigateToCart());

    }

    private void navigateToHome() {
        startActivity(new Intent(requireContext(), CustomerDashboardActivity.class));
        requireActivity().finish();
    }

    private void navigateToProfile() {
        // TODO: Implement profile navigation
        startActivity(new Intent(requireContext(), ProfileActivity.class));
        requireActivity().finish();
    }

    private void navigateToOrders() {
        // TODO: Implement orders navigation
         startActivity(new Intent(requireContext(), OrdersActivity.class));
        requireActivity().finish();
    }

    private void navigateToCart() {
        startActivity(new Intent(requireContext(), CartActivity.class));
        requireActivity().finish();
    }

    private void handleLogout() {
        firebaseManager.getAuth().signOut();
        sessionManager.logout();
        startActivity(new Intent(requireContext(), LoginActivity.class));
        requireActivity().finish();
    }
}