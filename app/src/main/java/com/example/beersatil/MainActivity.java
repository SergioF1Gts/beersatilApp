package com.example.beersatil;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.beersatil.adapter.TaskAdapter;
import com.example.beersatil.helper.AuthManager;
import com.example.beersatil.helper.FirestoreRepository;
import com.example.beersatil.model.TaskItem;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.firestore.ListenerRegistration;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class MainActivity extends AppCompatActivity {
    private TaskAdapter taskAdapter;
    private List<TaskItem> allTasks;
    private List<TaskItem> filteredTasks;
    private AuthManager authManager;
    private FirestoreRepository firestoreRepository;
    private ListenerRegistration tasksListener;
    private CardView cardSummaryPanel;
    private TextView textSummaryStats;
    private EditText etSearch;
    private String currentQuery = "";

    private final ActivityResultLauncher<String> notificationPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                if (!granted) {
                    Toast.makeText(this,
                            "Sin permiso de notificaciones no verás los avisos de tareas.",
                            Toast.LENGTH_LONG).show();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        authManager = new AuthManager(this);
        if (!authManager.isLoggedIn()) {   // sin sesión válida, de vuelta al Login
            goToLogin();
            return;
        }

        setContentView(R.layout.activity_main);
        requestNotificationPermissionIfNeeded();

        firestoreRepository = new FirestoreRepository();
        RecyclerView recyclerView = findViewById(R.id.recyclerViewTasks);
        FloatingActionButton fabAddTask = findViewById(R.id.fabAddTask);
        Button btnOpenMap = findViewById(R.id.btnOpenMap);
        Button btnOpenProfile = findViewById(R.id.btnOpenProfile);
        cardSummaryPanel = findViewById(R.id.cardSummaryPanel);
        textSummaryStats = findViewById(R.id.textSummaryStats);
        etSearch = findViewById(R.id.etSearch);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        allTasks = new ArrayList<>();
        filteredTasks = new ArrayList<>();

        final boolean isAdmin = authManager.isAdmin();
        final String uid = authManager.getCurrentUid();
        cardSummaryPanel.setVisibility(isAdmin ? View.VISIBLE : View.GONE);

        taskAdapter = new TaskAdapter(filteredTasks, authManager.getCurrentRole(),
                new TaskAdapter.OnTaskActionListener() {
                    @Override
                    public void onDelete(TaskItem task) {
                        confirmDelete(task);
                    }

                    @Override
                    public void onEdit(TaskItem task) {
                        openDetail(task);
                    }
                });
        recyclerView.setAdapter(taskAdapter);

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                currentQuery = s != null ? s.toString() : "";
                filterAndDisplayTasks();
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        // Sincronización en tiempo real: el admin recibe todo, el operario solo lo suyo
        tasksListener = firestoreRepository.getTasksRealtime(isAdmin, uid,
                new FirestoreRepository.OnTasksLoadedListener() {
                    @Override
                    public void onTasksLoaded(List<TaskItem> tasks) {
                        allTasks.clear();
                        allTasks.addAll(tasks);
                        filterAndDisplayTasks();
                        if (isAdmin) updateSummaryStats(allTasks);
                    }

                    @Override
                    public void onError(Exception e) {
                        Toast.makeText(MainActivity.this,
                                "Error de sincronización: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                });

        fabAddTask.setOnClickListener(v ->
                startActivity(new Intent(MainActivity.this, AddTaskActivity.class)));
        btnOpenMap.setOnClickListener(v ->
                startActivity(new Intent(MainActivity.this, MapsActivity.class)));
        btnOpenProfile.setOnClickListener(v ->
                startActivity(new Intent(MainActivity.this, ProfileActivity.class)));
    }

    @Override
    protected void onDestroy() {
        if (tasksListener != null) tasksListener.remove();
        super.onDestroy();
    }

    private void goToLogin() {
        Intent i = new Intent(this, LoginActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(i);
        finish();
    }

    private void requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
        }
    }

    private void confirmDelete(TaskItem task) {
        new AlertDialog.Builder(this)
                .setTitle("Eliminar tarea")
                .setMessage("¿Eliminar \"" + task.getTitle() + "\"? Esta acción no se puede deshacer.")
                .setNegativeButton("Cancelar", null)
                .setPositiveButton("Eliminar", (d, w) ->
                        firestoreRepository.deleteTask(task.getId(),
                                () -> Toast.makeText(this, "Tarea eliminada", Toast.LENGTH_SHORT).show(),
                                error -> Toast.makeText(this, "Error al eliminar: " + error,
                                        Toast.LENGTH_SHORT).show()))
                .show();
    }

    private void openDetail(TaskItem task) {
        Intent intent = new Intent(this, TaskDetailActivity.class);
        intent.putExtra("task_id", task.getId());
        intent.putExtra("task_title", task.getTitle());
        intent.putExtra("task_desc", task.getDescription());
        intent.putExtra("task_image", task.getImageUrl());
        intent.putExtra("task_lat", task.getLatitude());
        intent.putExtra("task_lng", task.getLongitude());
        intent.putExtra("task_address", task.getAddress());
        intent.putExtra("task_type", task.getType());
        intent.putExtra("task_status", task.getStatus());
        intent.putExtra("task_apiary", task.getApiary());
        intent.putExtra("task_author", task.getAuthorEmail());
        startActivity(intent);
    }

    private String normalizeString(String input) {
        if (input == null) return "";
        String n = Normalizer.normalize(input, Normalizer.Form.NFD);
        return n.replaceAll("\\p{InCombiningDiacriticalMarks}+", "").toLowerCase().trim();
    }

    private void filterAndDisplayTasks() {
        filteredTasks.clear();
        String q = normalizeString(currentQuery);
        if (q.isEmpty()) {
            filteredTasks.addAll(allTasks);
        } else {
            for (TaskItem t : allTasks) {
                if (normalizeString(t.getTitle()).contains(q)
                        || normalizeString(t.getDescription()).contains(q)
                        || normalizeString(t.getType()).contains(q)
                        || normalizeString(t.getApiary()).contains(q)) {
                    filteredTasks.add(t);
                }
            }
        }
        taskAdapter.notifyDataSetChanged();
    }

    private void updateSummaryStats(List<TaskItem> tasks) {
        int open = 0;
        Map<String, Integer> byOperator = new TreeMap<>();
        for (TaskItem t : tasks) {
            if (!"Resuelta".equalsIgnoreCase(t.getStatus())) open++;
            String who = t.getAuthorEmail() != null ? t.getAuthorEmail() : "desconocido";
            Integer n = byOperator.get(who);
            byOperator.put(who, n == null ? 1 : n + 1);
        }
        StringBuilder sb = new StringBuilder("Total tareas: " + tasks.size()
                + " | Incidentes abiertos: " + open);
        for (Map.Entry<String, Integer> e : byOperator.entrySet()) {
            sb.append("\n• ").append(e.getKey()).append(": ").append(e.getValue());
        }
        textSummaryStats.setText(sb.toString());
    }
}