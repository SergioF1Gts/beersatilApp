package com.example.beersatil.helper;

import android.content.Context;
import android.content.SharedPreferences;
import com.google.firebase.FirebaseNetworkException;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseAuthWeakPasswordException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.HashMap;
import java.util.Map;

public class AuthManager {
    public static final String ROLE_ADMIN = "admin";
    public static final String ROLE_OPERATOR = "operator";

    private final SharedPreferences prefs;
    private final FirebaseAuth auth;
    private final FirebaseFirestore db;

    public AuthManager(Context context) {
        prefs = context.getSharedPreferences("beersatil_session", Context.MODE_PRIVATE);
        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
    }

    // Sesión válida solo si hay usuario de Firebase Y una sesión guardada con rol
    public boolean isLoggedIn() {
        return auth.getCurrentUser() != null && prefs.getBoolean("is_logged_in", false);
    }

    public String getCurrentEmail() {
        FirebaseUser user = auth.getCurrentUser();
        if (user != null && user.getEmail() != null) return user.getEmail();
        return prefs.getString("user_email", "");
    }

    public String getCurrentUid() {
        FirebaseUser user = auth.getCurrentUser();
        return user != null ? user.getUid() : "";
    }

    public String getCurrentRole() {
        return prefs.getString("user_role", ROLE_OPERATOR); // mínimo privilegio por defecto
    }

    public boolean isAdmin() {
        return ROLE_ADMIN.equals(getCurrentRole());
    }

    public void login(String email, String password, OnAuthListener listener) {
        auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        fetchRoleAndSaveSession(listener);
                    } else {
                        listener.onError(translateError(task.getException()));
                    }
                });
    }

    public void register(String email, String password, OnAuthListener listener) {
        auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    FirebaseUser user = auth.getCurrentUser();
                    if (!task.isSuccessful() || user == null) {
                        listener.onError(translateError(task.getException()));
                        return;
                    }

                    // Todo registro nuevo es operador. Los admin se asignan desde la consola de Firebase.
                    Map<String, Object> userData = new HashMap<>();
                    userData.put("email", email);
                    userData.put("role", ROLE_OPERATOR);
                    userData.put("createdAt", FieldValue.serverTimestamp());

                    db.collection("users").document(user.getUid())
                            .set(userData)
                            .addOnSuccessListener(v -> {
                                saveSession(email, ROLE_OPERATOR);
                                listener.onSuccess();
                            })
                            .addOnFailureListener(e -> {
                                user.delete();      // evita dejar un usuario sin documento de rol
                                auth.signOut();
                                listener.onError("No se pudo completar el registro. Intenta de nuevo.");
                            });
                });
    }

    private void fetchRoleAndSaveSession(OnAuthListener listener) {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            listener.onError("No se pudo obtener la sesión");
            return;
        }
        db.collection("users").document(user.getUid()).get()
                .addOnSuccessListener(doc -> {
                    String role = doc.exists() ? doc.getString("role") : null;
                    if (!ROLE_ADMIN.equals(role)) role = ROLE_OPERATOR; // cualquier otro valor = operador
                    saveSession(user.getEmail(), role);
                    listener.onSuccess();
                })
                .addOnFailureListener(e -> {
                    auth.signOut(); // si no se puede verificar el rol, no se entra
                    listener.onError("No se pudo verificar tu rol. Revisa tu conexión.");
                });
    }

    public void logout(Runnable onComplete) {
        auth.signOut();
        prefs.edit().clear().apply();
        onComplete.run();
    }

    private void saveSession(String email, String role) {
        prefs.edit()
                .putBoolean("is_logged_in", true)
                .putString("user_email", email)
                .putString("user_role", role)
                .apply();
    }

    private String translateError(Exception e) {
        if (e instanceof FirebaseAuthInvalidUserException
                || e instanceof FirebaseAuthInvalidCredentialsException)
            return "Correo o contraseña incorrectos";
        if (e instanceof FirebaseAuthUserCollisionException)
            return "Ya existe una cuenta con ese correo";
        if (e instanceof FirebaseAuthWeakPasswordException)
            return "La contraseña es demasiado débil";
        if (e instanceof FirebaseNetworkException)
            return "Sin conexión a internet";
        return "Ocurrió un error. Intenta de nuevo.";
    }

    public interface OnAuthListener {
        void onSuccess();
        void onError(String error);
    }
}