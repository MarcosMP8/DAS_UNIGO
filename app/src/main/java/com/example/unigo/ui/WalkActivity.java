package com.example.unigo.ui;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.preference.PreferenceManager;

import com.example.unigo.R;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;

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
    private static final int REQUEST_PERMS = 123;
    private static final int REQUEST_LOCATION = 1;

    private static final GeoPoint CAMPUS_LOCATION = new GeoPoint(42.839448, -2.670349);

    private MapView map;
    private IMapController mapController;
    private FusedLocationProviderClient locClient;
    private ImageButton btnBack;
    private TextView tvInfo;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Configuration.getInstance().load(
                this,
                PreferenceManager.getDefaultSharedPreferences(this)
        );
        setContentView(R.layout.activity_walk);

        btnBack = findViewById(R.id.btn_back);
        tvInfo = findViewById(R.id.tv_info);
        tvInfo.setVisibility(View.GONE);
        progressBar = findViewById(R.id.progressBar);

        if (btnBack != null) {
            btnBack.bringToFront();
            btnBack.setOnClickListener(v -> {
                Intent intent = new Intent(this, MainMenuActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
                finish();
            });
        }

        map = findViewById(R.id.map);
        map.setTileSource(TileSourceFactory.MAPNIK);
        map.setMultiTouchControls(true);
        mapController = map.getController();
        mapController.setZoom(14.5);

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

    @SuppressLint("MissingPermission")
    private void setupMap() {
        progressBar.setVisibility(View.VISIBLE);
        Toast.makeText(this, getString(R.string.getting_location), Toast.LENGTH_SHORT).show();

        locClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null).addOnSuccessListener(this, location -> {
            if (location != null) {
                GeoPoint startPoint = new GeoPoint(location.getLatitude(), location.getLongitude());
                mapController.setCenter(startPoint);
                addUserMarker(startPoint);
                addDestMarker(CAMPUS_LOCATION, getString(R.string.campus));
                drawRoute(startPoint, CAMPUS_LOCATION);
            } else {
                Toast.makeText(this, getString(R.string.not_location), Toast.LENGTH_LONG).show();
                progressBar.setVisibility(View.GONE);
            }
        });
    }

    private void addUserMarker(GeoPoint p) {
        Marker m = new Marker(map);
        m.setPosition(p);
        m.setTitle(getString(R.string.your_location));
        m.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        map.getOverlays().add(m);
    }

    private void addDestMarker(GeoPoint p, String title) {
        Marker m = new Marker(map);
        m.setPosition(p);
        m.setTitle(title);
        m.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        m.setIcon(getResources().getDrawable(R.drawable.ic_campus_red, getTheme()));
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

            runOnUiThread(() -> {
                if (road.mStatus != Road.STATUS_OK) {
                    Log.e("WalkActivity", "Error al calcular ruta, status=" + road.mStatus);
                    return;
                }

                Polyline overlay = RoadManager.buildRoadOverlay(road);
                Paint paint = overlay.getPaint();
                paint.setColor(Color.RED);
                paint.setStrokeWidth(8f);
                paint.setPathEffect(new DashPathEffect(new float[]{10, 10}, 0));

                map.getOverlays().add(overlay);
                map.invalidate();

                double distanciaKm = road.mLength;
                int minutos = (int) Math.round((distanciaKm / 5.0) * 60);
                String info = getString(
                        R.string.walk_info,
                        distanciaKm,
                        minutos
                );
                tvInfo.setText(info);
                tvInfo.setVisibility(View.VISIBLE);
                tvInfo.bringToFront();
                tvInfo.invalidate();
                tvInfo.requestLayout();
            });
        }).start();
    }

    private GeoPoint generarUbicacionDentroVitoria() {
        double minLat = 42.82, maxLat = 42.87;
        double minLon = -2.71, maxLon = -2.64;
        double lat = minLat + Math.random() * (maxLat - minLat);
        double lon = minLon + Math.random() * (maxLon - minLon);
        return new GeoPoint(lat, lon);
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode,
            @NonNull String[] permissions,
            @NonNull int[] grantResults
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if ((requestCode == REQUEST_PERMS || requestCode == REQUEST_LOCATION)
                && grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            setupMap();
        } else {
            Toast.makeText(
                    this,
                    getString(R.string.permission_location_denied),
                    Toast.LENGTH_SHORT
            ).show();
        }
    }
}