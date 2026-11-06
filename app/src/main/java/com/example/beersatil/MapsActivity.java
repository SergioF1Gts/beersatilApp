package com.example.beersatil;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.ViewGroup;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.beersatil.helper.AuthManager;
import com.example.beersatil.helper.FirestoreRepository;
import com.example.beersatil.helper.ImageUploader;
import com.example.beersatil.model.TaskItem;
import com.google.firebase.firestore.ListenerRegistration;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.List;

public class MapsActivity extends AppCompatActivity {
    private WebView webViewMap;
    private Button btnToggleLayer;
    private ListenerRegistration tasksListener;
    private AuthManager authManager;

    private boolean isSatellite = false;
    private boolean pageLoaded = false;
    private String pendingJson = null;   // último JSON de pines, por si la página aún no cargó

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        authManager = new AuthManager(this);
        if (!authManager.isLoggedIn()) {
            finish();
            return;
        }
        setContentView(R.layout.activity_maps);

        if (savedInstanceState != null) {
            isSatellite = savedInstanceState.getBoolean("satellite", false);
        }

        webViewMap = findViewById(R.id.webViewMap);
        btnToggleLayer = findViewById(R.id.btnToggleLayer);
        Button btnFitAll = findViewById(R.id.btnFitAll);

        setupWebView();
        updateToggleText();

        btnToggleLayer.setOnClickListener(v -> {
            isSatellite = !isSatellite;
            applyLayer();
        });

        btnFitAll.setOnClickListener(v -> {
            if (pageLoaded) webViewMap.evaluateJavascript("fitAll();", null);
        });

        webViewMap.loadUrl("file:///android_asset/map.html");
        loadTasksIntoMap();
    }

    private void setupWebView() {
        WebSettings s = webViewMap.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setAllowFileAccess(false);      // los assets siguen funcionando con file:///android_asset
        s.setAllowContentAccess(false);

        webViewMap.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                pageLoaded = true;
                applyLayer();
                if (pendingJson != null) pushMarkers(pendingJson);
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                Uri uri = request.getUrl();
                if ("file".equals(uri.getScheme())) return false;
                // Enlaces externos (p. ej. la atribución de Leaflet) se abren en el navegador
                try {
                    startActivity(new Intent(Intent.ACTION_VIEW, uri));
                } catch (Exception ignored) {}
                return true;
            }
        });
    }

    private void loadTasksIntoMap() {
        FirestoreRepository repository = new FirestoreRepository();
        // Mismo filtro por rol que la lista: admin ve todo, operario solo lo suyo
        tasksListener = repository.getTasksRealtime(
                authManager.isAdmin(), authManager.getCurrentUid(),
                new FirestoreRepository.OnTasksLoadedListener() {
                    @Override
                    public void onTasksLoaded(List<TaskItem> tasks) {
                        try {
                            JSONArray array = new JSONArray();
                            for (TaskItem t : tasks) {
                                if (t.getLatitude() == 0.0 && t.getLongitude() == 0.0) continue; // sin ubicación
                                JSONObject o = new JSONObject();
                                o.put("title", t.getTitle());
                                o.put("type", t.getType());
                                o.put("status", t.getStatus());
                                o.put("apiary", t.getApiary());
                                o.put("address", t.getAddress());
                                o.put("author", t.getAuthorEmail());
                                o.put("latitude", t.getLatitude());
                                o.put("longitude", t.getLongitude());
                                String img = t.getImageUrl();
                                if (img != null && img.startsWith("https://")) {
                                    o.put("image", ImageUploader.optimized(img, 400));
                                }
                                array.put(o);
                            }
                            pendingJson = array.toString();
                            if (pageLoaded) pushMarkers(pendingJson);
                        } catch (Exception e) {
                            Toast.makeText(MapsActivity.this,
                                    "No se pudieron preparar los pines", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onError(Exception e) {
                        Toast.makeText(MapsActivity.this,
                                "Error al cargar el mapa: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    /** JSONObject.quote() genera un literal de texto JavaScript con todo bien escapado. */
    private void pushMarkers(String json) {
        webViewMap.evaluateJavascript("addMarkers(" + JSONObject.quote(json) + ");", null);
    }

    private void applyLayer() {
        updateToggleText();
        if (pageLoaded) {
            webViewMap.evaluateJavascript(
                    "setLayer('" + (isSatellite ? "satellite" : "osm") + "');", null);
        }
    }

    private void updateToggleText() {
        btnToggleLayer.setText(isSatellite ? "Normal" : "Satélite");
    }

    @Override
    protected void onSaveInstanceState(Bundle out) {
        super.onSaveInstanceState(out);
        out.putBoolean("satellite", isSatellite);
    }

    @Override
    protected void onDestroy() {
        if (tasksListener != null) tasksListener.remove();
        if (webViewMap != null) {
            ViewGroup parent = (ViewGroup) webViewMap.getParent();
            if (parent != null) parent.removeView(webViewMap);
            webViewMap.destroy();
        }
        super.onDestroy();
    }
}