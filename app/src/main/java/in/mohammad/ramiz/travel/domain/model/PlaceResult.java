package in.mohammad.ramiz.travel.domain.model;

/**
 * Provider-agnostic search result. UI layers depend on this instead of
 * Mappls (or any other provider) response classes, so swapping the search
 * backend never touches fragments/dialogs again.
 */
public class PlaceResult {

    public String name;
    public String address;
    public double lat;
    public double lng;
    /** Mappls pin (6-char code), when available. */
    public String mapplsPin;

    /** Straight-line distance from the user, metres; < 0 when unknown. */
    public double distanceMeters = -1;

    public PlaceResult() {
    }

    public PlaceResult(String name, String address, double lat, double lng, String mapplsPin) {
        this.name = name;
        this.address = address;
        this.lat = lat;
        this.lng = lng;
        this.mapplsPin = mapplsPin;
    }
}

