package com.example.beersatil.helper;

import java.util.Map;
import android.net.Uri;
import com.example.beersatil.model.TaskItem;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class FirestoreRepository {
    private static final String TASKS = "tasks";
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    public interface OnTasksLoadedListener {
        void onTasksLoaded(List<TaskItem> tasks);
        void onError(Exception e);
    }

    public interface OnErrorListener {
        void onError(String message);
    }

    /**
     * Escucha en tiempo real. El admin recibe todas las tareas;
     * el operario solo las suyas. Guarda el resultado y llama a remove()
     * en onDestroy/onStop de la Activity.
     */
    public ListenerRegistration getTasksRealtime(boolean isAdmin, String uid,
                                                 OnTasksLoadedListener listener) {
        Query query = db.collection(TASKS);
        if (!isAdmin) {
            query = query.whereEqualTo("authorUid", uid);
        }
        return query.addSnapshotListener((snapshot, error) -> {
            if (error != null) {
                listener.onError(error);
                return;
            }
            List<TaskItem> list = new ArrayList<>();
            if (snapshot != null) {
                for (DocumentSnapshot doc : snapshot.getDocuments()) {
                    TaskItem item = doc.toObject(TaskItem.class);
                    if (item != null) {
                        item.setId(doc.getId());
                        list.add(item);
                    }
                }
            }
            // Más recientes primero (en la app, para no exigir un índice compuesto)
            Collections.sort(list, (a, b) -> Long.compare(b.getTimestamp(), a.getTimestamp()));
            listener.onTasksLoaded(list);
        });
    }

    /** @deprecated Sin filtro por rol. Reemplázalo por la versión con (isAdmin, uid). */
    @Deprecated
    public ListenerRegistration getTasksRealtime(OnTasksLoadedListener listener) {
        return getTasksRealtime(true, "", listener);
    }

    /** Crea o actualiza una tarea. Si no tiene id, genera uno. */
    public void saveTask(TaskItem task, Runnable onSuccess, OnErrorListener onError) {
        if (task.getId() == null || task.getId().isEmpty()) {
            task.setId(db.collection(TASKS).document().getId());
        }
        db.collection(TASKS).document(task.getId())
                .set(task)
                .addOnSuccessListener(v -> onSuccess.run())
                .addOnFailureListener(e -> onError.onError(e.getMessage()));
    }

    /** @deprecated Temporal. Guarda una URI local, que solo funciona en este teléfono. */
    @Deprecated
    public void uploadImageAndAddTask(Uri imageUri, TaskItem task,
                                      Runnable onSuccess, OnErrorListener onError) {
        if (imageUri != null) task.setImageUrl(imageUri.toString());
        saveTask(task, onSuccess, onError);
    }

    public void deleteTask(String taskId, Runnable onSuccess, OnErrorListener onError) {
        db.collection(TASKS).document(taskId)
                .delete()
                .addOnSuccessListener(v -> onSuccess.run())
                .addOnFailureListener(e -> onError.onError(e.getMessage()));
    }
    public void updateTask(String taskId, Map<String, Object> fields,
                           Runnable onSuccess, OnErrorListener onError) {
        db.collection(TASKS).document(taskId)
                .update(fields)
                .addOnSuccessListener(v -> onSuccess.run())
                .addOnFailureListener(e -> onError.onError(e.getMessage()));
    }
}