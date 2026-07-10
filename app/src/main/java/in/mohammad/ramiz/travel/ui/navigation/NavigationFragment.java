package in.mohammad.ramiz.travel.ui.navigation;

import android.animation.ValueAnimator;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.LinearInterpolator;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.ColorUtils;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import in.mohammad.ramiz.travel.R;
import in.mohammad.ramiz.travel.databinding.FragmentNavigationBinding;
import in.mohammad.ramiz.travel.domain.model.NavigationState;
import in.mohammad.ramiz.travel.service.NavigationService;
import in.mohammad.ramiz.travel.ui.home.HomeFragment;
import in.mohammad.ramiz.travel.util.FormatUtil;
import in.mohammad.ramiz.travel.util.MapUiUtil;
import in.mohammad.ramiz.travel.util.PolylineUtil;
import com.mappls.sdk.maps.MapplsMap;
import com.mappls.sdk.maps.OnMapReadyCallback;
import com.mappls.sdk.maps.annotations.MarkerOptions;
import com.mappls.sdk.maps.camera.CameraPosition;
import com.mappls.sdk.maps.camera.CameraUpdateFactory;
import com.mappls.sdk.maps.geometry.LatLng;

import java.util.List;

import dagger.hilt.android.AndroidEntryPoint;

/** Live turn-by-turn UI on a Mappls map, observing NavigationService state. */
@AndroidEntryPoint
public class NavigationFragment extends Fragment implements OnMapReadyCallback {

    private FragmentNavigationBinding binding;
    private NavigationViewModel viewModel;
    @Nullable
    private MapplsMap map;
    /** Journey route, decoded once; drawn as soon as both it and the map exist. */
    @Nullable
    private List<LatLng> routePath;
    @Nullable
    private LatLng destination;
    private String destinationName;
    private boolean routeDrawn;
    private boolean journeyRequested;
    /** Position puck (accent arrow); moved on every state update. */
    @Nullable
    private com.mappls.sdk.maps.annotations.Marker selfMarker;

    /** Below this speed the compass drives the map rotation instead of GPS bearing. */
    private static final double COMPASS_BELOW_KMH = 5;
    private static final float NAV_ZOOM = 18f;
    private static final double NAV_TILT = 55.0;

    /** True once the service has delivered its first live state. */
    private boolean navLive;
    @Nullable
    private com.mappls.sdk.maps.annotations.Polyline routeAccentLine;
    @Nullable
    private ValueAnimator pulseAnimator;

    @Nullable
    private SensorManager sensorManager;
    private final float[] rotationMatrix = new float[9];
    private final float[] orientationAngles = new float[3];
    /** Low-pass factor: closer to 0 = smoother/slower compass. */
    private static final float COMPASS_SMOOTHING = 0.12f;
    private static final long COMPASS_CAMERA_INTERVAL_MS = 200;
    private static final float COMPASS_MIN_DELTA_DEG = 1.5f;
    private float compassAzimuthDeg;
    private boolean compassInitialized;
    private float lastAppliedBearingDeg = Float.NaN;
    private long lastCompassCameraAt;
    @Nullable
    private LatLng lastHere;
    private double lastSpeedKmh;

    private final SensorEventListener compassListener = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent event) {
            SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values);
            SensorManager.getOrientation(rotationMatrix, orientationAngles);
            float raw = (float) Math.toDegrees(orientationAngles[0]);
            if (raw < 0) raw += 360;
            if (!compassInitialized) {
                compassAzimuthDeg = raw;
                compassInitialized = true;
            } else {
                // Exponential low-pass with wrap-around so 359° -> 1° doesn't spin.
                float delta = ((raw - compassAzimuthDeg + 540f) % 360f) - 180f;
                compassAzimuthDeg = (compassAzimuthDeg + COMPASS_SMOOTHING * delta + 360f) % 360f;
            }
            maybeRotateWithCompass();
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        }
    };

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentNavigationBinding.inflate(inflater, container, false);
        binding.mapView.onCreate(savedInstanceState);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        viewModel = new ViewModelProvider(this).get(NavigationViewModel.class);
        binding.mapView.getMapAsync(this);

        NavigationService.observeState().observe(getViewLifecycleOwner(), this::render);

        binding.buttonStop.setOnClickListener(v -> {
            Intent stop = new Intent(requireContext(), NavigationService.class);
            stop.setAction(NavigationService.ACTION_STOP);
            requireContext().startService(stop);
            backHome();
        });
    }

    @Override
    public void onMapReady(@NonNull MapplsMap mapplsMap) {
        map = mapplsMap;
        MapUiUtil.applyAetherStyle(map);
        drawRouteIfReady();
    }

    /** Loads the journey once the service tells us its id, to get the route path. */
    private void requestJourney(long journeyId) {
        if (journeyRequested || journeyId <= 0) return;
        journeyRequested = true;
        viewModel.observeJourney(journeyId).observe(getViewLifecycleOwner(), journey -> {
            if (journey == null || journey.routePolyline == null) return;
            routePath = PolylineUtil.decode(journey.routePolyline);
            destination = new LatLng(journey.destLat, journey.destLng);
            destinationName = journey.destName;
            drawRouteIfReady();
        });
    }

    /** Fix: the pathway was never drawn; draw casing + accent line + destination pin. */
    private void drawRouteIfReady() {
        if (routeDrawn || map == null || routePath == null || routePath.isEmpty()) return;
        routeDrawn = true;
        routeAccentLine = MapUiUtil.drawStyledRoute(requireContext(), map, routePath);
        if (!navLive) startRoutePulse();
        if (destination != null) {
            MarkerOptions marker = new MarkerOptions().position(destination);
            if (destinationName != null) marker.setTitle(destinationName);
            com.mappls.sdk.maps.annotations.Icon icon =
                    MapUiUtil.markerIcon(requireContext(), R.drawable.ic_pin_destination);
            if (icon != null) marker.setIcon(icon);
            map.addMarker(marker);
        }
    }

    @Override
    public void onMapError(int errorCode, String errorMessage) {
        // Navigation stats remain functional without map tiles.
    }

    private void render(NavigationState s) {
        if (s == null || binding == null) return;
        if (s.arrived) {
            // Ignore "arrived" from a previous journey replayed by LiveData
            // before the service has published anything for this one.
            if (!navLive) return;
            binding.textInstruction.setText(R.string.nav_arrived);
            binding.getRoot().postDelayed(this::backHome, 2500);
            return;
        }
        requestJourney(s.journeyId);
        if (!navLive) {
            navLive = true;
            stopRoutePulse(); // service is live: freeze the "warming up" pulse
        }
        binding.textSpeed.setText(String.valueOf(Math.round(s.speedKmh)));
        boolean speeding = s.speedLimitKmh > 0 && s.speedKmh > s.speedLimitKmh + 3;
        binding.textSpeed.setTextColor(ContextCompat.getColor(requireContext(),
                speeding ? R.color.error : R.color.on_surface));
        binding.textSpeedLimit.setText(
                s.speedLimitKmh > 0 ? String.valueOf(Math.round(s.speedLimitKmh)) : "--");
        binding.textArrival.setText(FormatUtil.clockTime(s.etaEpochMillis));
        binding.textRemaining.setText(FormatUtil.distance(s.remainingMeters)
                + " Â· " + FormatUtil.duration(s.remainingSeconds));
        if (s.rerouting) {
            binding.textInstruction.setText(R.string.nav_rerouting);
        } else if (s.nextInstruction != null) {
            binding.textInstruction.setText(s.nextInstruction);
            binding.textStepDistance.setText("In " + FormatUtil.distance(s.metersToNextStep));
        }
        if (map != null) {
            LatLng here = new LatLng(s.currentLat, s.currentLng);
            lastHere = here;
            lastSpeedKmh = s.speedKmh;
            updateSelfMarker(here);
            // While moving, the GPS course rotates the map; when slow or
            // stationary the compass takes over (see maybeRotateWithCompass).
            double bearing = s.speedKmh >= COMPASS_BELOW_KMH
                    ? s.bearingDegrees : compassAzimuthDeg;
            map.animateCamera(CameraUpdateFactory.newCameraPosition(
                    new CameraPosition.Builder()
                            .target(here)
                            .zoom(NAV_ZOOM)
                            .tilt(NAV_TILT)
                            .bearing(bearing)
                            .build()));
        }
    }

    /** Device-rotation follow: rotate the map with the compass while slow. */
    private void maybeRotateWithCompass() {
        long now = System.currentTimeMillis();
        if (map == null || lastHere == null) return;
        if (lastSpeedKmh >= COMPASS_BELOW_KMH) return;
        if (now - lastCompassCameraAt < COMPASS_CAMERA_INTERVAL_MS) return;
        // Skip micro-jitter: only move the camera for a meaningful change.
        if (!Float.isNaN(lastAppliedBearingDeg)) {
            float diff = Math.abs(((compassAzimuthDeg - lastAppliedBearingDeg + 540f) % 360f) - 180f);
            if (diff < COMPASS_MIN_DELTA_DEG) return;
        }
        lastCompassCameraAt = now;
        lastAppliedBearingDeg = compassAzimuthDeg;
        // easeCamera (linear, matching the sensor cadence) instead of
        // animateCamera avoids the stop-start choppiness of restarted animations.
        map.easeCamera(CameraUpdateFactory.newCameraPosition(
                new CameraPosition.Builder()
                        .target(lastHere)
                        .zoom(NAV_ZOOM)
                        .tilt(NAV_TILT)
                        .bearing(compassAzimuthDeg)
                        .build()), (int) COMPASS_CAMERA_INTERVAL_MS + 50);
    }

    /** Uber-style pulse of the route line while navigation is warming up. */
    private void startRoutePulse() {
        if (routeAccentLine == null || pulseAnimator != null) return;
        int base = ContextCompat.getColor(requireContext(), R.color.route_line);
        pulseAnimator = ValueAnimator.ofFloat(0f, 1f);
        pulseAnimator.setDuration(750);
        pulseAnimator.setInterpolator(new LinearInterpolator());
        pulseAnimator.setRepeatCount(ValueAnimator.INFINITE);
        pulseAnimator.setRepeatMode(ValueAnimator.REVERSE);
        pulseAnimator.addUpdateListener(anim -> {
            if (routeAccentLine == null) return;
            float t = (float) anim.getAnimatedValue();
            routeAccentLine.setColor(ColorUtils.setAlphaComponent(base, 70 + (int) (185 * t)));
            routeAccentLine.setWidth(4f + 5f * t);
        });
        pulseAnimator.start();
    }

    private void stopRoutePulse() {
        if (pulseAnimator != null) {
            pulseAnimator.cancel();
            pulseAnimator = null;
        }
        if (routeAccentLine != null) {
            routeAccentLine.setColor(
                    ContextCompat.getColor(requireContext(), R.color.route_line));
            routeAccentLine.setWidth(6f);
        }
    }

    private void updateSelfMarker(LatLng here) {
        if (map == null) return;
        if (selfMarker == null) {
            MarkerOptions options = new MarkerOptions().position(here);
            com.mappls.sdk.maps.annotations.Icon icon =
                    MapUiUtil.markerIcon(requireContext(), R.drawable.ic_nav_arrow);
            if (icon != null) options.setIcon(icon);
            selfMarker = map.addMarker(options);
        } else {
            selfMarker.setPosition(here);
        }
    }

    private void backHome() {
        if (!isAdded()) return;
        requireActivity().getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, new HomeFragment())
                .commit();
    }

    // ---- Mappls MapView lifecycle forwarding ----

    @Override
    public void onStart() {
        super.onStart();
        if (binding != null) binding.mapView.onStart();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (binding != null) binding.mapView.onResume();
        // Rotation-vector sensor (accelerometer + gyro + magnetometer fusion)
        // drives map rotation while the user is slow/stationary.
        if (sensorManager == null) {
            sensorManager = (SensorManager) requireContext()
                    .getSystemService(Context.SENSOR_SERVICE);
        }
        if (sensorManager != null) {
            Sensor rotation = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
            if (rotation != null) {
                sensorManager.registerListener(compassListener, rotation,
                        SensorManager.SENSOR_DELAY_UI);
            }
        }
    }

    @Override
    public void onPause() {
        if (sensorManager != null) sensorManager.unregisterListener(compassListener);
        if (binding != null) binding.mapView.onPause();
        super.onPause();
    }

    @Override
    public void onStop() {
        if (binding != null) binding.mapView.onStop();
        super.onStop();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        if (binding != null) binding.mapView.onLowMemory();
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if (binding != null) binding.mapView.onSaveInstanceState(outState);
    }

    @Override
    public void onDestroyView() {
        stopRoutePulse();
        if (binding != null) binding.mapView.onDestroy();
        super.onDestroyView();
        binding = null;
        map = null;
        selfMarker = null;
        routeAccentLine = null;
        routeDrawn = false;
    }
}

