package com.example.beersatil.helper;

import android.util.Log;
import androidx.annotation.NonNull;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import java.util.Map;

public class MyFirebaseMessagingService extends FirebaseMessagingService {
    private static final String TAG = "BeerSatilFCM";

    @Override
    public void onNewToken(@NonNull String token) {
        super.onNewToken(token);
        // Útil para enviar un mensaje de prueba desde la consola a este dispositivo
        Log.d(TAG, "Nuevo token FCM: " + token);
    }

    /** Se ejecuta cuando llega un push con la app abierta (en primer plano). */
    @Override
    public void onMessageReceived(@NonNull RemoteMessage message) {
        super.onMessageReceived(message);

        String title = "BeerSatil";
        String body = "";

        RemoteMessage.Notification n = message.getNotification();
        if (n != null) {
            if (n.getTitle() != null) title = n.getTitle();
            if (n.getBody() != null) body = n.getBody();
        } else {
            Map<String, String> data = message.getData();
            if (data.get("title") != null) title = data.get("title");
            if (data.get("body") != null) body = data.get("body");
        }

        new NotificationHelper(this).showTaskNotification(title, body);
    }
}