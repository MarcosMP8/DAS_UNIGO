package com.example.unigo.model;

import org.osmdroid.util.GeoPoint;

public class ParadaBus {
    private int stopId;
    private String stopName;
    private double lat;
    private double lon;

    public ParadaBus(int stopId, String stopName, double lat, double lon) {
        this.stopId = stopId;
        this.stopName = stopName;
        this.lat = lat;
        this.lon = lon;
    }

    public int getStopId() {
        return stopId;
    }

    public String getStopName() {
        return stopName;
    }

    public double getLat() {
        return lat;
    }

    public double getLon() {
        return lon;
    }

    public GeoPoint getGeoPoint() {
        return new GeoPoint(lat, lon);
    }
}
