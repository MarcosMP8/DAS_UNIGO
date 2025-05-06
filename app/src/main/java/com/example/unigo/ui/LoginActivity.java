package com.example.unigo.ui;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.unigo.R;
import com.example.unigo.network.ApiService;
import com.example.unigo.network.LoginResponse;
import com.example.unigo.network.RetrofitClient;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LoginActivity extends AppCompatActivity {

    private EditText etUsername, etPassword;
    private Button btnLogin;
    private TextView tvRegister;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Comprobar si hay sesión ya iniciada
        SharedPreferences prefs = getSharedPreferences("SessionPrefs", MODE_PRIVATE);
        if (prefs.getBoolean("isLoggedIn", false)) {
            Toast.makeText(this, "Sesión ya iniciada", Toast.LENGTH_SHORT).show();
            // Aquí puedes redirigir a MapActivity u otra pantalla cuando esté lista
            // startActivity(new Intent(this, MapActivity.class));
            // finish();
        }

        setContentView(R.layout.activity_login);

        etUsername = findViewById(R.id.etUsername);
        etPassword = findViewById(R.id.etPassword);
        btnLogin   = findViewById(R.id.btnLogin);
        tvRegister = findViewById(R.id.tvRegister);

        btnLogin.setOnClickListener(v -> loginUser());

        tvRegister.setOnClickListener(v -> {
            startActivity(new Intent(LoginActivity.this, RegisterActivity.class));
            finish();
        });
    }

    private void loginUser() {
        String username = etUsername.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (username.isEmpty() || password.isEmpty()) {
            showErrorDialog("Campos vacíos", "Introduce usuario y contraseña.");
            return;
        }

        ApiService apiService = RetrofitClient.getInstance().create(ApiService.class);
        Call<LoginResponse> call = apiService.loginUser(username, password);
        call.enqueue(new Callback<LoginResponse>() {
            @Override
            public void onResponse(Call<LoginResponse> call, Response<LoginResponse> response) {
                if (!response.isSuccessful() || response.body() == null) {
                    showErrorDialog("Error", "No se pudo conectar al servidor.");
                    return;
                }

                LoginResponse login = response.body();
                if (login.isSuccess()) {
                    // Guardar sesión
                    SharedPreferences.Editor editor = getSharedPreferences("SessionPrefs", MODE_PRIVATE).edit();
                    editor.putBoolean("isLoggedIn", true);
                    editor.putString("name", username);
                    editor.putString("email", login.getEmail());
                    editor.putString("phone", login.getPhone());
                    editor.putInt("userId", login.getId());
                    editor.putString("photoUrl", login.getPhotoUrl());
                    editor.apply();

                    Toast.makeText(LoginActivity.this, "Login exitoso", Toast.LENGTH_SHORT).show();

                    // Aquí podrías redirigir más adelante:
                    // startActivity(new Intent(LoginActivity.this, MapActivity.class));
                    // finish();

                } else {
                    showErrorDialog("Error de login", login.getMessage());
                }
            }

            @Override
            public void onFailure(Call<LoginResponse> call, Throwable t) {
                showErrorDialog("Fallo de conexión", t.getMessage());
            }
        });
    }

    private void showErrorDialog(String title, String message) {
        new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("Aceptar", null)
                .show();
    }
}
