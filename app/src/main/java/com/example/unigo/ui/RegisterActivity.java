// RegisterActivity.java
package com.example.unigo.ui;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.unigo.R;
import com.example.unigo.network.ApiService;
import com.example.unigo.network.RegisterResponse;
import com.example.unigo.network.RetrofitClient;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RegisterActivity extends AppCompatActivity {
    private Button btnVolverRegister, btnRegister;
    private EditText etUsername, etPassword, etConfirmPassword, etPhone, etEmail;
    private TextView tvGoToLogin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        btnVolverRegister   = findViewById(R.id.btnVolverRegister);
        etUsername          = findViewById(R.id.etRegisterUsername);
        etPassword          = findViewById(R.id.etRegisterPassword);
        etConfirmPassword   = findViewById(R.id.etConfirmPassword);
        etPhone             = findViewById(R.id.etRegisterPhone);
        etEmail             = findViewById(R.id.etRegisterEmail);
        btnRegister         = findViewById(R.id.btnRegister);
        tvGoToLogin         = findViewById(R.id.tvGoToLogin);

        // Botón Volver al MainActivity
        btnVolverRegister.setOnClickListener(v -> {
            startActivity(new Intent(RegisterActivity.this, MainActivity.class));
            finish();
        });

        btnRegister.setOnClickListener(v -> registerUser());
        tvGoToLogin.setOnClickListener(v -> {
            startActivity(new Intent(RegisterActivity.this, LoginActivity.class));
            finish();
        });
    }

    private void registerUser() {
        String username = etUsername.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        String confirm  = etConfirmPassword.getText().toString().trim();
        String phone    = etPhone.getText().toString().trim();
        String email    = etEmail.getText().toString().trim();

        if (username.isEmpty() || password.isEmpty() || confirm.isEmpty()) {
            showErrorDialog("Campos vacíos", "Todos los campos son obligatorios.");
            return;
        }
        if (!password.equals(confirm)) {
            showErrorDialog("Error en la contraseña", "Las contraseñas no coinciden.");
            return;
        }

        ApiService api = RetrofitClient.getInstance().create(ApiService.class);
        api.registerUser(username, email, password, phone)
                .enqueue(new Callback<RegisterResponse>() {
                    @Override
                    public void onResponse(Call<RegisterResponse> call,
                                           Response<RegisterResponse> resp) {
                        if (resp.isSuccessful() && resp.body()!=null && resp.body().isSuccess()) {
                            startActivity(new Intent(RegisterActivity.this, LoginActivity.class));
                            finish();
                        } else {
                            showErrorDialog("Error", resp.body()!=null
                                    ? resp.body().getMessage()
                                    : "No se pudo conectar al servidor.");
                        }
                    }
                    @Override
                    public void onFailure(Call<RegisterResponse> call, Throwable t) {
                        showErrorDialog("Fallo de conexión", t.getMessage());
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
