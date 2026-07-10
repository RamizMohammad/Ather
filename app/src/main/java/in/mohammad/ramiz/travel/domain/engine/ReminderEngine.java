package in.mohammad.ramiz.travel.domain.engine;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import in.mohammad.ramiz.travel.data.local.entity.ReminderEntity;
import in.mohammad.ramiz.travel.util.GeoUtil;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Evaluates which reminders should fire, given the current position and live ETA.
 *
 * Algorithm per pending reminder:
 *  - MINUTES_BEFORE_ARRIVAL: fires when remaining ETA to the reminder's place
 *    drops to <= minutesBefore. ETA is approximated as distance / current average
 *    speed when the place is not the navigation destination; when it is, the
 *    navigation ETA is used directly. Traffic changes shift the ETA, so triggers
 *    adjust automatically on every evaluation tick.
 *  - ON_ARRIVAL: fires within ARRIVAL_RADIUS of the place.
 *  - AFTER_LEAVING: arms itself inside the radius, fires once the user exits.
 *  - RADIUS: fires when inside the configured radius.
 */
@Singleton
public class ReminderEngine {

    private static final float ARRIVAL_RADIUS_M = 150f;
    private static final double DEFAULT_SPEED_KMH = 35;

    /** Tracks which AFTER_LEAVING reminders are currently "armed" (user inside zone). */
    private final List<Long> armedLeaveReminders = new ArrayList<>();

    @Inject
    public ReminderEngine() {
    }

    public static class Evaluation {
        public final ReminderEntity reminder;
        public final String reasonText;

        public Evaluation(ReminderEntity reminder, String reasonText) {
            this.reminder = reminder;
            this.reasonText = reasonText;
        }
    }

    /**
     * @param etaToDestinationSec live navigation ETA in seconds, or -1 when not navigating
     * @param destLat/destLng     navigation destination, NaN when not navigating
     * @param speedKmh            current speed, <=0 when unknown
     */
    @NonNull
    public List<Evaluation> evaluate(@NonNull List<ReminderEntity> pending,
                                     double lat, double lng, double speedKmh,
                                     long etaToDestinationSec,
                                     double destLat, double destLng) {
        List<Evaluation> out = new ArrayList<>();
        for (ReminderEntity r : pending) {
            Evaluation e = evaluateOne(r, lat, lng, speedKmh, etaToDestinationSec, destLat, destLng);
            if (e != null) out.add(e);
        }
        return out;
    }

    @Nullable
    private Evaluation evaluateOne(ReminderEntity r, double lat, double lng, double speedKmh,
                                   long etaToDestSec, double destLat, double destLng) {
        float distToPlace = GeoUtil.distanceMeters(lat, lng, r.placeLat, r.placeLng);

        switch (r.triggerType) {
            case ReminderEntity.TRIGGER_ON_ARRIVAL:
                if (distToPlace <= ARRIVAL_RADIUS_M) {
                    return new Evaluation(r, "You have arrived at " + r.placeName);
                }
                return null;

            case ReminderEntity.TRIGGER_RADIUS:
                if (distToPlace <= Math.max(r.radiusMeters, ARRIVAL_RADIUS_M)) {
                    return new Evaluation(r, r.placeName + " is nearby");
                }
                return null;

            case ReminderEntity.TRIGGER_AFTER_LEAVING:
                boolean inside = distToPlace <= Math.max(r.radiusMeters, ARRIVAL_RADIUS_M);
                boolean armed = armedLeaveReminders.contains(r.id);
                if (inside && !armed) {
                    armedLeaveReminders.add(r.id);
                } else if (!inside && armed) {
                    armedLeaveReminders.remove(r.id);
                    return new Evaluation(r, "You just left " + r.placeName);
                }
                return null;

            case ReminderEntity.TRIGGER_MINUTES_BEFORE:
                long etaSec = estimateEtaSeconds(r, distToPlace, speedKmh, etaToDestSec, destLat, destLng);
                if (etaSec >= 0 && etaSec <= r.minutesBefore * 60L) {
                    long mins = Math.max(1, etaSec / 60);
                    return new Evaluation(r, "About " + mins + " min from " + r.placeName);
                }
                return null;

            default:
                return null;
        }
    }

    private long estimateEtaSeconds(ReminderEntity r, float distToPlaceM, double speedKmh,
                                    long etaToDestSec, double destLat, double destLng) {
        // If the reminder place is effectively the navigation destination, trust live ETA.
        if (etaToDestSec >= 0 && !Double.isNaN(destLat)
                && GeoUtil.distanceMeters(r.placeLat, r.placeLng, destLat, destLng) < 300) {
            return etaToDestSec;
        }
        double speed = speedKmh > 5 ? speedKmh : DEFAULT_SPEED_KMH;
        return (long) (distToPlaceM / (speed * 1000.0 / 3600.0));
    }
}

