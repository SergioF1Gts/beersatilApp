package com.example.beersatil.helper;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.widget.TextView;

public class StatusStyle {
    public static void apply(TextView view, String status) {
        String s = status != null ? status : "Pendiente";
        int color;
        switch (s) {
            case "En proceso": color = Color.parseColor("#1E88E5"); break;
            case "Resuelta":   color = Color.parseColor("#2E7D32"); break;
            default:           color = Color.parseColor("#E67E00"); break; // Pendiente
        }
        float d = view.getResources().getDisplayMetrics().density;
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(color);
        bg.setCornerRadius(16 * d);
        view.setBackground(bg);
        view.setTextColor(Color.WHITE);
        view.setPadding((int) (10 * d), (int) (3 * d), (int) (10 * d), (int) (3 * d));
        view.setText(s);
    }
}