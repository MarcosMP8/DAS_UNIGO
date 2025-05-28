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
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import com.bumptech.glide.Glide;
import com.example.unigo.R;
import com.example.unigo.network.ApiService;
import com.example.unigo.network.GenericResponse;
import com.example.unigo.network.RetrofitClient;
import com.google.android.material.textfield.TextInputEditText;
import java.io.ByteArrayOutputStream;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ProfileActivity extends AppCompatActivity {
    private static final String TAG       = "ProfileActivity";
    private static final String PREFS     = "SessionPrefs";
    private static final String KEY_PHOTO = "fotoUrl";

    private ImageView imgUserIcon;
    private TextView  tvName, tvPhone, tvEmail;
    private Button    btnBack, btnLogout;
    private View      cardProfileInfo;

    private String username;
    private int    userId;

    private final ActivityResultLauncher<Intent> takePictureLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(), result -> {
                        if (result.getResultCode() == RESULT_OK && result.getData()!=null) {
                            Bitmap thumb = result.getData().getExtras().getParcelable("data");
                            imgUserIcon.setImageBitmap(thumb);
                            uploadImageToServer(thumb);
                        }
                    }
            );

    private final ActivityResultLauncher<String> permissionLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.RequestPermission(), granted -> {
                        if (granted) openCamera();
                        else Toast.makeText(this, "Permiso de cámara denegado", Toast.LENGTH_SHORT).show();
                    }
            );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);
        Log.d(TAG, "onCreate");

        imgUserIcon      = findViewById(R.id.imgUserIcon);
        tvName           = findViewById(R.id.tvName);
        tvPhone          = findViewById(R.id.tvPhone);
        tvEmail          = findViewById(R.id.tvEmail);
        btnBack          = findViewById(R.id.btnBack);
        btnLogout        = findViewById(R.id.btnLogout);
        cardProfileInfo  = findViewById(R.id.cardProfileInfo);

        SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        username = prefs.getString("name", "Usuario");
        userId   = prefs.getInt("userId", -1);
        String phone   = prefs.getString("phone", "");
        String email   = prefs.getString("email", "");
        String photoUrl= prefs.getString(KEY_PHOTO, "");

        tvName.setText(username);
        tvPhone.setText("tel: " + phone);
        tvEmail.setText("email: "   + email);

        if (!photoUrl.isEmpty()) {
            Glide.with(this).load(photoUrl)
                    .placeholder(R.drawable.usuario).into(imgUserIcon);
        }

        imgUserIcon.setOnClickListener(v -> {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                    == PackageManager.PERMISSION_GRANTED) {
                openCamera();
            } else {
                permissionLauncher.launch(Manifest.permission.CAMERA);
            }
        });

        // Al pulsar el CardView abrimos el diálogo de edición
        cardProfileInfo.setOnClickListener(v -> showEditDialog());

        btnBack.setOnClickListener(v -> finish());
        btnLogout.setOnClickListener(v -> {
            getSharedPreferences(PREFS, MODE_PRIVATE).edit().clear().apply();
            Intent i = new Intent(this, LoginActivity.class);
            i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK|Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(i);
        });
    }

    private void showEditDialog() {
        View dialogView = LayoutInflater.from(this)
                .inflate(R.layout.dialog_edit_profile, null);
        TextInputEditText etName  = dialogView.findViewById(R.id.etDialogName);
        TextInputEditText etPhone = dialogView.findViewById(R.id.etDialogPhone);
        TextInputEditText etEmail = dialogView.findViewById(R.id.etDialogEmail);

        etName.setText(tvName.getText().toString());
        etPhone.setText(tvPhone.getText().toString().replace("Teléfono: ",""));
        etEmail.setText(tvEmail.getText().toString().replace("Email: ",""));

        new AlertDialog.Builder(this)
                .setTitle("Editar perfil")
                .setView(dialogView)
                .setPositiveButton("Guardar", (d, which) -> {
                    String newName  = etName.getText().toString().trim();
                    String newPhone = etPhone.getText().toString().trim();
                    String newEmail = etEmail.getText().toString().trim();
                    if (newName.isEmpty() || newEmail.isEmpty()) {
                        Toast.makeText(this,
                                "El nombre y email son obligatorios", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    updateProfileOnServer(newName,newPhone,newEmail);
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void updateProfileOnServer(String name, String phone, String email) {
        ApiService api = RetrofitClient.getInstance().create(ApiService.class);
        Call<GenericResponse> call = api.updateProfile(userId, name, email, phone);
        call.enqueue(new Callback<GenericResponse>() {
            @Override
            public void onResponse(Call<GenericResponse> call,
                                   Response<GenericResponse> resp) {
                if (resp.isSuccessful() && resp.body()!=null&&resp.body().isSuccess()) {
                    // Guardar en prefs
                    SharedPreferences.Editor e = getSharedPreferences(PREFS,MODE_PRIVATE).edit();
                    e.putString("name",name);
                    e.putString("phone",phone);
                    e.putString("email",email);
                    e.apply();
                    // Actualizar UI
                    tvName.setText(name);
                    tvPhone.setText("Teléfono: "+phone);
                    tvEmail.setText("Email: "+email);
                    Toast.makeText(ProfileActivity.this,
                            "Perfil actualizado", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(ProfileActivity.this,
                            "Error al actualizar: "+
                                    (resp.body()!=null?resp.body().getMessage():"Servidor"),
                            Toast.LENGTH_LONG).show();
                }
            }
            @Override
            public void onFailure(Call<GenericResponse> call, Throwable t) {
                Toast.makeText(ProfileActivity.this,
                        "Fallo de red: "+t.getMessage(),
                        Toast.LENGTH_LONG).show();
            }
        });
    }

    private void openCamera() {
        takePictureLauncher.launch(
                new Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        );
    }

    private void uploadImageToServer(Bitmap bitmap) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG,90,baos);
        String b64 = Base64.encodeToString(baos.toByteArray(),Base64.DEFAULT);
        ApiService api = RetrofitClient.getInstance().create(ApiService.class);
        api.uploadProfileImage(userId,b64)
                .enqueue(new Callback<GenericResponse>() {
                    @Override
                    public void onResponse(Call<GenericResponse> c, Response<GenericResponse> r) {
                        if (r.isSuccessful() && r.body()!=null&&r.body().isSuccess()) {
                            String url= r.body().getUrl();
                            getSharedPreferences(PREFS,MODE_PRIVATE).edit()
                                    .putString(KEY_PHOTO,url).apply();
                            Glide.with(ProfileActivity.this).load(url)
                                    .into(imgUserIcon);
                            Toast.makeText(ProfileActivity.this,
                                    "Foto actualizada",Toast.LENGTH_SHORT).show();
                        }
                    }
                    @Override public void onFailure(Call<GenericResponse> c, Throwable t){}
                });
    }
}