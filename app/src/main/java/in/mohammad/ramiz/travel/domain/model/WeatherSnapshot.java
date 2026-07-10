package in.mohammad.ramiz.travel.domain.model;

/**
 * Flattened weather view consumed by the rule engine and UI.
 * One snapshot describes conditions at one place at one time
 * (current location now, destination now, or destination at ETA).
 */
public class WeatherSnapshot {

    public String placeName;
    public double lat;
    public double lng;

    /** Epoch millis this snapshot describes (now, or arrival time). */
    public long forTime;

    public double tempC;
    public double feelsLikeC;
    public String conditionText;
    public int conditionCode;
    public double rainChancePercent;
    public double snowChancePercent;
    public double windKph;
    public double gustKph;
    public double humidity;
    public double uvIndex;
    public double visibilityKm;
    public double precipMm;
    public double aqiPm25;
    public int usEpaIndex;

    public String sunrise;
    public String sunset;

    /** True when served from an expired cache (offline mode). */
    public boolean stale;
}

