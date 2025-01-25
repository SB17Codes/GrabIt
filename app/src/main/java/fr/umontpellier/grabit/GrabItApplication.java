package fr.umontpellier.grabit;

import android.app.Application;
import com.google.firebase.FirebaseApp;

public class GrabItApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        // Initialize Firebase
        FirebaseApp.initializeApp(this);
        // Add other global initializations if needed
    }
}