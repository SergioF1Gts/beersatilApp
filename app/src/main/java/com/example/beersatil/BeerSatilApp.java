package com.example.beersatil;

import android.app.Application;
import android.util.Log;
import com.example.beersatil.helper.NotificationHelper;
import com.google.firebase.FirebaseApp;
import com.google.firebase.messaging.FirebaseMessaging;

public class BeerSatilApp extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        if (FirebaseApp.getApps(this).isEmpty()) {
            FirebaseApp.initializeApp(this);
        }

        // Crea el canal desde el inicio, para que también existan los push con la app cerrada
        new NotificationHelper(this);

        // Muestra en Logcat el token del dispositivo, para las pruebas desde la consola
        FirebaseMessaging.getInstance().getToken().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Log.d("BeerSatilFCM", "Token FCM: " + task.getResult());
            }
        });
    }
}