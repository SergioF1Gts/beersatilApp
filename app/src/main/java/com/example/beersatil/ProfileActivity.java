package com.example.beersatil;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.example.beersatil.helper.AuthManager;

public class ProfileActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        AuthManager authManager = new AuthManager(this);
        if (!authManager.isLoggedIn()) {   // sin sesión válida, de vuelta al Login
            goToLogin();
            return;
        }
        setContentView(R.layout.activity_profile);

        TextView textProfileEmail = findViewById(R.id.textProfileEmail);
        TextView textProfileRole = findViewById(R.id.textProfileRole);
        Button btnLogout = findViewById(R.id.btnLogout);
        Button btnBack = findViewById(R.id.btnBack);

        String roleLabel = authManager.isAdmin() ? "Administrador" : "Operario";
        textProfileEmail.setText("Correo: " + authManager.getCurrentEmail());
        textProfileRole.setText("Rol asignado (RBAC): " + roleLabel);

        btnBack.setOnClickListener(v -> finish());

        // Cierra sesión en Firebase, limpia SharedPreferences y borra la pila de pantallas
        btnLogout.setOnClickListener(v -> authManager.logout(this::goToLogin));
    }

    private void goToLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}