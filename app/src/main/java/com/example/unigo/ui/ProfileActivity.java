package com.example.unigo.ui;

import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.unigo.R;

public class ProfileActivity extends AppCompatActivity {

    private LinearLayout layoutVista, layoutEditar;
    private TextView tvNombrePerfil, tvCorreoPerfil;
    private EditText etNombrePerfil, etCorreoPerfil;
    private ImageButton btnEditarNombre;
    private Button btnGuardarPerfil, btnVolverPerfil, btnCerrarSesion;
    private Spinner spinnerGenero;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        // Referencias a vistas
        layoutVista      = findViewById(R.id.layoutVista);
        layoutEditar     = findViewById(R.id.layoutEditar);
        tvNombrePerfil   = findViewById(R.id.tvNombrePerfil);
        tvCorreoPerfil   = findViewById(R.id.tvCorreoPerfil);
        etNombrePerfil   = findViewById(R.id.etNombrePerfil);
       // etCorreoPerfil   = findViewById(R.id.etCorreoPerfil);
        btnEditarNombre  = findViewById(R.id.btnEditarNombre);
        btnGuardarPerfil = findViewById(R.id.btnGuardarPerfil);
        btnVolverPerfil  = findViewById(R.id.btnVolverPerfil);
        btnCerrarSesion  = findViewById(R.id.btnCerrarSesion);
        spinnerGenero    = findViewById(R.id.spinnerGenero);

        // Al pulsar el icono de editar, ocultamos la vista de solo lectura y mostramos la de edición
        btnEditarNombre.setOnClickListener(v -> {
            layoutVista.setVisibility(View.GONE);
            layoutEditar.setVisibility(View.VISIBLE);
            // Rellenamos los campos de edición con los valores actuales
            etNombrePerfil.setText(tvNombrePerfil.getText());
            etCorreoPerfil.setText(tvCorreoPerfil.getText());
        });

        // Guardar cambios: actualizamos los TextView y volvemos al modo solo lectura
        btnGuardarPerfil.setOnClickListener(v -> {
            String nuevoNombre = etNombrePerfil.getText().toString().trim();
            String nuevoCorreo = etCorreoPerfil.getText().toString().trim();
            if (!nuevoNombre.isEmpty()) {
                tvNombrePerfil.setText(nuevoNombre);
            }
            if (!nuevoCorreo.isEmpty()) {
                tvCorreoPerfil.setText(nuevoCorreo);
            }
            layoutEditar.setVisibility(View.GONE);
            layoutVista.setVisibility(View.VISIBLE);
        });

        // Botón volver
        btnVolverPerfil.setOnClickListener(v -> finish());

        // Botón cerrar sesión
        btnCerrarSesion.setOnClickListener(v -> {
            // TODO: limpiar preferencias / volver a LoginActivity
            finish();
        });

        // Selector de género
        spinnerGenero.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                String genero = parent.getItemAtPosition(pos).toString();
                Toast.makeText(ProfileActivity.this, "Género: " + genero, Toast.LENGTH_SHORT).show();
            }
            @Override public void onNothingSelected(AdapterView<?> parent) { }
        });
    }
}