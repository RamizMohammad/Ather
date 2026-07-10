package in.mohammad.ramiz.travel.util;

import com.mappls.sdk.maps.geometry.LatLng;

import java.util.ArrayList;
import java.util.List;

/**
 * Decodes encoded polylines into Mappls LatLng lists.
 * Mappls Directions returns polyline6 (6-digit precision) by default;
 * classic Google-style encoding uses precision 5.
 */
public final class PolylineUtil {

    private PolylineUtil() {
    }

    public static List<LatLng> decode(String encoded) {
        return decode(encoded, 6);
    }

    public static List<LatLng> decode(String encoded, int precision) {
        List<LatLng> poly = new ArrayList<>();
        if (encoded == null || encoded.isEmpty()) return poly;
        double factor = Math.pow(10, precision);
        int index = 0, len = encoded.length();
        long lat = 0, lng = 0;
        while (index < len) {
            int b, shift = 0, result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            long dlat = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lat += dlat;
            shift = 0;
            result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            long dlng = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lng += dlng;
            poly.add(new LatLng(lat / factor, lng / factor));
        }
        return poly;
    }
}

