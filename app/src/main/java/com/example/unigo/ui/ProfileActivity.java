package com.example.unigo.ui;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.example.unigo.R;
import com.example.unigo.network.ApiService;
import com.example.unigo.network.GenericResponse;
import com.example.unigo.network.RetrofitClient;

import java.io.ByteArrayOutputStream;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ProfileActivity extends AppCompatActivity {
    private static final String TAG        = "ProfileActivity";
    private static final String PREFS      = "SessionPrefs";
    private static final String KEY_PHOTO  = "fotoUrl";

    private ImageView imgUserIcon;
    private TextView  tvName, tvPhone, tvEmail;
    private Button    btnBack;

    private String username;

    // Launcher para recibir el thumbnail de la cámara (emulador)
    private final ActivityResultLauncher<Intent> takePictureLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        Log.d(TAG, "onActivityResult: resultCode=" + result.getResultCode());
                        if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                            Bitmap thumbnail = result.getData().getExtras().getParcelable("data");
                            if (thumbnail != null) {
                                Log.d(TAG, "received camera thumbnail, size=" +
                                        thumbnail.getWidth() + "x" + thumbnail.getHeight());
                                imgUserIcon.setImageBitmap(thumbnail);
                                uploadImageToServer(thumbnail);
                            } else {
                                Log.e(TAG, "onActivityResult: thumbnail is null");
                                Toast.makeText(this, "No se obtuvo imagen", Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            Log.w(TAG, "onActivityResult: canceled or no data");
                        }
                    }
            );

    // Launcher para pedir permiso de cámara
    private final ActivityResultLauncher<String> permissionLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.RequestPermission(),
                    granted -> {
                        Log.d(TAG, "Camera permission granted? " + granted);
                        if (granted) {
                            openCamera();
                        } else {
                            Toast.makeText(this, "Permiso de cámara denegado", Toast.LENGTH_SHORT).show();
                        }
                    }
            );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);
        Log.d(TAG, "onCreate");

        imgUserIcon = findViewById(R.id.imgUserIcon);
        tvName      = findViewById(R.id.tvName);
        tvPhone     = findViewById(R.id.tvPhone);
        tvEmail     = findViewById(R.id.tvEmail);
        btnBack     = findViewById(R.id.btnBack);

        // Cargar datos de SharedPreferences
        SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        username = prefs.getString("name", "Usuario");
        String phone = prefs.getString("phone", "");
        String email = prefs.getString("email", "");
        String photoUrl = prefs.getString(KEY_PHOTO, "");

        Log.d(TAG, "loadProfile(): name=" + username +
                ", phone=" + phone + ", email=" + email +
                ", photoUrl=" + photoUrl);

        tvName.setText(username);
        tvPhone.setText("Teléfono: " + phone);
        tvEmail.setText("Email: " + email);

        if (!photoUrl.isEmpty()) {
            Glide.with(this)
                    .load(photoUrl)
                    .placeholder(R.drawable.usuario)
                    .into(imgUserIcon);
        } else {
            imgUserIcon.setImageResource(R.drawable.usuario);
        }

        imgUserIcon.setOnClickListener(v -> {
            Log.d(TAG, "imgUserIcon clicked");
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                    == PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "Camera permission already granted");
                openCamera();
            } else {
                Log.d(TAG, "Requesting camera permission");
                permissionLauncher.launch(Manifest.permission.CAMERA);
            }
        });

        btnBack.setOnClickListener(v -> {
            Log.d(TAG, "btnBack clicked, finishing activity");
            finish();
        });
    }

    private void openCamera() {
        Log.d(TAG, "openCamera()");
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        takePictureLauncher.launch(intent);
    }

    private void uploadImageToServer(Bitmap bitmap) {
        Log.d(TAG, "uploadImageToServer()");
        // Convertir bitmap a Base64
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, baos);
        String base64Image = Base64.encodeToString(baos.toByteArray(), Base64.DEFAULT);
        Log.d(TAG, "Base64 length=" + base64Image.length());

        // Llamada Retrofit
        ApiService api = RetrofitClient.getInstance().create(ApiService.class);
        Call<GenericResponse> call = api.uploadProfileImage(username, base64Image);
        Log.d(TAG, "Enqueue uploadProfileImage call");
        call.enqueue(new Callback<GenericResponse>() {
            @Override
            public void onResponse(Call<GenericResponse> call,
                                   Response<GenericResponse> response) {
                Log.d(TAG, "onResponse: code=" + response.code());
                if (response.isSuccessful() && response.body() != null) {
                    GenericResponse body = response.body();
                    Log.d(TAG, "Response body: success=" + body.isSuccess() +
                            " message=" + body.getMessage() +
                            " url=" + body.getUrl());
                    if (body.isSuccess()) {
                        String newUrl = body.getUrl();
                        getSharedPreferences(PREFS, MODE_PRIVATE)
                                .edit()
                                .putString(KEY_PHOTO, newUrl)
                                .apply();
                        Toast.makeText(ProfileActivity.this,
                                "Foto subida correctamente", Toast.LENGTH_SHORT).show();
                    } else {
                        Log.e(TAG, "Server error: " + body.getMessage());
                        Toast.makeText(ProfileActivity.this,
                                "Error servidor: " + body.getMessage(), Toast.LENGTH_LONG).show();
                    }
                } else {
                    try {
                        String err = response.errorBody() != null
                                ? response.errorBody().string()
                                : "nulo";
                        Log.e(TAG, "HTTP error: " + response.code() +
                                " body=" + err);
                    } catch (Exception e) {
                        Log.e(TAG, "Error leyendo errorBody", e);
                    }
                    Toast.makeText(ProfileActivity.this,
                            "Error servidor HTTP " + response.code(),
                            Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<GenericResponse> call, Throwable t) {
                Log.e(TAG, "onFailure: " + t.getMessage(), t);
                Toast.makeText(ProfileActivity.this,
                        "Fallo de red: " + t.getMessage(),
                        Toast.LENGTH_LONG).show();
            }
        });
    }
}
