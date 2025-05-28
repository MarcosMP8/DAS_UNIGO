package com.example.unigo.ui;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import com.example.unigo.R;

import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private static final String PREFS_NAME  = "app_prefs";
    private static final String KEY_LOCALE  = "app_locale";
    private static final String KEY_DARK    = "dark_mode";

    private Button btnAcceder;
    private TextView enlaceRegistro;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        boolean dark = prefs.getBoolean(KEY_DARK, false);
        AppCompatDelegate.setDefaultNightMode(
                dark
                        ? AppCompatDelegate.MODE_NIGHT_YES
                        : AppCompatDelegate.MODE_NIGHT_NO
        );

        String savedLang = prefs.getString(KEY_LOCALE, Locale.getDefault().getLanguage());
        setAppLocale(savedLang);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btnAcceder     = findViewById(R.id.btnAcceder);
        enlaceRegistro = findViewById(R.id.enlaceRegistro);

        // Ir a LoginActivity cuando se pulsa "Acceder"
        btnAcceder.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, LoginActivity.class);
            startActivity(intent);
        });

        // Ir a RegisterActivity desde el texto de registro
        enlaceRegistro.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, RegisterActivity.class);
            startActivity(intent);
        });
    }

    private void setAppLocale(String langCode) {
        Locale locale = new Locale(langCode);
        Locale.setDefault(locale);
        Configuration config = getResources().getConfiguration();
        config.setLocale(locale);
        getResources().updateConfiguration(config, getResources().getDisplayMetrics());
    }
}
