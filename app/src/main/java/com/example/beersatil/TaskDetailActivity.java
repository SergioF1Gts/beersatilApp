package com.example.beersatil;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.example.beersatil.helper.ImageUploader;
import com.example.beersatil.helper.LocationHelper;
import com.example.beersatil.helper.StatusStyle;

public class TaskDetailActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent in = getIntent();
        if (in == null || !in.hasExtra("task_id")) {   // sin datos, no hay nada que mostrar
            finish();
            return;
        }
        setContentView(R.layout.activity_task_detail);

        Button btnBack = findViewById(R.id.btnBack);
        Button btnEditTask = findViewById(R.id.btnEditTask);
        ImageView imageDetailEvidence = findViewById(R.id.imageDetailEvidence);
        TextView textDetailType = findViewById(R.id.textDetailType);
        TextView textDetailStatus = findViewById(R.id.textDetailStatus);
        TextView textDetailTitle = findViewById(R.id.textDetailTitle);
        TextView textDetailDesc = findViewById(R.id.textDetailDesc);
        TextView textDetailLocation = findViewById(R.id.textDetailLocation);
        TextView textDetailAuthor = findViewById(R.id.textDetailAuthor);

        btnBack.setOnClickListener(v -> finish());

        final String taskId = in.getStringExtra("task_id");
        final String title = in.getStringExtra("task_title");
        final String desc = in.getStringExtra("task_desc");
        final String imageUrl = in.getStringExtra("task_image");
        final double lat = in.getDoubleExtra("task_lat", 0.0);
        final double lng = in.getDoubleExtra("task_lng", 0.0);
        final String address = in.getStringExtra("task_address");
        final String type = in.getStringExtra("task_type");
        final String status = in.getStringExtra("task_status");
        final String apiary = in.getStringExtra("task_apiary");
        final String authorEmail = in.getStringExtra("task_author");

        boolean hasApiary = apiary != null && !apiary.isEmpty();
        textDetailType.setText("Tipo: " + (type != null ? type : "Revisión de colmena")
                + (hasApiary ? " | Colmena: " + apiary : ""));
        StatusStyle.apply(textDetailStatus, status);
        textDetailTitle.setText(title);
        textDetailDesc.setText(desc);

        boolean hasAddress = address != null && !address.isEmpty();
        boolean hasCoords = lat != 0.0 || lng != 0.0;
        textDetailLocation.setText("📍 " + (hasAddress ? address
                : hasCoords ? "Lat: " + lat + ", Lng: " + lng
                : LocationHelper.NO_LOCATION));
        textDetailAuthor.setText("Registrado por: " + (authorEmail != null ? authorEmail : "desconocido"));

        if (imageUrl != null && !imageUrl.isEmpty() && !imageUrl.equalsIgnoreCase("null")) {
            imageDetailEvidence.setVisibility(View.VISIBLE);
            Glide.with(this)
                    .load(ImageUploader.optimized(imageUrl, 1200))
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .placeholder(android.R.drawable.ic_menu_gallery)
                    .error(android.R.drawable.ic_menu_report_image)
                    .centerCrop()
                    .into(imageDetailEvidence);
        } else {
            imageDetailEvidence.setVisibility(View.GONE);
        }

        btnEditTask.setOnClickListener(v -> {
            Intent intent = new Intent(this, AddTaskActivity.class);
            intent.putExtra("task_id", taskId);
            intent.putExtra("task_title", title);
            intent.putExtra("task_desc", desc);
            intent.putExtra("task_image", imageUrl);
            intent.putExtra("task_lat", lat);
            intent.putExtra("task_lng", lng);
            intent.putExtra("task_address", address);
            intent.putExtra("task_type", type);
            intent.putExtra("task_status", status);
            intent.putExtra("task_apiary", apiary);
            startActivity(intent);
            finish();
        });
    }
}