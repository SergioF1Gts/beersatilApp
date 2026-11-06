package com.example.beersatil.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.example.beersatil.R;
import com.example.beersatil.helper.ImageUploader;
import com.example.beersatil.helper.LocationHelper;
import com.example.beersatil.helper.StatusStyle;
import com.example.beersatil.model.TaskItem;
import java.util.List;

public class TaskAdapter extends RecyclerView.Adapter<TaskAdapter.TaskViewHolder> {
    private final List<TaskItem> taskList;
    private final String userRole;
    private final OnTaskActionListener actionListener;

    public interface OnTaskActionListener {
        void onDelete(TaskItem task);
        void onEdit(TaskItem task);
    }

    public TaskAdapter(List<TaskItem> taskList, String userRole, OnTaskActionListener actionListener) {
        this.taskList = taskList;
        this.userRole = userRole;
        this.actionListener = actionListener;
    }

    @NonNull
    @Override
    public TaskViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_task, parent, false);
        return new TaskViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TaskViewHolder holder, int position) {
        TaskItem task = taskList.get(position);

        holder.titleText.setText(task.getTitle());
        holder.descText.setText(task.getDescription());

        boolean hasAddress = task.getAddress() != null && !task.getAddress().isEmpty();
        boolean hasCoords = task.getLatitude() != 0.0 || task.getLongitude() != 0.0;
        holder.locationText.setText(hasAddress ? task.getAddress()
                : hasCoords ? "Lat: " + task.getLatitude() + ", Lng: " + task.getLongitude()
                : LocationHelper.NO_LOCATION);

        String author = task.getAuthorEmail() != null && !task.getAuthorEmail().isEmpty()
                ? task.getAuthorEmail() : "desconocido";
        holder.authorText.setText("Autor: " + author);

        if (holder.typeText != null) {
            String apiary = !task.getApiary().isEmpty() ? " | " + task.getApiary() : "";
            holder.typeText.setText(task.getType() + apiary);
        }
        if (holder.statusText != null) {
            StatusStyle.apply(holder.statusText, task.getStatus());
        }

        // Glide: caché, placeholder, error y sin bug de reciclaje
        String url = task.getImageUrl();
        if (url != null && !url.isEmpty() && !url.equalsIgnoreCase("null")) {
            holder.taskImageView.setVisibility(View.VISIBLE);
            Glide.with(holder.itemView.getContext())
                    .load(ImageUploader.optimized(url, 600))
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .placeholder(android.R.drawable.ic_menu_gallery)
                    .error(android.R.drawable.ic_menu_report_image)
                    .centerCrop()
                    .into(holder.taskImageView);
        } else {
            Glide.with(holder.itemView.getContext()).clear(holder.taskImageView);
            holder.taskImageView.setImageDrawable(null);
            holder.taskImageView.setVisibility(View.GONE);
        }

        // RBAC: solo el administrador ve el botón de eliminar
        boolean isAdmin = userRole != null && userRole.equalsIgnoreCase("admin");
        if (isAdmin) {
            holder.deleteButton.setVisibility(View.VISIBLE);
            holder.deleteButton.setContentDescription("Eliminar tarea");
            holder.deleteButton.setOnClickListener(v -> {
                if (actionListener != null) actionListener.onDelete(task);
            });
        } else {
            holder.deleteButton.setVisibility(View.GONE);
            holder.deleteButton.setOnClickListener(null);
        }

        holder.itemView.setOnClickListener(v -> {
            if (actionListener != null) actionListener.onEdit(task);
        });
    }

    @Override
    public int getItemCount() {
        return taskList.size();
    }

    public static class TaskViewHolder extends RecyclerView.ViewHolder {
        TextView titleText, descText, locationText, authorText, typeText, statusText;
        ImageView taskImageView, deleteButton;

        public TaskViewHolder(@NonNull View itemView) {
            super(itemView);
            titleText = itemView.findViewById(R.id.textTaskTitle);
            descText = itemView.findViewById(R.id.textTaskDesc);
            locationText = itemView.findViewById(R.id.textTaskLocation);
            authorText = itemView.findViewById(R.id.textTaskAuthor);
            typeText = itemView.findViewById(R.id.textTaskType);
            statusText = itemView.findViewById(R.id.textTaskStatus);
            taskImageView = itemView.findViewById(R.id.imageTaskEvidence);
            deleteButton = itemView.findViewById(R.id.buttonDeleteTask);
        }
    }
}