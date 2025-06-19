package com.example.unigo.ui;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.location.Location;
import android.os.Bundle;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.unigo.R;
import com.example.unigo.model.ParadaBus;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;

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
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class BusActivity extends AppCompatActivity {

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;
    private MapView map;
    private GeoPoint campus = new GeoPoint(42.8386, -2.6733);
    private GeoPoint ubicacionActual; // Reemplaza la ubicación aleatoria
    private List<ParadaBus> todasLasParadas = new ArrayList<>();
    private Map<String, List<Integer>> tripStops = new HashMap<>();

    // Componentes de la UI
    private TextView tvInfo;
    private LinearLayout listaParadas;
    private ScrollView panelRuta;
    private ImageButton btnToggle;
    private boolean infoExpandida = false;

    // Cliente para servicios de ubicación
    private FusedLocationProviderClient fusedLocationClient;


    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Context ctx = getApplicationContext();
        Configuration.getInstance().load(ctx, PreferenceManager.getDefaultSharedPreferences(ctx));
        setContentView(R.layout.activity_bus);

        // Inicialización del cliente de ubicación
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // Inicialización de vistas
        map = findViewById(R.id.map);
        tvInfo = findViewById(R.id.tv_info);
        listaParadas = findViewById(R.id.lista_paradas);
        panelRuta = findViewById(R.id.panel_ruta);
        btnToggle = findViewById(R.id.btn_toggle_panel);

        setupMap();
        setupUIListeners();

        // Iniciar el proceso para obtener la ubicación y calcular la ruta
        iniciarProcesoDeRuta();
    }

    /**
     * Configura las propiedades iniciales del mapa.
     */
    private void setupMap() {
        map.setTileSource(TileSourceFactory.MAPNIK);
        map.setMultiTouchControls(true);
        map.getZoomController().setVisibility(CustomZoomButtonsController.Visibility.ALWAYS);
        map.getController().setZoom(14.5);
    }

    /**
     * Configura los listeners para los elementos de la UI.
     */
    @SuppressLint("ClickableViewAccessibility")
    private void setupUIListeners() {
        map.setOnTouchListener((v, event) -> {
            if (panelRuta.getVisibility() == View.VISIBLE) {
                panelRuta.animate()
                        .translationX(panelRuta.getWidth())
                        .setDuration(300)
                        .withEndAction(() -> panelRuta.setVisibility(View.GONE))
                        .start();
                return true;
            }
            return false;
        });

        btnToggle.setOnClickListener(v -> {
            if (panelRuta.getVisibility() == View.GONE) {
                panelRuta.setVisibility(View.VISIBLE);
                panelRuta.animate().translationX(0).setDuration(300).start();
            } else {
                panelRuta.animate()
                        .translationX(panelRuta.getWidth())
                        .setDuration(300)
                        .withEndAction(() -> panelRuta.setVisibility(View.GONE))
                        .start();
            }
        });

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());
    }

    /**
     * Inicia el flujo: comprueba permisos y si se conceden, obtiene la ubicación.
     */
    private void iniciarProcesoDeRuta() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            obtenerUbicacionYCalcularRuta();
        } else {
            // Solicitar permisos si no se tienen
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
        }
    }


    /**
     * Callback que se ejecuta tras la solicitud de permisos.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permiso concedido, obtener ubicación
                obtenerUbicacionYCalcularRuta();
            } else {
                // Permiso denegado
                Toast.makeText(this, "Permiso de ubicación necesario para calcular la ruta.", Toast.LENGTH_LONG).show();
                // Opcional: Centrar el mapa en el campus si no hay ubicación
                map.getController().setCenter(campus);
                marcarCampusAlava();
            }
        }
    }

    /**
     * Obtiene la última ubicación conocida y comienza el cálculo de la ruta.
     */
    @SuppressLint("MissingPermission")
    private void obtenerUbicacionYCalcularRuta() {
        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(this, location -> {
                    if (location != null) {
                        // Ubicación obtenida con éxito
                        ubicacionActual = new GeoPoint(location.getLatitude(), location.getLongitude());
                        Log.d("LOCATION", "Ubicación obtenida: " + ubicacionActual.toString());

                        // Centrar mapa en la ubicación actual y añadir marcadores
                        map.getController().setCenter(ubicacionActual);
                        mostrarUbicacionActual();
                        marcarCampusAlava();

                        // Iniciar el cálculo de la ruta en un hilo secundario
                        calcularRutaCompleta();
                    } else {
                        Toast.makeText(this, "No se pudo obtener la ubicación actual. Intenta activar la ubicación del dispositivo.", Toast.LENGTH_LONG).show();
                        Log.e("LOCATION", "Location is null");
                        // Centrar el mapa en el campus como fallback
                        map.getController().setCenter(campus);
                        marcarCampusAlava();
                    }
                });
    }

    /**
     * Contiene toda la lógica de carga de datos y cálculo de rutas en un hilo secundario.
     */
    private void calcularRutaCompleta() {
        new Thread(() -> {
            // Cargar datos de paradas y rutas de bus
            cargarParadas();
            cargarStopTimes();

            if (ubicacionActual == null) {
                runOnUiThread(() -> Toast.makeText(BusActivity.this, "Ubicación desconocida.", Toast.LENGTH_SHORT).show());
                return;
            }

            // Encontrar la parada más cercana que tenga una ruta hacia el campus
            ParadaBus origen = encontrarParadaCercanaConRutaACampus();
            if (origen == null) {
                runOnUiThread(() -> Toast.makeText(this, "No se encontró una ruta de bus directa al campus desde una parada cercana.", Toast.LENGTH_LONG).show());
                return;
            }

            int campusId = encontrarStopIdPorGeoPoint(campus);
            List<Integer> stopsRuta = buscarRutaBus(origen.getStopId(), campusId);
            if (stopsRuta == null || stopsRuta.isEmpty()) {
                runOnUiThread(() -> Toast.makeText(this, "Ruta en bus no encontrada.", Toast.LENGTH_SHORT).show());
                return;
            }

            ParadaBus ultima = getParadaById(stopsRuta.get(stopsRuta.size() - 1));

            // Mostrar información y rutas en la UI
            runOnUiThread(() -> {
                mostrarParada(origen, "Parada origen");
                mostrarParada(ultima, "Última parada");
                mostrarParadasEnLista(stopsRuta);
            });

            // Calcular distancias y tiempos para cada tramo
            double[] distancias = new double[3];
            int[] tiempos = new int[3];

            distancias[0] = ubicacionActual.distanceToAsDouble(origen.getGeoPoint()) / 1000.0;
            tiempos[0] = (int) Math.round(distancias[0] / 5.0 * 60); // A pie (5km/h)

            // La distancia del bus se calculará con la ruta real
            // El tiempo se basará en esa distancia (ej. 30km/h)

            distancias[2] = ultima.getGeoPoint().distanceToAsDouble(campus) / 1000.0;
            tiempos[2] = (int) Math.round(distancias[2] / 5.0 * 60); // A pie

            // Dibujar las rutas en el mapa
            drawWalkingRoute(ubicacionActual, origen.getGeoPoint());
            drawBusRouteOnRoad(stopsRuta, (distanciaBus, tiempoBus) -> {
                // Este callback se ejecuta cuando la ruta del bus se ha calculado
                distancias[1] = distanciaBus;
                tiempos[1] = tiempoBus;

                runOnUiThread(() -> {
                    tvInfo.setText(String.format(
                            Locale.getDefault(),
                            "🟢 A pie hasta '%s': %.2f km (%d min)\n🚌 En bus: %.2f km (%d min)\n🔴 A pie al campus: %.2f km (%d min)",
                            origen.getStopName(), distancias[0], tiempos[0],
                            distancias[1], tiempos[1],
                            distancias[2], tiempos[2]
                    ));
                    tvInfo.setMaxLines(3);
                    tvInfo.setOnClickListener(v -> {
                        infoExpandida = !infoExpandida;
                        tvInfo.setMaxLines(infoExpandida ? Integer.MAX_VALUE : 3);
                    });
                    tvInfo.setVisibility(View.VISIBLE);
                });
            });
            drawWalkingRoute(ultima.getGeoPoint(), campus);

        }).start();
    }


    private void mostrarParadasEnLista(List<Integer> ids) {
        // Limpiar vistas antiguas, manteniendo el título
        if (listaParadas.getChildCount() > 1) {
            listaParadas.removeViews(1, listaParadas.getChildCount() - 1);
        }
        for (int id : ids) {
            ParadaBus p = getParadaById(id);
            if (p != null) {
                TextView t = new TextView(this);
                t.setText("• " + p.getStopName());
                t.setTextSize(16f);
                t.setPadding(8, 8, 8, 8);
                listaParadas.addView(t);
            }
        }
    }


    private List<Integer> buscarRutaBus(int origenId, int destinoId) {
        for (List<Integer> stops : tripStops.values()) {
            if (stops.contains(origenId) && stops.contains(destinoId)) {
                int i1 = stops.indexOf(origenId);
                int i2 = stops.indexOf(destinoId);
                if (i1 < i2) return stops.subList(i1, i2 + 1);
            }
        }
        return null;
    }

    private void cargarParadas() {
        if (!todasLasParadas.isEmpty()) return; // Evitar recargar
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(getAssets().open("stops.txt")))) {
            reader.readLine(); // skip header
            String line;
            while ((line = reader.readLine()) != null) {
                String[] p = line.split(",");
                if (p.length >= 7) {
                    int id = Integer.parseInt(p[0]);
                    String name = p[2].replace("\"", "");
                    double lat = Double.parseDouble(p[5]);
                    double lon = Double.parseDouble(p[6]);
                    todasLasParadas.add(new ParadaBus(id, name, lat, lon));
                }
            }
        } catch (Exception e) {
            Log.e("BUS", "Error leyendo stops.txt", e);
        }
    }

    private void cargarStopTimes() {
        if (!tripStops.isEmpty()) return; // Evitar recargar
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(getAssets().open("stop_times.txt")))) {
            reader.readLine(); // skip header
            String line;
            while ((line = reader.readLine()) != null) {
                String[] p = line.split(",");
                if (p.length >= 4) {
                    String tripId = p[0];
                    int stopId = Integer.parseInt(p[3]);
                    tripStops.computeIfAbsent(tripId, k -> new ArrayList<>()).add(stopId);
                }
            }
        } catch (Exception e) {
            Log.e("BUS", "Error leyendo stop_times.txt", e);
        }
    }

    /**
     * Dibuja la ruta del bus siguiendo las carreteras.
     * @param ids Lista de IDs de las paradas de la ruta.
     * @param callback Devuelve la distancia y tiempo calculados para la ruta del bus.
     */
    private void drawBusRouteOnRoad(List<Integer> ids, BusRouteCallback callback) {
        new Thread(() -> {
            RoadManager roadManager = new OSRMRoadManager(this, "UNIGO-BUS");
            // Se puede especificar el medio de transporte, CAR es el más adecuado para buses.
            ((OSRMRoadManager) roadManager).setMean(OSRMRoadManager.MEAN_BY_CAR);

            ArrayList<GeoPoint> waypoints = new ArrayList<>();
            for (int id : ids) {
                ParadaBus p = getParadaById(id);
                if (p != null) waypoints.add(p.getGeoPoint());
            }

            if (waypoints.size() < 2) {
                if (callback != null) callback.onRouteCalculated(0,0);
                return;
            }

            Road road = roadManager.getRoad(waypoints);
            double distanciaKm = road.mLength; // OSRM devuelve la distancia en km
            int tiempoMin = (int) Math.round(road.mDuration / 60.0); // y la duración en segundos

            runOnUiThread(() -> {
                if (road.mStatus == Road.STATUS_OK) {
                    Polyline roadOverlay = RoadManager.buildRoadOverlay(road);
                    roadOverlay.setColor(Color.rgb(0, 0, 139));
                    roadOverlay.setWidth(8f);
                    map.getOverlays().add(roadOverlay);
                    map.invalidate();
                } else {
                    Log.e("BUS_ROUTE", "Error al calcular la ruta del bus: " + road.mStatus);
                }
                if (callback != null) {
                    callback.onRouteCalculated(distanciaKm, tiempoMin);
                }
            });
        }).start();
    }


    /**
     * Dibuja una ruta a pie entre dos puntos.
     */
    private void drawWalkingRoute(GeoPoint start, GeoPoint end) {
        new Thread(() -> {
            RoadManager roadManager = new OSRMRoadManager(this, "UNIGO-WALK");
            ((OSRMRoadManager) roadManager).setMean(OSRMRoadManager.MEAN_BY_FOOT);
            Road road = roadManager.getRoad(new ArrayList<>(Arrays.asList(start, end)));

            if (Looper.myLooper() == null) {
                Looper.prepare();
            }

            runOnUiThread(() -> {
                if (road.mStatus != Road.STATUS_OK) {
                    Log.e("WALK_ROUTE", "Error al calcular ruta a pie: " + road.mStatus);
                    return;
                }
                Polyline overlay = RoadManager.buildRoadOverlay(road);
                Paint p = overlay.getPaint();
                p.setColor(Color.RED);
                p.setPathEffect(new DashPathEffect(new float[]{15, 10}, 0));
                p.setStrokeWidth(10f);
                map.getOverlays().add(overlay);
                map.invalidate();
            });
        }).start();
    }

    private int encontrarStopIdPorGeoPoint(GeoPoint p) {
        double min = Double.MAX_VALUE;
        ParadaBus mejor = null;
        for (ParadaBus parada : todasLasParadas) {
            double d = p.distanceToAsDouble(parada.getGeoPoint());
            if (d < min) {
                min = d;
                mejor = parada;
            }
        }
        return mejor != null ? mejor.getStopId() : -1;
    }

    private ParadaBus encontrarParadaCercanaConRutaACampus() {
        ParadaBus mejor = null;
        int campusId = encontrarStopIdPorGeoPoint(campus);
        double min = Double.MAX_VALUE;

        if (ubicacionActual == null) return null; // No podemos buscar si no sabemos dónde estamos

        for (ParadaBus p : todasLasParadas) {
            // Comprobar si existe una ruta que contenga esta parada y la del campus
            boolean tieneRuta = false;
            for (List<Integer> stops : tripStops.values()) {
                if (stops.contains(p.getStopId()) && stops.contains(campusId)) {
                    // Asegurarse que la parada está antes que la del campus en la ruta
                    if (stops.indexOf(p.getStopId()) < stops.indexOf(campusId)) {
                        tieneRuta = true;
                        break;
                    }
                }
            }

            if (tieneRuta) {
                double dist = ubicacionActual.distanceToAsDouble(p.getGeoPoint());
                if (dist < min) {
                    min = dist;
                    mejor = p;
                }
            }
        }
        return mejor;
    }

    private void mostrarParada(ParadaBus parada, String titulo) {
        Marker marker = new Marker(map);
        marker.setPosition(parada.getGeoPoint());
        marker.setTitle(titulo + ": " + parada.getStopName());
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        try {
            marker.setIcon(ContextCompat.getDrawable(this, R.drawable.ic_parada));
        } catch (Exception e) {
            Log.e("MARKER_ICON", "Error al cargar el icono de parada.", e);
        }
        map.getOverlays().add(marker);
    }

    private void marcarCampusAlava() {
        Marker marker = new Marker(map);
        marker.setPosition(campus);
        marker.setTitle("Campus UPV/EHU");
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        try {
            marker.setIcon(ContextCompat.getDrawable(this, R.drawable.ic_campus_red));
        } catch (Exception e) {
            Log.e("MARKER_ICON", "Error al cargar el icono del campus.", e);
        }
        map.getOverlays().add(marker);
    }

    /**
     * Muestra un marcador en la ubicación actual del usuario.
     */
    private void mostrarUbicacionActual() {
        if (ubicacionActual == null) return;
        Marker marker = new Marker(map);
        marker.setPosition(ubicacionActual);
        marker.setTitle("Tu ubicación");
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        // Opcional: Usar un icono diferente para la ubicación del usuario
        // marker.setIcon(ContextCompat.getDrawable(this, R.drawable.ic_my_location));
        map.getOverlays().add(marker);
    }

    private ParadaBus getParadaById(int id) {
        for (ParadaBus p : todasLasParadas) {
            if (p.getStopId() == id) return p;
        }
        return null;
    }

    // Interfaz de callback para obtener el resultado del cálculo de la ruta del bus
    interface BusRouteCallback {
        void onRouteCalculated(double distanciaKm, int tiempoMin);
    }
}
