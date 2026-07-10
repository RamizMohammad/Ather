package in.mohammad.ramiz.travel.ui.home;

import android.location.Location;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import in.mohammad.ramiz.travel.core.Result;
import in.mohammad.ramiz.travel.data.local.entity.SearchHistoryEntity;
import in.mohammad.ramiz.travel.data.repository.FavoritesRepository;
import in.mohammad.ramiz.travel.data.repository.LocationRepository;
import in.mohammad.ramiz.travel.data.repository.PlacesRepository;
import in.mohammad.ramiz.travel.data.repository.SettingsRepository;
import in.mohammad.ramiz.travel.data.repository.WeatherRepository;
import in.mohammad.ramiz.travel.domain.engine.RecommendationEngine;
import in.mohammad.ramiz.travel.domain.model.PlaceResult;
import in.mohammad.ramiz.travel.domain.model.Recommendation;
import in.mohammad.ramiz.travel.domain.model.TravelContext;
import in.mohammad.ramiz.travel.domain.model.WeatherSnapshot;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class HomeViewModel extends ViewModel {

    private final WeatherRepository weatherRepository;
    private final PlacesRepository placesRepository;
    private final LocationRepository locationRepository;
    private final RecommendationEngine recommendationEngine;
    private final SettingsRepository settingsRepository;
    private final FavoritesRepository favoritesRepository;

    private final MutableLiveData<WeatherSnapshot> weather = new MutableLiveData<>();
    private final MutableLiveData<List<Recommendation>> insights = new MutableLiveData<>();
    private final MutableLiveData<Result<List<PlaceResult>>> searchResults = new MutableLiveData<>();

    @Inject
    public HomeViewModel(WeatherRepository weatherRepository,
                         PlacesRepository placesRepository,
                         LocationRepository locationRepository,
                         RecommendationEngine recommendationEngine,
                         SettingsRepository settingsRepository,
                         FavoritesRepository favoritesRepository) {
        this.weatherRepository = weatherRepository;
        this.placesRepository = placesRepository;
        this.locationRepository = locationRepository;
        this.recommendationEngine = recommendationEngine;
        this.settingsRepository = settingsRepository;
        this.favoritesRepository = favoritesRepository;
    }

    public LiveData<WeatherSnapshot> getWeather() {
        return weather;
    }

    public LiveData<List<Recommendation>> getInsights() {
        return insights;
    }

    public LiveData<Result<List<PlaceResult>>> getSearchResults() {
        return searchResults;
    }

    public LiveData<Location> getLocation() {
        return locationRepository.observeLocation();
    }

    public LiveData<List<SearchHistoryEntity>> getRecentSearches() {
        return favoritesRepository.observeRecentSearches();
    }

    public void startLocation() {
        locationRepository.startUpdates(10_000);
    }

    /** Bias search ranking towards where the user currently is. */
    public void updateSearchBias(Location location) {
        placesRepository.setLocationBias(location);
    }

    public void refreshWeather(double lat, double lng) {
        weatherRepository.getCurrentWeather(lat, lng, result -> {
            if (result.hasData()) {
                weather.setValue(result.getData());
                TravelContext ctx = new TravelContext();
                ctx.currentWeather = result.getData();
                ctx.transportMode = settingsRepository.getTransportMode();
                insights.setValue(recommendationEngine.evaluate(ctx));
            }
        });
    }

    /**
     * Live destination search. Results are annotated with the straight-line
     * distance from the user's position and sorted closest-first.
     */
    public void search(String query) {
        placesRepository.searchDestination(query, result -> {
            List<PlaceResult> places = result.getData();
            Location here = locationRepository.getCachedLocation();
            if (places != null && here != null) {
                float[] out = new float[1];
                for (PlaceResult p : places) {
                    Location.distanceBetween(here.getLatitude(), here.getLongitude(),
                            p.lat, p.lng, out);
                    p.distanceMeters = out[0];
                }
                Collections.sort(places, Comparator.comparingDouble(p -> p.distanceMeters));
            }
            searchResults.setValue(result);
        });
    }
}

