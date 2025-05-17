package com.example.unigo.ui;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.unigo.R;

public class UniversityActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_university);

        // 1. Configurar Spinner con layouts multilinea
        Spinner spinnerCarreras = findViewById(R.id.spinnerCarreras);
        String[] carreras = getResources().getStringArray(R.array.alava_carreras);

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                R.layout.spinner_item_multiline,              // diseño collapsed
                carreras
        );
        adapter.setDropDownViewResource(
                R.layout.spinner_dropdown_multiline           // diseño dropdown
        );
        spinnerCarreras.setAdapter(adapter);

        // 2. Manejar selección de carrera
        spinnerCarreras.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            boolean firstCall = true;
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (firstCall) {
                    firstCall = false;
                    return;
                }
                String seleccion = carreras[position];
                Toast.makeText(
                        UniversityActivity.this,
                        "Has seleccionado: " + seleccion,
                        Toast.LENGTH_SHORT
                ).show();
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) { }
        });

        // 3. Enlace a la web de la UPV/EHU usando TextView clicable
        TextView tvVisitUniversity = findViewById(R.id.tvVisitUniversity);
        tvVisitUniversity.setOnClickListener(v -> {
            Intent intent = new Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("https://www.ehu.eus")
            );
            startActivity(intent);
        });

        // 4. Botón “Volver” que abre MainMenuActivity
        Button btnVolver = findViewById(R.id.btnVolverUniversity);
        btnVolver.setOnClickListener(v -> {
            Intent backIntent = new Intent(
                    UniversityActivity.this,
                    MainMenuActivity.class
            );
            startActivity(backIntent);
            finish();
        });
    }
}