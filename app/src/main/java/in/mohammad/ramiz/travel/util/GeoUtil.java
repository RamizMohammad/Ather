package in.mohammad.ramiz.travel.util;

import android.location.Location;

import com.mappls.sdk.maps.geometry.LatLng;

import java.util.List;
import java.util.Locale;

/** Distance / bearing helpers built on android.location.Location. */
public final class GeoUtil {

    private GeoUtil() {
    }

    /** Meters between two coordinates. */
    public static float distanceMeters(double lat1, double lng1, double lat2, double lng2) {
        float[] out = new float[1];
        Location.distanceBetween(lat1, lng1, lat2, lng2, out);
        return out[0];
    }

    /** Remaining distance along a polyline from the nearest point to the end. */
    public static double remainingMetersOnPath(List<LatLng> path, double lat, double lng) {
        if (path == null || path.isEmpty()) return 0;
        int nearest = 0;
        float best = Float.MAX_VALUE;
        for (int i = 0; i < path.size(); i++) {
            float d = distanceMeters(lat, lng,
                    path.get(i).getLatitude(), path.get(i).getLongitude());
            if (d < best) {
                best = d;
                nearest = i;
            }
        }
        double remaining = distanceMeters(lat, lng,
                path.get(nearest).getLatitude(), path.get(nearest).getLongitude());
        for (int i = nearest; i < path.size() - 1; i++) {
            remaining += distanceMeters(
                    path.get(i).getLatitude(), path.get(i).getLongitude(),
                    path.get(i + 1).getLatitude(), path.get(i + 1).getLongitude());
        }
        return remaining;
    }

    /** True when (lat,lng) is farther than toleranceMeters from every path vertex. */
    public static boolean isOffRoute(List<LatLng> path, double lat, double lng, float toleranceMeters) {
        if (path == null || path.isEmpty()) return false;
        float best = Float.MAX_VALUE;
        for (LatLng p : path) {
            float d = distanceMeters(lat, lng, p.getLatitude(), p.getLongitude());
            if (d < best) best = d;
            if (best <= toleranceMeters) return false;
        }
        return true;
    }

    public static String formatLatLng(double lat, double lng) {
        return String.format(Locale.US, "%.6f,%.6f", lat, lng);
    }

    /** ~1.1 km cache grid cell key. */
    public static String cellKey(double lat, double lng) {
        return String.format(Locale.US, "%.2f,%.2f", lat, lng);
    }
}

