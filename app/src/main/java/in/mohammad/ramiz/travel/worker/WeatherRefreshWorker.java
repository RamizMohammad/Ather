package in.mohammad.ramiz.travel.worker;

import android.content.Context;
import android.location.Location;

import androidx.annotation.NonNull;
import androidx.hilt.work.HiltWorker;
import androidx.work.ListenableWorker;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import in.mohammad.ramiz.travel.data.repository.LocationRepository;
import in.mohammad.ramiz.travel.data.repository.SettingsRepository;
import in.mohammad.ramiz.travel.data.repository.WeatherRepository;
import in.mohammad.ramiz.travel.data.remote.dto.weather.ForecastResponse;
import in.mohammad.ramiz.travel.domain.engine.NotificationEngine;
import in.mohammad.ramiz.travel.domain.engine.RecommendationEngine;
import in.mohammad.ramiz.travel.domain.model.Recommendation;
import in.mohammad.ramiz.travel.domain.model.TravelContext;
import in.mohammad.ramiz.travel.domain.model.WeatherSnapshot;

import dagger.assisted.Assisted;
import dagger.assisted.AssistedInject;

/**
 * Every 30 min: refresh the weather cache for the last known position and
 * run the rule engine, posting any newly firing recommendations
 * (cooldowns inside NotificationEngine keep this quiet).
 */
@HiltWorker
public class WeatherRefreshWorker extends Worker {

    private final WeatherRepository weatherRepository;
    private final LocationRepository locationRepository;
    private final RecommendationEngine recommendationEngine;
    private final NotificationEngine notificationEngine;
    private final SettingsRepository settingsRepository;

    @AssistedInject
    public WeatherRefreshWorker(@Assisted @NonNull Context context,
                                @Assisted @NonNull WorkerParameters params,
                                WeatherRepository weatherRepository,
                                LocationRepository locationRepository,
                                RecommendationEngine recommendationEngine,
                                NotificationEngine notificationEngine,
                                SettingsRepository settingsRepository) {
        super(context, params);
        this.weatherRepository = weatherRepository;
        this.locationRepository = locationRepository;
        this.recommendationEngine = recommendationEngine;
        this.notificationEngine = notificationEngine;
        this.settingsRepository = settingsRepository;
    }

    @NonNull
    @Override
    public ListenableWorker.Result doWork() {
        Location loc = locationRepository.getCachedLocation();
        if (loc == null) return ListenableWorker.Result.success(); // nothing to refresh yet

        ForecastResponse forecast = weatherRepository.getForecastBlocking(
                loc.getLatitude(), loc.getLongitude());
        if (forecast == null) return ListenableWorker.Result.retry();

        if (!settingsRepository.areInsightNotificationsEnabled()) return ListenableWorker.Result.success();

        // Evaluate ambient (non-journey) rules on current conditions.
        WeatherSnapshot current = new WeatherSnapshot();
        if (forecast.current != null) {
            current.tempC = forecast.current.tempC;
            current.windKph = forecast.current.windKph;
            current.gustKph = forecast.current.gustKph;
            current.uvIndex = forecast.current.uv;
            current.visibilityKm = forecast.current.visKm;
            current.humidity = forecast.current.humidity;
            if (forecast.current.condition != null) {
                current.conditionCode = forecast.current.condition.code;
                current.conditionText = forecast.current.condition.text;
            }
            if (forecast.current.airQuality != null) {
                current.aqiPm25 = forecast.current.airQuality.pm25;
                current.usEpaIndex = forecast.current.airQuality.usEpaIndex;
            }
        }
        if (forecast.forecast != null && forecast.forecast.forecastDays != null
                && !forecast.forecast.forecastDays.isEmpty()
                && forecast.forecast.forecastDays.get(0).day != null) {
            current.rainChancePercent = forecast.forecast.forecastDays.get(0).day.chanceOfRain;
            current.snowChancePercent = forecast.forecast.forecastDays.get(0).day.chanceOfSnow;
        }

        TravelContext ctx = new TravelContext();
        ctx.currentWeather = current;
        ctx.transportMode = settingsRepository.getTransportMode();

        for (Recommendation rec : recommendationEngine.evaluate(ctx)) {
            notificationEngine.postRecommendation(rec, null);
        }
        return ListenableWorker.Result.success();
    }
}

