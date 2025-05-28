package com.example.unigo.ui;

import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import com.example.unigo.R;
import com.google.android.material.card.MaterialCardView;

import java.util.Locale;

public class MainMenuActivity extends AppCompatActivity {

    private static final String PREFS_NAME = "app_prefs";
    private static final String KEY_LOCALE  = "app_locale";
    private static final String KEY_DARK    = "dark_mode";

    private String currentLang;

    private MaterialCardView cardUniversity;
    private MaterialCardView cardBike;
    private MaterialCardView cardTram;
    private MaterialCardView cardBus;
    private MaterialCardView cardWalk;
    private MaterialCardView cardProfile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        boolean dark = prefs.getBoolean(KEY_DARK, false);
        AppCompatDelegate.setDefaultNightMode(
                dark ? AppCompatDelegate.MODE_NIGHT_YES
                        : AppCompatDelegate.MODE_NIGHT_NO
        );

        String savedLang = prefs.getString(KEY_LOCALE, "es");
        currentLang = savedLang;
        setAppLocale(savedLang);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_menu);

        cardUniversity = findViewById(R.id.card_university);
        cardBike       = findViewById(R.id.card_bike);
        cardTram       = findViewById(R.id.card_tram);
        cardBus        = findViewById(R.id.card_bus);
        cardWalk       = findViewById(R.id.card_walk);
        cardProfile    = findViewById(R.id.card_profile);

        cardUniversity.setOnClickListener(v -> {
            Toast.makeText(this, "Universidad seleccionada", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, UniversityActivity.class));
        });

        cardBike.setOnClickListener(v -> {
            Toast.makeText(this, "Bicicleta seleccionada", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, BikeActivity.class));
        });

        cardTram.setOnClickListener(v -> {
                Toast.makeText(this, "Ajustes seleccionada", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, SettingsActivity.class));
        });

        cardBus.setOnClickListener(v -> {
            Toast.makeText(this, "Autobús seleccionado", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(MainMenuActivity.this, BusActivity.class));
        });

        cardWalk.setOnClickListener(v -> {
            Toast.makeText(this, "A pie seleccionado", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(MainMenuActivity.this, WalkActivity.class));
        });

        cardProfile.setOnClickListener(v -> {
            Toast.makeText(this, "Perfil seleccionado", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, ProfileActivity.class));
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String newLang = prefs.getString(KEY_LOCALE, currentLang);
        if (!newLang.equals(currentLang)) {
            currentLang = newLang;
            recreate();
        }
    }

    private void setAppLocale(String langCode) {
        Locale locale = new Locale(langCode);
        Locale.setDefault(locale);
        Configuration config = new Configuration(getResources().getConfiguration());
        config.setLocale(locale);
        getResources().updateConfiguration(config, getResources().getDisplayMetrics());
    }
}
