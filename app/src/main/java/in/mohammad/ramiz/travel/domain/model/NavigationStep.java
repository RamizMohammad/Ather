package in.mohammad.ramiz.travel.domain.model;

import com.mappls.sdk.maps.geometry.LatLng;

/** One turn-by-turn instruction. */
public class NavigationStep {

    public String instruction;   // plain text, HTML stripped
    public String maneuver;      // "turn-right", "roundabout-left", ...
    public int distanceMeters;
    public int durationSeconds;
    public LatLng start;
    public LatLng end;
}

