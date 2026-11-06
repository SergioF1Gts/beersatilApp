package com.example.beersatil;

import android.Manifest;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import com.bumptech.glide.Glide;
import com.example.beersatil.helper.AuthManager;
import com.example.beersatil.helper.FirestoreRepository;
import com.example.beersatil.helper.ImageUploader;
import com.example.beersatil.helper.LocationHelper;
import com.example.beersatil.helper.NotificationHelper;
import com.example.beersatil.model.TaskItem;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import java.io.File;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class AddTaskActivity extends AppCompatActivity {
    private static final String[] TYPES = {"Revisión de colmena", "Alimentación",
            "Cosecha de miel", "Plaga/Incidente", "Tratamiento sanitario"};
    private static final String[] STATUSES = {"Pendiente", "En proceso", "Resuelta"};
    private static final String NO_LOCATION = "Ubicación no disponible";

    private TextInputLayout tilTitle, tilDesc;
    private TextInputEditText etTitle, etDesc, etApiary;
    private Spinner spinnerType, spinnerStatus;
    private TextView textLocationInfo;
    private ImageView imageEvidencePreview;
    private ProgressBar progressBarUpload;
    private Button btnSaveTask;

    private Uri selectedImageUri;   // foto nueva elegida (aún sin subir)
    private Uri cameraUri;          // destino de la cámara
    private double latitude = 0.0;  // 0,0 = sin ubicación
    private double longitude = 0.0;
    private String currentAddress = NO_LOCATION;
    private String editingTaskId = null;
    private String existingImageUrl = "";

    private LocationHelper locationHelper;
    private NotificationHelper notificationHelper;
    private AuthManager authManager;
    private FirestoreRepository firestoreRepository;

    private ActivityResultLauncher<String> galleryLauncher;
    private ActivityResultLauncher<Uri> cameraLauncher;
    private ActivityResultLauncher<String> cameraPermissionLauncher;
    private ActivityResultLauncher<String[]> locationPermissionLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_task);

        locationHelper = new LocationHelper(this);
        notificationHelper = new NotificationHelper(this);
        authManager = new AuthManager(this);
        firestoreRepository = new FirestoreRepository();

        tilTitle = findViewById(R.id.tilTitle);
        tilDesc = findViewById(R.id.tilDesc);
        etTitle = findViewById(R.id.etTitle);
        etDesc = findViewById(R.id.etDesc);
        etApiary = findViewById(R.id.etApiary);
        spinnerType = findViewById(R.id.spinnerType);
        spinnerStatus = findViewById(R.id.spinnerStatus);
        textLocationInfo = findViewById(R.id.textLocationInfo);
        imageEvidencePreview = findViewById(R.id.imageEvidencePreview);
        progressBarUpload = findViewById(R.id.progressBarUpload);
        Button btnSelectPhoto = findViewById(R.id.btnSelectGallery);
        Button btnRefreshGps = findViewById(R.id.btnRefreshGps);
        btnSaveTask = findViewById(R.id.btnSaveTask);

        spinnerType.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, TYPES));
        spinnerStatus.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, STATUSES));

        registerLaunchers();

        // Modo edición
        if (getIntent() != null && getIntent().hasExtra("task_id")) {
            editingTaskId = getIntent().getStringExtra("task_id");
            etTitle.setText(getIntent().getStringExtra("task_title"));
            etDesc.setText(getIntent().getStringExtra("task_desc"));
            etApiary.setText(getIntent().getStringExtra("task_apiary"));
            String img = getIntent().getStringExtra("task_image");
            existingImageUrl = img != null ? img : "";
            latitude = getIntent().getDoubleExtra("task_lat", 0.0);
            longitude = getIntent().getDoubleExtra("task_lng", 0.0);
            String addr = getIntent().getStringExtra("task_address");
            currentAddress = addr != null && !addr.isEmpty() ? addr : NO_LOCATION;
            spinnerType.setSelection(indexOf(TYPES, getIntent().getStringExtra("task_type")));
            spinnerStatus.setSelection(indexOf(STATUSES, getIntent().getStringExtra("task_status")));
            btnSaveTask.setText("Actualizar tarea");
        }

        // Restaurar estado tras rotar la pantalla
        if (savedInstanceState != null) {
            String sel = savedInstanceState.getString("selected_uri");
            if (sel != null) selectedImageUri = Uri.parse(sel);
            String cam = savedInstanceState.getString("camera_uri");
            if (cam != null) cameraUri = Uri.parse(cam);
            latitude = savedInstanceState.getDouble("lat", latitude);
            longitude = savedInstanceState.getDouble("lng", longitude);
            currentAddress = savedInstanceState.getString("address", currentAddress);
        }

        // Mostrar la foto (la nueva tiene prioridad sobre la existente)
        if (selectedImageUri != null) {
            showPreview(selectedImageUri);
        } else if (!existingImageUrl.isEmpty()) {
            showPreview(Uri.parse(existingImageUrl));
        }
        updateLocationText();

        // Validaciones en tiempo real
        watch(etTitle, this::validateTitle);
        watch(etDesc, this::validateDesc);

        btnSelectPhoto.setOnClickListener(v -> showPhotoOptions());
        btnRefreshGps.setOnClickListener(v -> checkLocationPermissionAndFetch());
        btnSaveTask.setOnClickListener(v -> onSaveClicked());

        // GPS automático solo al crear y solo la primera vez (no al rotar)
        if (editingTaskId == null && savedInstanceState == null) {
            checkLocationPermissionAndFetch();
        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle out) {
        super.onSaveInstanceState(out);
        if (selectedImageUri != null) out.putString("selected_uri", selectedImageUri.toString());
        if (cameraUri != null) out.putString("camera_uri", cameraUri.toString());
        out.putDouble("lat", latitude);
        out.putDouble("lng", longitude);
        out.putString("address", currentAddress);
    }

    private void registerLaunchers() {
        galleryLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(), uri -> {
                    if (uri != null) setSelectedImage(uri);
                });

        cameraLauncher = registerForActivityResult(
                new ActivityResultContracts.TakePicture(), success -> {
                    if (Boolean.TRUE.equals(success) && cameraUri != null) {
                        setSelectedImage(cameraUri);
                    }
                });

        cameraPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(), granted -> {
                    if (granted) openCamera();
                    else Toast.makeText(this,
                            "Permiso de cámara denegado. Puedes elegir una foto de la galería.",
                            Toast.LENGTH_LONG).show();
                });

        locationPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestMultiplePermissions(), result -> {
                    boolean fine = Boolean.TRUE.equals(result.get(Manifest.permission.ACCESS_FINE_LOCATION));
                    boolean coarse = Boolean.TRUE.equals(result.get(Manifest.permission.ACCESS_COARSE_LOCATION));
                    if (fine || coarse) {
                        fetchLocation();
                    } else {
                        currentAddress = NO_LOCATION;
                        updateLocationText();
                        Toast.makeText(this,
                                "Sin permiso de ubicación. La tarea se guardará sin GPS.",
                                Toast.LENGTH_LONG).show();
                    }
                });
    }

    // ---------- Foto ----------

    private void showPhotoOptions() {
        new AlertDialog.Builder(this)
                .setTitle("Agregar foto")
                .setItems(new String[]{"Tomar foto", "Elegir de la galería"}, (d, which) -> {
                    if (which == 0) checkCameraPermissionAndOpen();
                    else galleryLauncher.launch("image/*");
                })
                .show();
    }

    private void checkCameraPermissionAndOpen() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            openCamera();
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA);
        }
    }

    private void openCamera() {
        try {
            File dir = new File(getCacheDir(), "images");
            if (!dir.exists()) dir.mkdirs();
            File file = new File(dir, "photo_" + System.currentTimeMillis() + ".jpg");
            cameraUri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", file);
            cameraLauncher.launch(cameraUri);
        } catch (Exception e) {
            Toast.makeText(this, "No se pudo abrir la cámara", Toast.LENGTH_SHORT).show();
        }
    }

    private void setSelectedImage(Uri uri) {
        selectedImageUri = uri;
        showPreview(uri);
    }

    private void showPreview(Uri uri) {
        imageEvidencePreview.setVisibility(View.VISIBLE);
        Glide.with(this).load(uri).centerCrop().into(imageEvidencePreview);
    }

    // ---------- Ubicación ----------

    private void checkLocationPermissionAndFetch() {
        boolean fine = ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        boolean coarse = ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        if (fine || coarse) {
            fetchLocation();
        } else {
            locationPermissionLauncher.launch(new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION});
        }
    }

    private void fetchLocation() {
        locationHelper.getFreshLocation((lat, lng, address) -> {
            if (isFinishing() || isDestroyed()) return;
            latitude = lat;
            longitude = lng;
            currentAddress = address != null && !address.isEmpty() ? address : NO_LOCATION;
            updateLocationText();
        });
    }

    private void updateLocationText() {
        boolean hasCoords = latitude != 0.0 || longitude != 0.0;
        String text = "Ubicación: " + currentAddress;
        if (hasCoords) {
            text += "\nCoords: " + String.format(Locale.US, "%.5f, %.5f", latitude, longitude);
        }
        textLocationInfo.setText(text);
    }

    // ---------- Guardado ----------

    private void onSaveClicked() {
        if (!(validateTitle() & validateDesc())) return;

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "Tu sesión expiró. Inicia sesión de nuevo.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        // Se arma aquí, para que no dependa de las vistas si la pantalla rota durante la subida
        final String title = etTitle.getText().toString().trim();
        final TaskItem task = new TaskItem(
                editingTaskId != null ? editingTaskId : "",
                title,
                etDesc.getText().toString().trim(),
                existingImageUrl,
                latitude,
                longitude,
                currentAddress,
                authManager.getCurrentEmail(),
                user.getUid(),
                authManager.getCurrentRole(),
                spinnerType.getSelectedItem().toString(),
                spinnerStatus.getSelectedItem().toString(),
                etApiary.getText() != null ? etApiary.getText().toString().trim() : "",
                System.currentTimeMillis());

        btnSaveTask.setEnabled(false);
        progressBarUpload.setVisibility(View.VISIBLE);

        if (selectedImageUri != null) {
            progressBarUpload.setIndeterminate(false);
            progressBarUpload.setMax(100);
            progressBarUpload.setProgress(0);

            ImageUploader.upload(this, selectedImageUri, new ImageUploader.Callback() {
                @Override public void onProgress(int percent) {
                    if (!isFinishing() && !isDestroyed()) progressBarUpload.setProgress(percent);
                }
                @Override public void onSuccess(String url) {
                    task.setImageUrl(url);   // URL pública de Cloudinary
                    persist(task);
                }
                @Override public void onError(String message) {
                    if (isFinishing() || isDestroyed()) return;
                    progressBarUpload.setVisibility(View.GONE);
                    btnSaveTask.setEnabled(true);
                    Toast.makeText(AddTaskActivity.this, message, Toast.LENGTH_LONG).show();
                }
            });
        } else {
            progressBarUpload.setIndeterminate(true);
            persist(task);
        }
    }

    private void persist(TaskItem task) {
        boolean editing = editingTaskId != null;
        Runnable onSuccess = () -> {
            notificationHelper.showTaskNotification(
                    (editing ? "Tarea modificada: " : "Tarea creada: ") + task.getTitle(),
                    editing ? "La tarea se actualizó correctamente."
                            : "Nueva tarea de apiario registrada y sincronizada.");
            if (isFinishing() || isDestroyed()) return;
            Toast.makeText(this, editing ? "¡Tarea actualizada!" : "¡Guardado con éxito!",
                    Toast.LENGTH_SHORT).show();
            finish();
        };
        FirestoreRepository.OnErrorListener onError = error -> {
            if (isFinishing() || isDestroyed()) return;
            progressBarUpload.setVisibility(View.GONE);
            btnSaveTask.setEnabled(true);
            Toast.makeText(this, "Error: " + error, Toast.LENGTH_LONG).show();
        };

        if (editing) {
            // Solo los campos editables: se conservan autor, rol y fecha originales
            Map<String, Object> fields = new HashMap<>();
            fields.put("title", task.getTitle());
            fields.put("description", task.getDescription());
            fields.put("imageUrl", task.getImageUrl());
            fields.put("latitude", task.getLatitude());
            fields.put("longitude", task.getLongitude());
            fields.put("address", task.getAddress());
            fields.put("type", task.getType());
            fields.put("status", task.getStatus());
            fields.put("apiary", task.getApiary());
            firestoreRepository.updateTask(editingTaskId, fields, onSuccess, onError);
        } else {
            firestoreRepository.saveTask(task, onSuccess, onError);
        }
    }

    // ---------- Validaciones ----------

    private boolean validateTitle() {
        String t = etTitle.getText() != null ? etTitle.getText().toString().trim() : "";
        if (t.isEmpty()) { tilTitle.setError("El título es obligatorio"); return false; }
        if (t.length() < 3) { tilTitle.setError("Mínimo 3 caracteres"); return false; }
        tilTitle.setError(null);
        return true;
    }

    private boolean validateDesc() {
        String d = etDesc.getText() != null ? etDesc.getText().toString().trim() : "";
        if (d.isEmpty()) { tilDesc.setError("La descripción es obligatoria"); return false; }
        tilDesc.setError(null);
        return true;
    }

    private void watch(TextInputEditText et, Runnable action) {
        et.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            @Override public void onTextChanged(CharSequence s, int st, int b, int c) {}
            @Override public void afterTextChanged(Editable s) { action.run(); }
        });
    }

    private int indexOf(String[] array, String value) {
        if (value != null) {
            for (int i = 0; i < array.length; i++) {
                if (array[i].equals(value)) return i;
            }
        }
        return 0;
    }
}