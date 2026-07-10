package in.mohammad.ramiz.travel.ui.route;

import android.location.Location;

import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import in.mohammad.ramiz.travel.core.Result;
import in.mohammad.ramiz.travel.data.local.entity.JourneyEntity;
import in.mohammad.ramiz.travel.data.repository.DirectionsRepository;
import in.mohammad.ramiz.travel.data.repository.JourneyRepository;
import in.mohammad.ramiz.travel.data.repository.LocationRepository;
import in.mohammad.ramiz.travel.data.repository.PackingRepository;
import in.mohammad.ramiz.travel.data.repository.SettingsRepository;
import in.mohammad.ramiz.travel.data.repository.WeatherRepository;
import in.mohammad.ramiz.travel.domain.engine.PackingEngine;
import in.mohammad.ramiz.travel.domain.engine.RecommendationEngine;
import in.mohammad.ramiz.travel.domain.model.Recommendation;
import in.mohammad.ramiz.travel.domain.model.RouteInfo;
import in.mohammad.ramiz.travel.domain.model.TravelContext;
import in.mohammad.ramiz.travel.domain.model.WeatherSnapshot;

import java.util.List;
import java.util.function.Consumer;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

/**
 * Route preview orchestration: route -> current + arrival weather -> insights.
 * On "Start Journey": persist journey, generate packing list, hand off to NavigationService.
 */
@HiltViewModel
public class RouteViewModel extends ViewModel {

    private final DirectionsRepository directionsRepository;
    private final WeatherRepository weatherRepository;
    private final LocationRepository locationRepository;
    private final JourneyRepository journeyRepository;
    private final PackingRepository packingRepository;
    private final PackingEngine packingEngine;
    private final RecommendationEngine recommendationEngine;
    private final SettingsRepository settingsRepository;

    private final MutableLiveData<Result<RouteInfo>> route = new MutableLiveData<>();
    private final MutableLiveData<WeatherSnapshot> currentWeather = new MutableLiveData<>();
    private final MutableLiveData<WeatherSnapshot> arrivalWeather = new MutableLiveData<>();
    private final MutableLiveData<List<Recommendation>> insights = new MutableLiveData<>();

    private double destLat, destLng;
    private String destName;

    @Inject
    public RouteViewModel(DirectionsRepository directionsRepository,
                          WeatherRepository weatherRepository,
                          LocationRepository locationRepository,
                          JourneyRepository journeyRepository,
                          PackingRepository packingRepository,
                          PackingEngine packingEngine,
                          RecommendationEngine recommendationEngine,
                          SettingsRepository settingsRepository) {
        this.directionsRepository = directionsRepository;
        this.weatherRepository = weatherRepository;
        this.locationRepository = locationRepository;
        this.journeyRepository = journeyRepository;
        this.packingRepository = packingRepository;
        this.packingEngine = packingEngine;
        this.recommendationEngine = recommendationEngine;
        this.settingsRepository = settingsRepository;
    }

    public LiveData<Result<RouteInfo>> getRoute() {
        return route;
    }

    public LiveData<WeatherSnapshot> getCurrentWeather() {
        return currentWeather;
    }

    public LiveData<WeatherSnapshot> getArrivalWeather() {
        return arrivalWeather;
    }

    public LiveData<List<Recommendation>> getInsights() {
        return insights;
    }

    @Nullable
    public Location getCurrentLocation() {
        return locationRepository.getCachedLocation();
    }

    /** Live location stream, used to show "you are here" when no destination is set. */
    public LiveData<Location> getLocationUpdates() {
        return locationRepository.observeLocation();
    }

    public void startLocationUpdates() {
        locationRepository.startUpdates(10_000);
    }

    public void load(double destLat, double destLng, String destName) {
        this.destLat = destLat;
        this.destLng = destLng;
        this.destName = destName;

        Location origin = locationRepository.getCachedLocation();
        if (origin == null) {
            route.setValue(Result.error(new IllegalStateException("No location fix yet"), null));
            return;
        }
        route.setValue(Result.loading());
        boolean motorcycle = in.mohammad.ramiz.travel.data.local.entity.JourneyEntity.MODE_MOTORCYCLE
                .equals(settingsRepository.getTransportMode());
        directionsRepository.getRoute(origin.getLatitude(), origin.getLongitude(),
                destLat, destLng, motorcycle, result -> {
                    route.setValue(result);
                    if (result.hasData()) {
                        loadWeather(origin, result.getData());
                    }
                });
    }

    private void loadWeather(Location origin, RouteInfo info) {
        weatherRepository.getCurrentWeather(origin.getLatitude(), origin.getLongitude(), r -> {
            if (r.hasData()) {
                currentWeather.setValue(r.getData());
                evaluate(info);
            }
        });
        long eta = info.etaEpochMillis(System.currentTimeMillis());
        weatherRepository.getWeatherAt(destLat, destLng, eta, r -> {
            if (r.hasData()) {
                arrivalWeather.setValue(r.getData());
                evaluate(info);
            }
        });
    }

    private void evaluate(RouteInfo info) {
        TravelContext ctx = buildContext(info);
        insights.setValue(recommendationEngine.evaluate(ctx));
    }

    private TravelContext buildContext(RouteInfo info) {
        TravelContext ctx = new TravelContext();
        ctx.currentWeather = currentWeather.getValue();
        ctx.arrivalWeather = arrivalWeather.getValue();
        ctx.transportMode = settingsRepository.getTransportMode();
        ctx.routeDistanceMeters = info.distanceMeters;
        ctx.routeDurationSeconds = info.durationInTrafficSeconds;
        ctx.departureEpochMillis = System.currentTimeMillis();
        ctx.trafficDelaySeconds = Math.max(0, info.durationInTrafficSeconds - info.durationSeconds);
        return ctx;
    }

    /** Persists the journey + packing list, then invokes onReady(journeyId). */
    public void startJourney(Consumer<Long> onReady) {
        Result<RouteInfo> r = route.getValue();
        Location origin = locationRepository.getCachedLocation();
        if (r == null || !r.hasData() || origin == null) return;
        RouteInfo info = r.getData();

        JourneyEntity journey = new JourneyEntity();
        journey.title = destName;
        journey.originLat = origin.getLatitude();
        journey.originLng = origin.getLongitude();
        journey.originName = info.startAddress;
        journey.destLat = destLat;
        journey.destLng = destLng;
        journey.destName = destName;
        journey.transportMode = settingsRepository.getTransportMode();
        journey.routePolyline = info.encodedPolyline;
        journey.plannedDepartureAt = System.currentTimeMillis();
        journey.distanceMeters = info.distanceMeters;
        journey.durationSeconds = info.durationInTrafficSeconds;

        journeyRepository.create(journey, journeyId -> {
            packingRepository.replaceForJourney(journeyId,
                    packingEngine.generate(journeyId, buildContext(info)));
            onReady.accept(journeyId);
        });
    }
}

