package com.example.unigo.ui;

import android.Manifest;
import android.content.pm.PackageManager;
import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;
import android.widget.ImageButton;
import android.widget.Toast;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.preference.PreferenceManager;

import com.example.unigo.R;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;

import org.osmdroid.api.IMapController;
import org.osmdroid.bonuspack.routing.OSRMRoadManager;
import org.osmdroid.bonuspack.routing.Road;
import org.osmdroid.bonuspack.routing.RoadManager;
import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Polyline;

import java.util.ArrayList;
import java.util.Locale;

public class WalkActivity extends AppCompatActivity {
    private static final int REQUEST_PERMS    = 123;
    private static final int REQUEST_LOCATION = 1;

    // Punto para centrar inicialmente el mapa (Vitoria-Gasteiz)
    private static final GeoPoint DEFAULT_CENTER = new GeoPoint(42.8467, -2.6731);
    // Coordenadas exactas del Campus Álava (C. Comandante Izarduy, 2)
    private static final GeoPoint CAMPUS_LOCATION = new GeoPoint(42.839448, -2.670349);
    // Distancia máxima (en metros) desde DEFAULT_CENTER para validar lectura
    private static final float MAX_DIST_METERS = 50_000f;

    private MapView map;
    private IMapController mapController;
    private FusedLocationProviderClient locClient;
    private ImageButton btnBack;
    private TextView tvInfo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Configuration.getInstance().load(
                this,
                PreferenceManager.getDefaultSharedPreferences(this)
        );
        setContentView(R.layout.activity_walk);

        // Encuentra el botón de volver
        btnBack = findViewById(R.id.btn_back);
        tvInfo  = findViewById(R.id.tv_info);

        tvInfo.setVisibility(View.GONE);

        // Comprueba en el log si la referencia es nula
        if (btnBack == null) {
            Log.d("WalkActivity", "btnBack es null! ¿Layout correcto?");;
        } else {
            // Asegura que el botón esté por delante de la vista de mapa
            btnBack.bringToFront();

            // Listener con Toast para verificar el clic
            btnBack.setOnClickListener(v -> {
                Toast.makeText(this, "Botón Atrás pulsado", Toast.LENGTH_SHORT).show();

                // Intent de vuelta a MainMenuActivity
                Intent intent = new Intent(this, MainMenuActivity.class);
                intent.addFlags(
                        Intent.FLAG_ACTIVITY_CLEAR_TOP |
                                Intent.FLAG_ACTIVITY_SINGLE_TOP
                );
                startActivity(intent);
                finish();
            });
        }

        map = findViewById(R.id.map);
        map.setTileSource(TileSourceFactory.MAPNIK);
        map.setMultiTouchControls(true);
        mapController = map.getController();
        mapController.setZoom(15.0);

        // Centro y marcador inicial en Vitoria-Gasteiz
//        mapController.setCenter(DEFAULT_CENTER);
//        Marker defaultMarker = new Marker(map);
//        defaultMarker.setPosition(DEFAULT_CENTER);
//        defaultMarker.setTitle("Vitoria-Gasteiz");
//        defaultMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
//        map.getOverlays().add(defaultMarker);

        locClient = LocationServices.getFusedLocationProviderClient(this);

        if (!hasLocationPermissions()) {
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                    },
                    REQUEST_PERMS
            );
        } else {
            setupMap();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        map.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        map.onPause();
    }

    private boolean hasLocationPermissions() {
        return ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
    }

    private void setupMap() {
        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this,
                        Manifest.permission.ACCESS_COARSE_LOCATION)
                        != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(
                    this,
                    new String[]{
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                    },
                    REQUEST_LOCATION
            );
            return;
        }

        LocationRequest req = LocationRequest.create()
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .setInterval(0)
                .setNumUpdates(1);

        locClient.requestLocationUpdates(
                req,
                new LocationCallback() {
                    @Override
                    public void onLocationResult(@NonNull LocationResult result) {
                        Location loc = result.getLastLocation();
                        handleLocation(loc);
                        locClient.removeLocationUpdates(this);
                    }
                },
                Looper.getMainLooper()
        );
    }

    private void handleLocation(Location loc) {
        if (loc == null) {
            fallbackToDefault();
            return;
        }
        float[] dist = new float[1];
        Location.distanceBetween(
                loc.getLatitude(), loc.getLongitude(),
                DEFAULT_CENTER.getLatitude(), DEFAULT_CENTER.getLongitude(),
                dist
        );
        if (dist[0] > MAX_DIST_METERS) {
            fallbackToDefault();
        } else {
            GeoPoint start = new GeoPoint(loc.getLatitude(), loc.getLongitude());
            mapController.setCenter(start);
            addUserMarker(start);

            // Marcador y ruta hacia la ubicación exacta del campus
            addDestMarker(CAMPUS_LOCATION, "Campus Álava");
            drawRoute(start, CAMPUS_LOCATION);
        }
    }

    private void fallbackToDefault() {
        Toast.makeText(
                this,
                "No hay ubicación válida. Centrado en Vitoria-Gasteiz",
                Toast.LENGTH_SHORT
        ).show();
        mapController.setCenter(DEFAULT_CENTER);
    }

    private void addUserMarker(GeoPoint p) {
        Marker m = new Marker(map);
        m.setPosition(p);
        m.setTitle("Tu posición");
        m.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        map.getOverlays().add(m);
    }

    private void addDestMarker(GeoPoint p, String title) {
        Marker m = new Marker(map);
        m.setPosition(p);
        m.setTitle(title);
        m.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        map.getOverlays().add(m);
    }

    private void drawRoute(GeoPoint start, GeoPoint end) {
        OSRMRoadManager roadManager = new OSRMRoadManager(this, "UNIGO");
        roadManager.setMean(OSRMRoadManager.MEAN_BY_FOOT);

        ArrayList<GeoPoint> waypoints = new ArrayList<>();
        waypoints.add(start);
        waypoints.add(end);

        new Thread(() -> {
            Road road = roadManager.getRoad(waypoints);

            Log.d("WalkActivity", String.format(
                    Locale.getDefault(),
                    "Road computation: status=%d, lengthMeters=%.2f",
                    road.mStatus,
                    road.mLength
            ));

            runOnUiThread(() -> {
                if (road.mStatus != Road.STATUS_OK) {
                    Log.e("WalkActivity", "Error al calcular ruta, status=" + road.mStatus);
                    return;
                }

                // 1) Dibujamos la ruta
                Polyline routeOverlay = RoadManager.buildRoadOverlay(road);
                map.getOverlays().add(routeOverlay);
                map.invalidate();

                // 2) Calculamos distancia y tiempo
                double distanciaKm = road.mLength;
                int minutos = (int) Math.round((distanciaKm / 5.0) * 60);

                // 3) ACTUALIZAMOS EL TEXTVIEW
                String info = String.format(
                        Locale.getDefault(),
                        "Dist: %.2f km\nTiempo: %d min",
                        distanciaKm, minutos
                );
                tvInfo.setText(info);
                tvInfo.setVisibility(View.VISIBLE);
                tvInfo.bringToFront();
                tvInfo.invalidate();
                tvInfo.requestLayout();

                Log.d("WalkActivity", "tvInfo actualizado a: [" + info + "]");});
        }).start();
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode,
            @NonNull String[] permissions,
            @NonNull int[] grantResults
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if ((requestCode == REQUEST_PERMS ||
                requestCode == REQUEST_LOCATION)
                && grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            setupMap();
        } else {
            Toast.makeText(
                    this,
                    "Permiso de ubicación denegado",
                    Toast.LENGTH_SHORT
            ).show();
        }
    }
}