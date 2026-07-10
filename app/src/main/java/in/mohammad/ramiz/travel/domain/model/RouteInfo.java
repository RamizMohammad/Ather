package in.mohammad.ramiz.travel.domain.model;

import com.mappls.sdk.maps.geometry.LatLng;

import java.util.List;

/** A parsed, ready-to-render route from the Directions API. */
public class RouteInfo {

    public String summary;
    public String encodedPolyline;
    public List<LatLng> path;
    public List<NavigationStep> steps;
    /** Legal speed limit (km/h) per geometry segment; 0 = unknown. May be empty. */
    public List<Double> segmentSpeedLimitsKmh;
    public int distanceMeters;
    public int durationSeconds;
    /** With live traffic, if available; otherwise == durationSeconds. */
    public int durationInTrafficSeconds;
    public String startAddress;
    public String endAddress;

    public long etaEpochMillis(long departureEpochMillis) {
        return departureEpochMillis + durationInTrafficSeconds * 1000L;
    }
}

