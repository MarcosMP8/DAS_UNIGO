package com.example.unigo.ui;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import com.example.unigo.R;
import com.google.android.material.card.MaterialCardView;

public class MainMenuActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_menu);

        // Universidad (quizá volvamos al dashboard o muestre info del campus)
//        findViewById(R.id.card_university).setOnClickListener(v -> {
            // Ejemplo: volver al Home
//            finish();
//        });

//        findViewById(R.id.card_walk).setOnClickListener(v ->
//                startActivity(new Intent(this, WalkActivity.class))
//        );

//        findViewById(R.id.card_bike).setOnClickListener(v ->
//                startActivity(new Intent(this, BikeActivity.class))
//        );

//        findViewById(R.id.card_tram).setOnClickListener(v ->
//                startActivity(new Intent(this, TramActivity.class))
//        );

//        findViewById(R.id.card_bus).setOnClickListener(v ->
//                startActivity(new Intent(this, BusActivity.class))
//        );

        // Perfil
        findViewById(R.id.card_profile).setOnClickListener(v ->
                startActivity(new Intent(this, ProfileActivity.class))
        );
    }
}