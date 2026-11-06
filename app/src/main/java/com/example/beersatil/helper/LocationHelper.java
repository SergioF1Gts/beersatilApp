package com.example.beersatil.helper;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.tasks.CancellationTokenSource;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class LocationHelper {
    public static final String NO_LOCATION = "Ubicación no disponible";

    private final Context context;
    private final FusedLocationProviderClient fusedLocationClient;
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public interface LocationCallback {
        void onLocationResult(double lat, double lng, String address);
    }

    public LocationHelper(Context context) {
        this.context = context.getApplicationContext(); // evita fugas de la Activity
        this.fusedLocationClient = LocationServices.getFusedLocationProviderClient(context);
    }

    private boolean hasPermission() {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
    }

    /** Última ubicación conocida; si no existe, pide una ubicación nueva. */
    @SuppressLint("MissingPermission")
    public void getLastLocation(LocationCallback callback) {
        if (!hasPermission()) {
            callback.onLocationResult(0.0, 0.0, NO_LOCATION);
            return;
        }
        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(location -> {
                    if (location != null) resolve(location, callback);
                    else requestFreshLocation(callback);
                })
                .addOnFailureListener(e -> requestFreshLocation(callback));
    }

    @SuppressLint("MissingPermission")
    private void requestFreshLocation(LocationCallback callback) {
        fusedLocationClient
                .getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, new CancellationTokenSource().getToken())
                .addOnSuccessListener(location -> {
                    if (location != null) resolve(location, callback);
                    else callback.onLocationResult(0.0, 0.0, NO_LOCATION);
                })
                .addOnFailureListener(e -> callback.onLocationResult(0.0, 0.0, NO_LOCATION));
    }

    private void resolve(Location location, LocationCallback callback) {
        getAddressFromCoordinatesAsync(location.getLatitude(), location.getLongitude(), callback);
    }

    public void getAddressFromCoordinatesAsync(double lat, double lng, LocationCallback callback) {
        final String fallback = String.format(Locale.US, "Lat: %.5f, Lng: %.5f", lat, lng);

        if (!Geocoder.isPresent()) {
            callback.onLocationResult(lat, lng, fallback);
            return;
        }

        Geocoder geocoder = new Geocoder(context, Locale.getDefault());

        if (Build.VERSION.SDK_INT >= 33) {
            geocoder.getFromLocation(lat, lng, 1, new Geocoder.GeocodeListener() {
                @Override
                public void onGeocode(@NonNull List<Address> addresses) {
                    String text = buildAddress(addresses, fallback);
                    mainHandler.post(() -> callback.onLocationResult(lat, lng, text));
                }

                @Override
                public void onError(String errorMessage) {
                    mainHandler.post(() -> callback.onLocationResult(lat, lng, fallback));
                }
            });
        } else {
            executorService.execute(() -> {
                String text = fallback;
                try {
                    @SuppressWarnings("deprecation")
                    List<Address> list = geocoder.getFromLocation(lat, lng, 1);
                    text = buildAddress(list, fallback);
                } catch (Exception ignored) {
                    // sin internet o sin servicio de geocodificación: se usa el respaldo
                }
                final String result = text;
                mainHandler.post(() -> callback.onLocationResult(lat, lng, result));
            });
        }
    }

    /** Arma "calle número, barrio, ciudad". */
    private String buildAddress(List<Address> list, String fallback) {
        if (list == null || list.isEmpty()) return fallback;
        Address a = list.get(0);

        List<String> parts = new ArrayList<>();
        String street = a.getThoroughfare();
        if (!TextUtils.isEmpty(street)) {
            if (!TextUtils.isEmpty(a.getSubThoroughfare())) street += " " + a.getSubThoroughfare();
            parts.add(street);
        }
        String neighborhood = a.getSubLocality();
        String city = a.getLocality();
        if (!TextUtils.isEmpty(neighborhood) && !neighborhood.equals(city)) parts.add(neighborhood);
        if (!TextUtils.isEmpty(city)) parts.add(city);

        if (parts.isEmpty()) {
            String line = a.getAddressLine(0);
            return !TextUtils.isEmpty(line) ? line : fallback;
        }
        return TextUtils.join(", ", parts);
    }
    /** Pide una ubicación nueva; si no hay, usa la última conocida. */
    @SuppressLint("MissingPermission")
    public void getFreshLocation(LocationCallback callback) {
        if (!hasPermission()) {
            callback.onLocationResult(0.0, 0.0, NO_LOCATION);
            return;
        }
        fusedLocationClient
                .getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, new CancellationTokenSource().getToken())
                .addOnSuccessListener(location -> {
                    if (location != null) resolve(location, callback);
                    else getLastLocation(callback);
                })
                .addOnFailureListener(e -> getLastLocation(callback));
    }
}