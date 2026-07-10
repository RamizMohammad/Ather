package in.mohammad.ramiz.travel.ui.route;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import in.mohammad.ramiz.travel.R;
import in.mohammad.ramiz.travel.core.Result;
import in.mohammad.ramiz.travel.databinding.FragmentRouteBinding;
import in.mohammad.ramiz.travel.domain.model.Recommendation;
import in.mohammad.ramiz.travel.domain.model.RouteInfo;
import in.mohammad.ramiz.travel.service.NavigationService;
import in.mohammad.ramiz.travel.ui.navigation.NavigationFragment;
import in.mohammad.ramiz.travel.util.FormatUtil;
import in.mohammad.ramiz.travel.util.MapUiUtil;
import com.google.android.material.chip.Chip;
import com.mappls.sdk.maps.MapplsMap;
import com.mappls.sdk.maps.OnMapReadyCallback;
import com.mappls.sdk.maps.annotations.MarkerOptions;
import com.mappls.sdk.maps.annotations.PolylineOptions;
import com.mappls.sdk.maps.camera.CameraUpdateFactory;
import com.mappls.sdk.maps.geometry.LatLng;
import com.mappls.sdk.maps.geometry.LatLngBounds;

import java.util.List;

import dagger.hilt.android.AndroidEntryPoint;

/**
 * Route preview on a Mappls (MapmyIndia) map: polyline, ETA/arrival/distance,
 * weather strip (now -> arrival) and insight chips.
 * "Start Journey" persists everything and launches NavigationService.
 */
@AndroidEntryPoint
public class RouteFragment extends Fragment implements OnMapReadyCallback {

    private static final String ARG_LAT = "lat";
    private static final String ARG_LNG = "lng";
    private static final String ARG_NAME = "name";

    private FragmentRouteBinding binding;
    private RouteViewModel viewModel;
    @Nullable
    private MapplsMap map;
    @Nullable
    private RouteInfo pendingRoute;
    /** Set when the tab is opened without a destination: just show where the user is. */
    @Nullable
    private LatLng pendingSelfLocation;
    private boolean selfCameraCentered;

    public static RouteFragment newInstance(double lat, double lng, String name) {
        RouteFragment f = new RouteFragment();
        Bundle args = new Bundle();
        args.putDouble(ARG_LAT, lat);
        args.putDouble(ARG_LNG, lng);
        args.putString(ARG_NAME, name);
        f.setArguments(args);
        return f;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentRouteBinding.inflate(inflater, container, false);
        binding.mapView.onCreate(savedInstanceState);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        viewModel = new ViewModelProvider(this).get(RouteViewModel.class);
        binding.mapView.getMapAsync(this);

        Bundle args = getArguments();
        if (args == null || !args.containsKey(ARG_LAT)) {
            // No destination selected: show the user's current position on the map.
            binding.textDestination.setText(R.string.route_no_destination);
            binding.buttonStart.setEnabled(false);
            viewModel.startLocationUpdates();
            viewModel.getLocationUpdates().observe(getViewLifecycleOwner(), loc -> {
                if (loc != null) {
                    showSelfLocation(new LatLng(loc.getLatitude(), loc.getLongitude()));
                }
            });
            return;
        }
        double lat = args.getDouble(ARG_LAT);
        double lng = args.getDouble(ARG_LNG);
        String name = args.getString(ARG_NAME, "");
        binding.textDestination.setText(name);

        viewModel.getRoute().observe(getViewLifecycleOwner(), this::onRoute);
        viewModel.getCurrentWeather().observe(getViewLifecycleOwner(), w -> {
            if (w != null) binding.textWeatherNow.setText(
                    "Now Â· " + FormatUtil.temp(w.tempC));
        });
        viewModel.getArrivalWeather().observe(getViewLifecycleOwner(), w -> {
            if (w != null) binding.textWeatherArrival.setText(
                    "Arrival Â· " + FormatUtil.temp(w.tempC));
        });
        viewModel.getInsights().observe(getViewLifecycleOwner(), this::renderInsights);

        binding.buttonStart.setOnClickListener(v -> {
            binding.buttonStart.setEnabled(false);
            viewModel.startJourney(journeyId -> {
                Intent intent = new Intent(requireContext(), NavigationService.class);
                intent.putExtra(NavigationService.EXTRA_JOURNEY_ID, journeyId);
                ContextCompat.startForegroundService(requireContext(), intent);
                requireActivity().getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, new NavigationFragment())
                        .commit();
            });
        });

        viewModel.load(lat, lng, name);
    }

    @Override
    public void onMapReady(@NonNull MapplsMap mapplsMap) {
        map = mapplsMap;
        MapUiUtil.applyAetherStyle(map);
        map.getUiSettings().setCompassEnabled(true);
        if (pendingRoute != null) {
            drawRoute(pendingRoute);
            pendingRoute = null;
        }
        if (pendingSelfLocation != null) {
            showSelfLocation(pendingSelfLocation);
            pendingSelfLocation = null;
        }
    }

    /** Marker + camera on the user's position (no-destination mode). */
    private void showSelfLocation(LatLng position) {
        if (map == null) {
            pendingSelfLocation = position;
            return;
        }
        map.clear();
        MarkerOptions marker = new MarkerOptions().position(position);
        marker.setTitle("You are here");
        com.mappls.sdk.maps.annotations.Icon selfIcon =
                MapUiUtil.markerIcon(requireContext(), R.drawable.ic_pin_self);
        if (selfIcon != null) marker.setIcon(selfIcon);
        map.addMarker(marker);
        if (!selfCameraCentered) {
            selfCameraCentered = true;
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(position, 15));
        }
    }

    @Override
    public void onMapError(int errorCode, String errorMessage) {
        if (isAdded()) {
            Toast.makeText(requireContext(),
                    "Map failed to load: " + errorMessage, Toast.LENGTH_LONG).show();
        }
    }

    private void onRoute(Result<RouteInfo> result) {
        if (result.getStatus() == Result.Status.LOADING) {
            binding.textEta.setText("â€¦");
            return;
        }
        if (!result.hasData()) {
            String detail = result.getError() != null
                    ? "\n(" + result.getError().getMessage() + ")" : "";
            Toast.makeText(requireContext(),
                    getString(R.string.route_error) + detail, Toast.LENGTH_LONG).show();
            binding.buttonStart.setEnabled(false);
            return;
        }
        RouteInfo info = result.getData();
        binding.buttonStart.setEnabled(true);
        binding.textEta.setText(FormatUtil.duration(info.durationInTrafficSeconds));
        binding.textArrivalTime.setText(FormatUtil.clockTime(
                info.etaEpochMillis(System.currentTimeMillis())));
        binding.textDistance.setText(FormatUtil.distance(info.distanceMeters));

        if (map != null) {
            drawRoute(info);
        } else {
            pendingRoute = info;
        }
    }

    private void drawRoute(RouteInfo info) {
        if (map == null || info.path == null || info.path.isEmpty()) return;
        map.clear();
        MapUiUtil.drawStyledRoute(requireContext(), map, info.path);

        LatLng start = info.path.get(0);
        MarkerOptions startMarker = new MarkerOptions().position(start);
        com.mappls.sdk.maps.annotations.Icon selfIcon =
                MapUiUtil.markerIcon(requireContext(), R.drawable.ic_pin_self);
        if (selfIcon != null) startMarker.setIcon(selfIcon);
        map.addMarker(startMarker);

        LatLng end = info.path.get(info.path.size() - 1);
        MarkerOptions marker = new MarkerOptions().position(end);
        marker.setTitle(binding.textDestination.getText().toString());
        com.mappls.sdk.maps.annotations.Icon destIcon =
                MapUiUtil.markerIcon(requireContext(), R.drawable.ic_pin_destination);
        if (destIcon != null) marker.setIcon(destIcon);
        map.addMarker(marker);

        LatLngBounds.Builder bounds = new LatLngBounds.Builder();
        for (LatLng p : info.path) bounds.include(p);
        map.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds.build(), 96));
    }

    private void renderInsights(List<Recommendation> recs) {
        binding.chipsInsights.removeAllViews();
        if (recs == null) return;
        int shown = 0;
        for (Recommendation rec : recs) {
            if (shown++ >= 4) break;
            Chip chip = new Chip(requireContext());
            chip.setText(rec.title);
            chip.setEnsureMinTouchTargetSize(false);
            chip.setChipBackgroundColorResource(R.color.surface_container_high);
            chip.setTextColor(ContextCompat.getColor(requireContext(), R.color.on_surface));
            binding.chipsInsights.addView(chip);
        }
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
    }

    @Override
    public void onPause() {
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
        if (binding != null) binding.mapView.onDestroy();
        super.onDestroyView();
        binding = null;
        map = null;
    }
}

