package com.example.beersatil;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Patterns;
import android.widget.Button;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.beersatil.helper.AuthManager;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

public class LoginActivity extends AppCompatActivity {
    private TextInputLayout tilEmail, tilPassword;
    private TextInputEditText etEmail, etPassword;
    private Button btnLogin, btnToggleRegister, btnFillAdmin, btnFillOperator;
    private AuthManager authManager;
    private boolean isRegistering = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        authManager = new AuthManager(this);

        // Auto-login
        if (authManager.isLoggedIn()) {
            startActivity(new Intent(this, MainActivity.class));
            finish();
            return;
        }

        setContentView(R.layout.activity_login);

        tilEmail = findViewById(R.id.tilEmail);
        tilPassword = findViewById(R.id.tilPassword);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        btnToggleRegister = findViewById(R.id.btnToggleRegister);
        btnFillAdmin = findViewById(R.id.btnFillAdmin);
        btnFillOperator = findViewById(R.id.btnFillOperator);

        // Validaciones en tiempo real
        watch(etEmail, this::validateEmail);
        watch(etPassword, this::validatePassword);

        btnToggleRegister.setOnClickListener(v -> {
            isRegistering = !isRegistering;
            btnLogin.setText(isRegistering ? "Registrarse" : "Iniciar Sesión");
            btnToggleRegister.setText(isRegistering
                    ? "¿Ya tienes cuenta? Inicia sesión"
                    : "¿No tienes cuenta? Regístrate");
        });

        btnFillAdmin.setOnClickListener(v -> {
            etEmail.setText("beer@gmail.com");
            etPassword.setText("123456");
        });

        btnFillOperator.setOnClickListener(v -> {
            etEmail.setText("operator@beersatil.com");
            etPassword.setText("password123");
        });

        btnLogin.setOnClickListener(v -> {
            // '&' (no '&&') para que se evalúen y muestren ambos errores a la vez
            if (!(validateEmail() & validatePassword())) return;

            String email = etEmail.getText().toString().trim();
            String password = etPassword.getText().toString();

            btnLogin.setEnabled(false);

            AuthManager.OnAuthListener listener = new AuthManager.OnAuthListener() {
                @Override
                public void onSuccess() {
                    startActivity(new Intent(LoginActivity.this, MainActivity.class));
                    finish();
                }

                @Override
                public void onError(String error) {
                    btnLogin.setEnabled(true);
                    Toast.makeText(LoginActivity.this, error, Toast.LENGTH_LONG).show();
                }
            };

            if (isRegistering) authManager.register(email, password, listener);
            else authManager.login(email, password, listener);
        });
    }

    private boolean validateEmail() {
        String email = etEmail.getText() != null ? etEmail.getText().toString().trim() : "";
        if (email.isEmpty()) {
            tilEmail.setError("El correo es obligatorio");
            return false;
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            tilEmail.setError("Ingresa un correo válido");
            return false;
        }
        tilEmail.setError(null);
        return true;
    }

    private boolean validatePassword() {
        String pass = etPassword.getText() != null ? etPassword.getText().toString() : "";
        if (pass.isEmpty()) {
            tilPassword.setError("La contraseña es obligatoria");
            return false;
        }
        if (pass.length() < 6) {
            tilPassword.setError("Mínimo 6 caracteres");
            return false;
        }
        tilPassword.setError(null);
        return true;
    }

    private void watch(TextInputEditText et, Runnable action) {
        et.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            @Override public void onTextChanged(CharSequence s, int st, int b, int c) {}
            @Override public void afterTextChanged(Editable s) { action.run(); }
        });
    }
}