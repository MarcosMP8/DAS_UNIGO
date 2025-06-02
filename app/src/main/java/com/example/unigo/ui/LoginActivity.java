package com.example.unigo.ui;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

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
    private Button btnVolverLogin, btnLogin;
    private EditText etUsername, etPassword;
    private TextView tvRegister;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        btnVolverLogin = findViewById(R.id.btnVolverLogin);
        etUsername     = findViewById(R.id.etUsername);
        etPassword     = findViewById(R.id.etPassword);
        btnLogin       = findViewById(R.id.btnLogin);
        tvRegister     = findViewById(R.id.tvRegister);

        btnVolverLogin.setOnClickListener(v -> {
            startActivity(new Intent(LoginActivity.this, MainActivity.class));
            finish();
        });

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
            showErrorDialog(getString(R.string.error_empty_fields_title), getString(R.string.error_empty_fields_message));
            return;
        }

        ApiService api = RetrofitClient.getInstance().create(ApiService.class);
        api.loginUser(username, password).enqueue(new Callback<LoginResponse>() {
            @Override
            public void onResponse(Call<LoginResponse> call, Response<LoginResponse> resp) {
                if (!resp.isSuccessful() || resp.body() == null) {
                    showErrorDialog(getString(R.string.error_generic_title), getString(R.string.error_server_message));
                    return;
                }
                LoginResponse login = resp.body();
                if (login.isSuccess()) {
                    SharedPreferences.Editor editor =
                            getSharedPreferences("SessionPrefs", MODE_PRIVATE).edit();
                    editor.putBoolean("isLoggedIn", true);
                    editor.putInt("userId", login.getId());
                    editor.putString("name", login.getNombre());
                    editor.putString("email", login.getEmail());
                    editor.putString("phone", login.getTelefono());
                    editor.apply();

                    startActivity(new Intent(LoginActivity.this, MainMenuActivity.class));
                    finish();
                } else {
                    showErrorDialog(getString(R.string.error_login_error_title), login.getMessage());
                }
            }

            @Override
            public void onFailure(Call<LoginResponse> call, Throwable t) {
                if (t instanceof java.net.UnknownHostException) {
                    showErrorDialog(
                            getString(R.string.error_no_connection_title),
                            getString(R.string.error_no_connection_message)
                    );
                } else if (t instanceof java.net.SocketTimeoutException) {
                    showErrorDialog(
                            getString(R.string.error_server_busy_title),
                            getString(R.string.error_server_busy_message)
                    );
                } else {
                    showErrorDialog(
                            getString(R.string.error_unknown_title),
                            t.getMessage());
                }
                android.util.Log.e("LOGIN_ERROR", "Error técnico: ", t);
            }
        });
    }

    private void showErrorDialog(String title, String msg) {
        new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(msg)
                .setPositiveButton("Aceptar", null)
                .show();
    }
}