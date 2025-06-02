package com.example.unigo.ui;

import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import com.example.unigo.R;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.switchmaterial.SwitchMaterial;

import java.util.Locale;

public class SettingsActivity extends AppCompatActivity {

    private static final String PREFS_NAME = "app_prefs";
    private static final String KEY_LOCALE  = "app_locale";
    private static final String KEY_DARK    = "dark_mode";

    private Spinner spinnerLanguage;
    private SwitchMaterial switchTheme;
    private MaterialButton btnBack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        boolean dark = prefs.getBoolean(KEY_DARK, false);
        AppCompatDelegate.setDefaultNightMode(
                dark ? AppCompatDelegate.MODE_NIGHT_YES
                        : AppCompatDelegate.MODE_NIGHT_NO
        );

        String savedLang = prefs.getString(KEY_LOCALE, Locale.getDefault().getLanguage());
        Locale locale = new Locale(savedLang);
        Locale.setDefault(locale);
        Configuration cfg = new Configuration(getResources().getConfiguration());
        cfg.setLocale(locale);
        getResources().updateConfiguration(cfg, getResources().getDisplayMetrics());

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        spinnerLanguage = findViewById(R.id.spinnerLanguage);
        switchTheme     = findViewById(R.id.switchTheme);
        btnBack         = findViewById(R.id.btnBackSettings);

        setupLanguageSpinner();
        setupThemeSwitch();
        setupBackButton();
    }

    private void setupLanguageSpinner() {
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                this,
                R.array.languages,
                android.R.layout.simple_spinner_item
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerLanguage.setAdapter(adapter);

        String[] langs   = getResources().getStringArray(R.array.languages);
        String spanish   = langs[0];
        String english   = langs[1];
        String basque  = langs[2];

        String savedLoc = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                .getString(KEY_LOCALE, Locale.getDefault().getLanguage());
        if ("en".equals(savedLoc)) {
            spinnerLanguage.setSelection(adapter.getPosition(english));
        } else if ("eu".equals(savedLoc)) {
            spinnerLanguage.setSelection(adapter.getPosition(basque));
        } else {
            spinnerLanguage.setSelection(adapter.getPosition(spanish));
        }

        spinnerLanguage.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                String sel = parent.getItemAtPosition(pos).toString();
                String code;
                if (sel.equals(english)) {
                    code = "en";
                } else if (sel.equals(basque)) {
                    code = "eu";
                } else {
                    code = "es";
                }
                SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
                String current = prefs.getString(KEY_LOCALE, "");
                if (!code.equals(current)) {
                    setLocale(code);
                }
            }
            @Override public void onNothingSelected(AdapterView<?> parent) { }
        });
    }

    private void setupThemeSwitch() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        boolean dark = prefs.getBoolean(KEY_DARK, false);
        switchTheme.setChecked(dark);

        switchTheme.setOnCheckedChangeListener((btn, isChecked) -> {
            SharedPreferences.Editor edit = getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit();
            edit.putBoolean(KEY_DARK, isChecked).apply();

            AppCompatDelegate.setDefaultNightMode(
                    isChecked ? AppCompatDelegate.MODE_NIGHT_YES
                            : AppCompatDelegate.MODE_NIGHT_NO
            );
        });
    }

    private void setupBackButton() {
        btnBack.setOnClickListener(v -> finish());
    }

    private void setLocale(String langCode) {
        Locale locale = new Locale(langCode);
        Locale.setDefault(locale);
        Configuration cfg = new Configuration(getResources().getConfiguration());
        cfg.setLocale(locale);
        getResources().updateConfiguration(cfg, getResources().getDisplayMetrics());

        getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                .edit()
                .putString(KEY_LOCALE, langCode)
                .apply();

        recreate();
    }
}