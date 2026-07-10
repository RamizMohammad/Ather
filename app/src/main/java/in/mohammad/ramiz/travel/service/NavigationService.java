package in.mohammad.ramiz.travel.service;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.location.Location;
import android.os.IBinder;
import android.speech.tts.TextToSpeech;

import androidx.annotation.Nullable;
import androidx.lifecycle.LifecycleService;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;

import in.mohammad.ramiz.travel.core.AppExecutors;
import in.mohammad.ramiz.travel.core.Result;
import in.mohammad.ramiz.travel.data.local.entity.ReminderEntity;
import in.mohammad.ramiz.travel.data.repository.DirectionsRepository;
import in.mohammad.ramiz.travel.data.repository.JourneyRepository;
import in.mohammad.ramiz.travel.data.repository.LocationRepository;
import in.mohammad.ramiz.travel.data.repository.ReminderRepository;
import in.mohammad.ramiz.travel.data.repository.SettingsRepository;
import in.mohammad.ramiz.travel.domain.engine.NotificationEngine;
import in.mohammad.ramiz.travel.domain.engine.ReminderEngine;
import in.mohammad.ramiz.travel.domain.model.NavigationState;
import in.mohammad.ramiz.travel.domain.model.NavigationStep;
import in.mohammad.ramiz.travel.domain.model.RouteInfo;
import in.mohammad.ramiz.travel.util.FormatUtil;
import in.mohammad.ramiz.travel.util.GeoUtil;

import java.util.List;
import java.util.Locale;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;

/**
 * Foreground service driving an active journey:
 * consumes location updates, computes speed / remaining distance / ETA,
 * advances turn-by-turn steps, speaks instructions (TTS), detects going off-route
 * (-> reroute via Directions), detects arrival, evaluates ETA-based reminders,
 * and persists live stats to the journey.
 */
@AndroidEntryPoint
public class NavigationService extends LifecycleService {

    public static final String EXTRA_JOURNEY_ID = "journey_id";
    public static final String ACTION_STOP = "in.mohammad.ramiz.ather.action.STOP_NAVIGATION";

    private static final int NOTIFICATION_ID = 42;
    private static final float OFF_ROUTE_TOLERANCE_M = 60f;
    private static final float ARRIVAL_RADIUS_M = 50f;
    private static final float STEP_ADVANCE_RADIUS_M = 25f;
    private static final float VOICE_TRIGGER_M = 220f;

    /** Static LiveData bridge so the UI can observe without binding. */
    private static final MutableLiveData<NavigationState> stateLive = new MutableLiveData<>();

    public static LiveData<NavigationState> observeState() {
        return stateLive;
    }

    @Inject LocationRepository locationRepository;
    @Inject DirectionsRepository directionsRepository;
    @Inject JourneyRepository journeyRepository;
    @Inject ReminderRepository reminderRepository;
    @Inject ReminderEngine reminderEngine;
    @Inject NotificationEngine notificationEngine;
    @Inject SettingsRepository settingsRepository;
    @Inject AppExecutors executors;

    private long journeyId = -1;
    private double destLat = Double.NaN, destLng = Double.NaN;

    @Nullable private RouteInfo route;
    private int stepIndex;
    private boolean rerouting;
    private boolean announcedCurrentStep;

    private double travelledMeters;
    private double maxSpeedKmh;
    private long startedAt;
    @Nullable private Location previousLocation;
    /** Last reliable direction of travel, degrees from north. */
    private float lastBearingDeg;

    @Nullable private TextToSpeech tts;
    private boolean ttsReady;

    private final Observer<Location> locationObserver = this::onLocation;

    @Override
    public void onCreate() {
        super.onCreate();
        tts = new TextToSpeech(this, status -> {
            ttsReady = status == TextToSpeech.SUCCESS;
            if (ttsReady && tts != null) tts.setLanguage(Locale.getDefault());
        });
    }

    @SuppressLint("MissingPermission")
    @Override
    public int onStartCommand(@Nullable Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        if (intent != null && ACTION_STOP.equals(intent.getAction())) {
            stopSelf();
            return START_NOT_STICKY;
        }
        if (intent != null) {
            journeyId = intent.getLongExtra(EXTRA_JOURNEY_ID, -1);
        }
        // Clear any state left over from a previous journey (stateLive is static);
        // otherwise the UI instantly receives a stale "arrived" and closes itself.
        stateLive.setValue(null);
        startForeground(NOTIFICATION_ID,
                notificationEngine.buildNavigationNotification("Aether navigation", "Startingâ€¦"));

        startedAt = System.currentTimeMillis();
        executors.diskIO().execute(() -> {
            in.mohammad.ramiz.travel.data.local.entity.JourneyEntity j =
                    journeyRepository.dao().getByIdSync(journeyId);
            if (j == null) {
                stopSelf();
                return;
            }
            destLat = j.destLat;
            destLng = j.destLng;
            journeyRepository.start(journeyId);
            requestRoute(j.originLat, j.originLng);
        });

        locationRepository.startUpdates(2000);
        locationRepository.observeLocation().observe(this, locationObserver);
        return START_STICKY;
    }

    private void requestRoute(double fromLat, double fromLng) {
        rerouting = true;
        boolean motorcycle = in.mohammad.ramiz.travel.data.local.entity.JourneyEntity.MODE_MOTORCYCLE
                .equals(settingsRepository.getTransportMode());
        // Mappls direction manager dispatches its own async call; callback lands on main thread.
        executors.postToMain(() -> directionsRepository.getRoute(
                fromLat, fromLng, destLat, destLng, motorcycle,
                (Result<RouteInfo> result) -> {
                    if (result.hasData()) {
                        route = result.getData();
                        stepIndex = 0;
                        announcedCurrentStep = false;
                        // Publish immediately so the UI can draw the route and
                        // stats before the first GPS fix arrives.
                        publishState(fromLat, fromLng, 0,
                                GeoUtil.distanceMeters(fromLat, fromLng, destLat, destLng),
                                false);
                    }
                    rerouting = false;
                }));
    }

    private void onLocation(Location location) {
        if (route == null || rerouting) return;

        double speedKmh = location.hasSpeed() ? location.getSpeed() * 3.6 : 0;
        // Keep the last good heading: GPS bearing is only meaningful while moving.
        if (location.hasBearing() && speedKmh > 2) {
            lastBearingDeg = location.getBearing();
        } else if (previousLocation != null
                && previousLocation.distanceTo(location) > 3) {
            lastBearingDeg = previousLocation.bearingTo(location);
        }
        if (previousLocation != null) {
            travelledMeters += previousLocation.distanceTo(location);
        }
        previousLocation = location;
        maxSpeedKmh = Math.max(maxSpeedKmh, speedKmh);

        double lat = location.getLatitude();
        double lng = location.getLongitude();

        // Arrival detection
        float distToDest = GeoUtil.distanceMeters(lat, lng, destLat, destLng);
        if (distToDest <= ARRIVAL_RADIUS_M) {
            onArrived(speedKmh);
            return;
        }

        // Off-route -> recalculate
        if (GeoUtil.isOffRoute(route.path, lat, lng, OFF_ROUTE_TOLERANCE_M)) {
            speak("Recalculating route");
            requestRoute(lat, lng);
            publishState(lat, lng, speedKmh, distToDest, true);
            return;
        }

        advanceSteps(lat, lng);
        publishState(lat, lng, speedKmh, distToDest, false);
        evaluateReminders(lat, lng, speedKmh);
        persistStats(speedKmh);
    }

    private void advanceSteps(double lat, double lng) {
        List<NavigationStep> steps = route.steps;
        if (steps == null || steps.isEmpty() || stepIndex >= steps.size()) return;

        NavigationStep current = steps.get(stepIndex);
        if (current.end != null) {
            float distToStepEnd = GeoUtil.distanceMeters(lat, lng,
                    current.end.getLatitude(), current.end.getLongitude());
            if (!announcedCurrentStep && distToStepEnd <= VOICE_TRIGGER_M) {
                speak(current.instruction);
                announcedCurrentStep = true;
            }
            if (distToStepEnd <= STEP_ADVANCE_RADIUS_M && stepIndex < steps.size() - 1) {
                stepIndex++;
                announcedCurrentStep = false;
            }
        }
    }

    private void publishState(double lat, double lng, double speedKmh,
                              float distToDest, boolean isRerouting) {
        NavigationState s = new NavigationState();
        s.journeyId = journeyId;
        s.currentLat = lat;
        s.currentLng = lng;
        s.speedKmh = speedKmh;
        s.bearingDegrees = lastBearingDeg;
        s.speedLimitKmh = speedLimitAt(lat, lng);
        s.rerouting = isRerouting;
        s.remainingMeters = GeoUtil.remainingMetersOnPath(route.path, lat, lng);
        double effSpeed = speedKmh > 10 ? speedKmh : 40;
        s.remainingSeconds = (long) (s.remainingMeters / (effSpeed * 1000 / 3600));
        s.etaEpochMillis = System.currentTimeMillis() + s.remainingSeconds * 1000;
        s.currentStepIndex = stepIndex;
        if (route.steps != null && stepIndex < route.steps.size()) {
            NavigationStep step = route.steps.get(stepIndex);
            s.nextInstruction = step.instruction;
            s.nextManeuver = step.maneuver;
            if (step.end != null) {
                s.metersToNextStep = GeoUtil.distanceMeters(lat, lng,
                        step.end.getLatitude(), step.end.getLongitude());
            }
        }
        stateLive.postValue(s);

        startForeground(NOTIFICATION_ID, notificationEngine.buildNavigationNotification(
                s.nextInstruction != null ? s.nextInstruction : "Navigating",
                "ETA " + FormatUtil.clockTime(s.etaEpochMillis)
                        + " Â· " + FormatUtil.distance(s.remainingMeters)));
    }

    /** Legal limit (km/h) of the route segment nearest to the position; 0 = unknown. */
    private double speedLimitAt(double lat, double lng) {
        if (route == null || route.path == null || route.path.isEmpty()
                || route.segmentSpeedLimitsKmh == null
                || route.segmentSpeedLimitsKmh.isEmpty()) {
            return 0;
        }
        int nearest = 0;
        float best = Float.MAX_VALUE;
        for (int i = 0; i < route.path.size(); i++) {
            float d = GeoUtil.distanceMeters(lat, lng,
                    route.path.get(i).getLatitude(), route.path.get(i).getLongitude());
            if (d < best) {
                best = d;
                nearest = i;
            }
        }
        int idx = Math.min(nearest, route.segmentSpeedLimitsKmh.size() - 1);
        Double limit = route.segmentSpeedLimitsKmh.get(idx);
        return limit != null ? limit : 0;
    }

    private void evaluateReminders(double lat, double lng, double speedKmh) {
        executors.diskIO().execute(() -> {
            List<ReminderEntity> pending = reminderRepository.getPendingBlocking();
            if (pending.isEmpty()) return;
            NavigationState s = stateLive.getValue();
            long eta = s != null ? s.remainingSeconds : -1;
            for (ReminderEngine.Evaluation eval :
                    reminderEngine.evaluate(pending, lat, lng, speedKmh, eta, destLat, destLng)) {
                reminderRepository.dao().markNotified(eval.reminder.id);
                notificationEngine.postReminder(eval.reminder.id,
                        eval.reminder.title, eval.reasonText);
                speak("Reminder: " + eval.reminder.title);
            }
        });
    }

    private void persistStats(double speedKmh) {
        long elapsedSec = Math.max(1, (System.currentTimeMillis() - startedAt) / 1000);
        double avg = (travelledMeters / elapsedSec) * 3.6;
        journeyRepository.updateLiveStats(journeyId, travelledMeters, maxSpeedKmh, avg);
    }

    private void onArrived(double speedKmh) {
        persistStats(speedKmh);
        journeyRepository.complete(journeyId, null);
        notificationEngine.postJourneyEvent("ARRIVAL", "You have arrived",
                "Journey complete. " + FormatUtil.distance(travelledMeters) + " travelled.", journeyId);
        speak("You have arrived at your destination");

        NavigationState s = new NavigationState();
        s.journeyId = journeyId;
        s.arrived = true;
        stateLive.postValue(s);
        stopSelf();
    }

    private void speak(String text) {
        if (ttsReady && tts != null && settingsRepository.isVoiceGuidanceEnabled()
                && text != null && !text.isEmpty()) {
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "aether_nav");
        }
    }

    @Override
    public void onDestroy() {
        stateLive.postValue(null); // don't leak this journey's state to the next one
        locationRepository.observeLocation().removeObserver(locationObserver);
        locationRepository.stopUpdates();
        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        super.onBind(intent);
        return null;
    }
}

