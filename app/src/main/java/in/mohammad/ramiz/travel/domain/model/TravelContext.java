package in.mohammad.ramiz.travel.domain.model;

import androidx.annotation.Nullable;

/**
 * Everything a rule may inspect. Rules receive this single immutable-ish
 * context object so new rules never require engine signature changes.
 */
public class TravelContext {

    /** Weather where the user is right now. */
    @Nullable
    public WeatherSnapshot currentWeather;

    /** Weather at the destination at estimated arrival time. */
    @Nullable
    public WeatherSnapshot arrivalWeather;

    /** CAR or MOTORCYCLE (JourneyEntity.MODE_*). */
    public String transportMode;

    /** Trip metrics; 0 when no route planned yet. */
    public int routeDistanceMeters;
    public int routeDurationSeconds;
    public long departureEpochMillis;

    /** Traffic delay = durationInTraffic - duration (seconds); 0 when unknown. */
    public int trafficDelaySeconds;

    public boolean hasArrivalWeather() {
        return arrivalWeather != null;
    }

    public boolean isMotorcycle() {
        return "MOTORCYCLE".equals(transportMode);
    }
}

