package com.example.unigo.ui;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.unigo.R;
import com.example.unigo.network.ApiService;
import com.example.unigo.network.GenericResponse;
import com.example.unigo.network.RetrofitClient;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ProfileActivity extends AppCompatActivity {

    private ImageButton btnVolverToolbar;
    private LinearLayout layoutVista, layoutEditar;
    private TextView tvNombrePerfil, tvCorreoPerfil, tvTelefonoPerfil;
    private EditText etNombrePerfil, etCorreoPerfil, etTelefonoPerfil;
    private ImageButton btnEditarNombre;
    private Button btnGuardarPerfil, btnVolverPerfil, btnCerrarSesion;

    private static final String PREFS = "SessionPrefs";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        // Referencias UI
        btnVolverToolbar  = findViewById(R.id.btnVolverToolbar);
        layoutVista       = findViewById(R.id.layoutVista);
        layoutEditar      = findViewById(R.id.layoutEditar);
        tvNombrePerfil    = findViewById(R.id.tvNombrePerfil);
        tvCorreoPerfil    = findViewById(R.id.tvCorreoPerfil);
        tvTelefonoPerfil  = findViewById(R.id.tvTelefonoPerfil);
        etNombrePerfil    = findViewById(R.id.etNombrePerfil);
        etCorreoPerfil    = findViewById(R.id.etCorreoPerfil);
        etTelefonoPerfil  = findViewById(R.id.etTelefonoPerfil);
        btnEditarNombre   = findViewById(R.id.btnEditarNombre);
        btnGuardarPerfil  = findViewById(R.id.btnGuardarPerfil);
        btnVolverPerfil   = findViewById(R.id.btnVolverPerfil);
        btnCerrarSesion   = findViewById(R.id.btnCerrarSesion);

        // Cargar perfil
        loadProfile();

        // Volver desde toolbar
        btnVolverToolbar.setOnClickListener(v -> finish());

        // Editar perfil
        btnEditarNombre.setOnClickListener(v -> {
            layoutVista.setVisibility(View.GONE);
            layoutEditar.setVisibility(View.VISIBLE);
            // Precargar campos
            etNombrePerfil.setText(tvNombrePerfil.getText());
            etCorreoPerfil.setText(tvCorreoPerfil.getText());
            etTelefonoPerfil.setText(tvTelefonoPerfil.getText());
        });

        // Guardar cambios y actualizar en servidor
        btnGuardarPerfil.setOnClickListener(v -> {
            SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
            int userId = prefs.getInt("userId", -1);
            String newName  = etNombrePerfil.getText().toString().trim();
            String newEmail = etCorreoPerfil.getText().toString().trim();
            String newPhone = etTelefonoPerfil.getText().toString().trim();

            // Llamada a API para actualizar en BD
            ApiService api = RetrofitClient.getInstance().create(ApiService.class);
            api.updateProfile(userId, newName, newEmail, newPhone)
                    .enqueue(new Callback<GenericResponse>() {
                        @Override
                        public void onResponse(Call<GenericResponse> call, Response<GenericResponse> response) {
                            if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                                // Actualizar SharedPreferences
                                prefs.edit()
                                        .putString("name", newName)
                                        .putString("email", newEmail)
                                        .putString("phone", newPhone)
                                        .apply();
                                // Actualizar UI
                                tvNombrePerfil.setText(newName);
                                tvCorreoPerfil.setText(newEmail);
                                tvTelefonoPerfil.setText(newPhone);
                                Toast.makeText(ProfileActivity.this,
                                        "Perfil guardado en servidor", Toast.LENGTH_SHORT).show();
                                layoutEditar.setVisibility(View.GONE);
                                layoutVista.setVisibility(View.VISIBLE);
                            } else {
                                String msg = response.body() != null ? response.body().getMessage() : "Error inesperado";
                                Toast.makeText(ProfileActivity.this, msg, Toast.LENGTH_SHORT).show();
                            }
                        }

                        @Override
                        public void onFailure(Call<GenericResponse> call, Throwable t) {
                            Toast.makeText(ProfileActivity.this,
                                    "Fallo de red: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
        });

        // Volver sin guardar
        btnVolverPerfil.setOnClickListener(v -> {
            layoutEditar.setVisibility(View.GONE);
            layoutVista.setVisibility(View.VISIBLE);
        });

        // Cerrar sesión
        btnCerrarSesion.setOnClickListener(v -> {
            getSharedPreferences(PREFS, MODE_PRIVATE).edit().clear().apply();
            startActivity(new Intent(ProfileActivity.this, LoginActivity.class));
            finish();
        });
    }

    private void loadProfile() {
        SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        String name  = prefs.getString("name", "");
        String email = prefs.getString("email", "");
        String phone = prefs.getString("phone", "");
        tvNombrePerfil.setText(name);
        tvCorreoPerfil.setText(email);
        tvTelefonoPerfil.setText(phone);
    }
}
