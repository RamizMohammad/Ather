package in.mohammad.ramiz.travel.domain.model;

/** Live navigation state broadcast by NavigationService to the UI. */
public class NavigationState {

    public long journeyId;
    public double currentLat;
    public double currentLng;
    public double speedKmh;
    /** Direction of travel, degrees clockwise from north (last known good fix). */
    public double bearingDegrees;
    /** Legal speed limit on the current road segment, km/h; 0 = unknown. */
    public double speedLimitKmh;
    public double remainingMeters;
    public long remainingSeconds;
    public long etaEpochMillis;
    public int currentStepIndex;
    public String nextInstruction;
    public String nextManeuver;
    public double metersToNextStep;
    public boolean rerouting;
    public boolean arrived;
}

