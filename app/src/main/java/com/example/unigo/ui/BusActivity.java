package com.example.unigo.ui;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.DashPathEffect;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.unigo.R;
import com.example.unigo.model.ParadaBus;

import org.osmdroid.bonuspack.routing.OSRMRoadManager;
import org.osmdroid.bonuspack.routing.Road;
import org.osmdroid.bonuspack.routing.RoadManager;
import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.CustomZoomButtonsController;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Polyline;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;

public class BusActivity extends AppCompatActivity {

    private MapView map;
    private GeoPoint ubicacionAleatoria;
    private GeoPoint campus = new GeoPoint(42.8467, -2.6731);
    private List<ParadaBus> todasLasParadas = new ArrayList<>();
    private Map<String, List<Integer>> tripStops = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Context ctx = getApplicationContext();
        Configuration.getInstance().load(ctx, PreferenceManager.getDefaultSharedPreferences(ctx));
        setContentView(R.layout.activity_bus);

        map = findViewById(R.id.map);
        map.setTileSource(TileSourceFactory.MAPNIK);
        map.setMultiTouchControls(true);
        map.getZoomController().setVisibility(CustomZoomButtonsController.Visibility.ALWAYS);

        ubicacionAleatoria = generarUbicacionDentroVitoria();
        map.getController().setZoom(14.5);
        map.getController().setCenter(ubicacionAleatoria);

        mostrarUbicacionSimulada();
        marcarCampusAlava();

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());

        new Thread(() -> {
            cargarParadas();
            cargarStopTimes();

            ParadaBus paradaOrigen = encontrarParadaCercanaConRutaACampus();
            if (paradaOrigen == null) {
                runOnUiThread(() -> Toast.makeText(this, "No hay ruta al campus", Toast.LENGTH_LONG).show());
                return;
            }

            runOnUiThread(() -> {
                mostrarParada(paradaOrigen, "Parada cercana");
                drawWalkingRoute(ubicacionAleatoria, paradaOrigen.getGeoPoint());
            });

            buscarRutaEnBus(paradaOrigen.getStopId());
        }).start();
    }

    private void cargarParadas() {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(getAssets().open("stops.txt")))) {
            String line;
            reader.readLine(); // skip header
            while ((line = reader.readLine()) != null) {
                String[] p = line.split(",");
                if (p.length >= 7) {
                    int id = Integer.parseInt(p[0]);
                    String name = p[2];
                    double lat = Double.parseDouble(p[5]);
                    double lon = Double.parseDouble(p[6]);
                    todasLasParadas.add(new ParadaBus(id, name, lat, lon));
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void cargarStopTimes() {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(getAssets().open("stop_times.txt")))) {
            String line;
            reader.readLine(); // skip header
            while ((line = reader.readLine()) != null) {
                String[] p = line.split(",");
                if (p.length >= 4) {
                    String tripId = p[0];
                    int stopId = Integer.parseInt(p[3]);
                    tripStops.computeIfAbsent(tripId, k -> new ArrayList<>()).add(stopId);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private ParadaBus encontrarParadaCercanaConRutaACampus() {
        ParadaBus mejor = null;
        double minDist = Double.MAX_VALUE;
        int campusId = encontrarStopIdPorGeoPoint(campus);

        for (ParadaBus parada : todasLasParadas) {
            for (List<Integer> stops : tripStops.values()) {
                if (stops.contains(parada.getStopId()) && stops.contains(campusId)) {
                    double dist = ubicacionAleatoria.distanceToAsDouble(parada.getGeoPoint());
                    if (dist < minDist) {
                        minDist = dist;
                        mejor = parada;
                    }
                }
            }
        }
        return mejor;
    }

    private void buscarRutaEnBus(int origenId) {
        int campusId = encontrarStopIdPorGeoPoint(campus);

        for (List<Integer> stops : tripStops.values()) {
            if (!stops.contains(origenId) || !stops.contains(campusId)) continue;

            List<GeoPoint> puntos = new ArrayList<>();
            boolean recogiendo = false;
            for (int id : stops) {
                if (id == origenId) recogiendo = true;
                if (recogiendo) {
                    ParadaBus p = getParadaById(id);
                    if (p != null) puntos.add(p.getGeoPoint());
                }
                if (id == campusId) break;
            }

            if (!puntos.isEmpty()) {
                runOnUiThread(() -> {
                    mostrarRutaBus(puntos);
                    drawWalkingRoute(puntos.get(puntos.size() - 1), campus);
                    ParadaBus paradaDestino = getParadaById(encontrarStopIdPorGeoPoint(puntos.get(puntos.size() - 1)));
                    if (paradaDestino != null) {
                        mostrarParada(paradaDestino, "Parada destino");
                    }
                });
                return;
            }
        }

        runOnUiThread(() -> Toast.makeText(this, "Ruta directa no encontrada", Toast.LENGTH_SHORT).show());
    }

    private int encontrarStopIdPorGeoPoint(GeoPoint punto) {
        ParadaBus mejor = null;
        double min = Double.MAX_VALUE;
        for (ParadaBus p : todasLasParadas) {
            double dist = punto.distanceToAsDouble(p.getGeoPoint());
            if (dist < min) {
                min = dist;
                mejor = p;
            }
        }
        return mejor != null ? mejor.getStopId() : -1;
    }

    private void mostrarRutaBus(List<GeoPoint> puntos) {
        Polyline linea = new Polyline();
        linea.setPoints(puntos);
        linea.setColor(Color.rgb(0, 0, 139));
        linea.setWidth(6f);
        map.getOverlays().add(linea);
        map.invalidate();
    }

    private ParadaBus getParadaById(int id) {
        for (ParadaBus p : todasLasParadas) {
            if (p.getStopId() == id) return p;
        }
        return null;
    }

    private void mostrarParada(ParadaBus parada, String titulo) {
        Marker marker = new Marker(map);
        marker.setPosition(parada.getGeoPoint());
        marker.setTitle(titulo + ": " + parada.getStopName());
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        marker.setIcon(getResources().getDrawable(R.drawable.ic_parada, getTheme()));
        map.getOverlays().add(marker);
    }

    private void drawWalkingRoute(GeoPoint inicio, GeoPoint fin) {
        new Thread(() -> {
            RoadManager roadManager = new OSRMRoadManager(this, "UNIGO");
            ((OSRMRoadManager) roadManager).setMean(OSRMRoadManager.MEAN_BY_FOOT);
            Road road = roadManager.getRoad(new ArrayList<>(Arrays.asList(inicio, fin)));

            runOnUiThread(() -> {
                if (road.mStatus != Road.STATUS_OK) return;

                Polyline overlay = RoadManager.buildRoadOverlay(road);
                Paint p = overlay.getPaint();
                p.setColor(Color.RED);
                p.setPathEffect(new DashPathEffect(new float[]{10, 10}, 0));
                p.setStrokeWidth(8f);

                map.getOverlays().add(overlay);
                map.invalidate();
            });
        }).start();
    }

    private void marcarCampusAlava() {
        Marker marker = new Marker(map);
        marker.setPosition(campus);
        marker.setTitle("Campus UPV/EHU");
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        marker.setIcon(getResources().getDrawable(R.drawable.ic_campus_red, getTheme()));
        map.getOverlays().add(marker);
    }

    private void mostrarUbicacionSimulada() {
        Marker marker = new Marker(map);
        marker.setPosition(ubicacionAleatoria);
        marker.setTitle("Ubicación simulada");
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        map.getOverlays().add(marker);
    }

    private GeoPoint generarUbicacionDentroVitoria() {
        double minLat = 42.82, maxLat = 42.87;
        double minLon = -2.71, maxLon = -2.64;
        double lat = minLat + Math.random() * (maxLat - minLat);
        double lon = minLon + Math.random() * (maxLon - minLon);
        return new GeoPoint(lat, lon);
    }
}