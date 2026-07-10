package in.mohammad.ramiz.travel.data.repository;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import in.mohammad.ramiz.travel.BuildConfig;
import in.mohammad.ramiz.travel.core.AppExecutors;
import in.mohammad.ramiz.travel.core.Result;
import in.mohammad.ramiz.travel.data.local.dao.WeatherCacheDao;
import in.mohammad.ramiz.travel.data.local.entity.WeatherCacheEntity;
import in.mohammad.ramiz.travel.data.remote.api.WeatherApiService;
import in.mohammad.ramiz.travel.data.remote.dto.weather.ForecastDayDto;
import in.mohammad.ramiz.travel.data.remote.dto.weather.ForecastResponse;
import in.mohammad.ramiz.travel.data.remote.dto.weather.HourDto;
import in.mohammad.ramiz.travel.domain.model.WeatherSnapshot;
import in.mohammad.ramiz.travel.util.GeoUtil;
import com.google.gson.Gson;

import java.io.IOException;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.inject.Singleton;

import retrofit2.Response;

/**
 * Single source of truth for weather.
 * Strategy: Room cache first (30 min TTL). On miss/expiry -> network -> upsert cache.
 * On network failure -> serve expired cache marked stale. Rounded coordinate cells
 * (~1.1 km) keep both API quota and cache size small.
 */
@Singleton
public class WeatherRepository {

    private static final long TTL_MILLIS = TimeUnit.MINUTES.toMillis(30);
    private static final int FORECAST_DAYS = 2;

    private final WeatherApiService api;
    private final WeatherCacheDao cacheDao;
    private final Gson gson;
    private final AppExecutors executors;

    public interface Callback {
        void onResult(Result<WeatherSnapshot> result);
    }

    public interface ForecastCallback {
        void onResult(@Nullable ForecastResponse forecast, boolean stale, @Nullable Throwable error);
    }

    @Inject
    public WeatherRepository(WeatherApiService api, WeatherCacheDao cacheDao,
                             Gson gson, AppExecutors executors) {
        this.api = api;
        this.cacheDao = cacheDao;
        this.gson = gson;
        this.executors = executors;
    }

    /** Current conditions at a coordinate. Async; callback on main thread. */
    public void getCurrentWeather(double lat, double lng, @NonNull Callback callback) {
        executors.network().execute(() -> {
            ForecastFetch fetch = fetchForecastBlocking(lat, lng);
            if (fetch.forecast == null) {
                post(callback, Result.error(fetch.error, null));
                return;
            }
            WeatherSnapshot snap = toCurrentSnapshot(fetch.forecast, lat, lng);
            snap.stale = fetch.stale;
            post(callback, fetch.stale ? Result.stale(snap) : Result.success(snap));
        });
    }

    /** Conditions at a coordinate at a specific future time (nearest forecast hour). */
    public void getWeatherAt(double lat, double lng, long epochMillis, @NonNull Callback callback) {
        executors.network().execute(() -> {
            ForecastFetch fetch = fetchForecastBlocking(lat, lng);
            if (fetch.forecast == null) {
                post(callback, Result.error(fetch.error, null));
                return;
            }
            WeatherSnapshot snap = toHourSnapshot(fetch.forecast, lat, lng, epochMillis);
            if (snap == null) {
                snap = toCurrentSnapshot(fetch.forecast, lat, lng);
                snap.forTime = epochMillis;
            }
            snap.stale = fetch.stale;
            post(callback, fetch.stale ? Result.stale(snap) : Result.success(snap));
        });
    }

    /** Full forecast payload (used by workers and the timeline UI). */
    public void getForecast(double lat, double lng, @NonNull ForecastCallback callback) {
        executors.network().execute(() -> {
            ForecastFetch fetch = fetchForecastBlocking(lat, lng);
            executors.postToMain(() -> callback.onResult(fetch.forecast, fetch.stale, fetch.error));
        });
    }

    /** Blocking variant for WorkManager workers (already on a background thread). */
    @Nullable
    public ForecastResponse getForecastBlocking(double lat, double lng) {
        return fetchForecastBlocking(lat, lng).forecast;
    }

    public void clearExpired() {
        executors.diskIO().execute(() -> cacheDao.deleteExpired(System.currentTimeMillis()));
    }

    // ------------------------------------------------------------------

    private static class ForecastFetch {
        @Nullable ForecastResponse forecast;
        boolean stale;
        @Nullable Throwable error;
    }

    @NonNull
    private ForecastFetch fetchForecastBlocking(double lat, double lng) {
        ForecastFetch out = new ForecastFetch();
        String cellKey = GeoUtil.cellKey(lat, lng);
        long now = System.currentTimeMillis();

        WeatherCacheEntity cached = cacheDao.getByCellSync(cellKey);
        if (cached != null && cached.expiresAt > now) {
            ForecastResponse fromCache = fromCacheJson(cached.rawJson);
            if (fromCache != null) {
                out.forecast = fromCache;
                return out;
            }
            // Unreadable/legacy cache row: fall through to the network.
        }

        try {
            Response<ForecastResponse> response = api.getForecast(
                    BuildConfig.WEATHER_API_KEY,
                    String.format(Locale.US, "%.4f,%.4f", lat, lng),
                    FORECAST_DAYS, "yes", "yes").execute();
            if (response.isSuccessful() && response.body() != null) {
                ForecastResponse body = response.body();
                cacheDao.upsert(toCacheEntity(cellKey, lat, lng, body, now));
                out.forecast = body;
                return out;
            }
            out.error = new IOException("Weather API HTTP " + response.code());
        } catch (Exception e) {
            out.error = e;
        }

        // Fallback: serve expired cache as stale data.
        if (cached != null) {
            ForecastResponse fromCache = fromCacheJson(cached.rawJson);
            if (fromCache != null) {
                out.forecast = fromCache;
                out.stale = true;
            }
        }
        return out;
    }

    /** Tolerates cache rows written by other providers by returning null. */
    @Nullable
    private ForecastResponse fromCacheJson(@Nullable String rawJson) {
        if (rawJson == null) return null;
        try {
            ForecastResponse f = gson.fromJson(rawJson, ForecastResponse.class);
            return (f != null && f.current != null) ? f : null;
        } catch (Exception e) {
            return null;
        }
    }

    private WeatherCacheEntity toCacheEntity(String cellKey, double lat, double lng,
                                             ForecastResponse body, long now) {
        WeatherCacheEntity e = new WeatherCacheEntity();
        e.cellKey = cellKey;
        e.lat = lat;
        e.lng = lng;
        e.rawJson = gson.toJson(body);
        if (body.current != null) {
            e.tempC = body.current.tempC;
            e.feelsLikeC = body.current.feelsLikeC;
            e.windKph = body.current.windKph;
            e.uvIndex = body.current.uv;
            e.humidity = body.current.humidity;
            e.visibilityKm = body.current.visKm;
            if (body.current.condition != null) {
                e.conditionCode = body.current.condition.code;
                e.conditionText = body.current.condition.text;
            }
            if (body.current.airQuality != null) {
                e.aqiPm25 = body.current.airQuality.pm25;
            }
        }
        if (body.forecast != null && body.forecast.forecastDays != null
                && !body.forecast.forecastDays.isEmpty()
                && body.forecast.forecastDays.get(0).day != null) {
            e.rainChance = body.forecast.forecastDays.get(0).day.chanceOfRain;
        }
        e.fetchedAt = now;
        e.expiresAt = now + TTL_MILLIS;
        return e;
    }

    private WeatherSnapshot toCurrentSnapshot(ForecastResponse f, double lat, double lng) {
        WeatherSnapshot s = new WeatherSnapshot();
        s.lat = lat;
        s.lng = lng;
        s.forTime = System.currentTimeMillis();
        if (f.location != null) s.placeName = f.location.name;
        if (f.current != null) {
            s.tempC = f.current.tempC;
            s.feelsLikeC = f.current.feelsLikeC;
            s.windKph = f.current.windKph;
            s.gustKph = f.current.gustKph;
            s.humidity = f.current.humidity;
            s.uvIndex = f.current.uv;
            s.visibilityKm = f.current.visKm;
            s.precipMm = f.current.precipMm;
            if (f.current.condition != null) {
                s.conditionText = f.current.condition.text;
                s.conditionCode = f.current.condition.code;
            }
            if (f.current.airQuality != null) {
                s.aqiPm25 = f.current.airQuality.pm25;
                s.usEpaIndex = f.current.airQuality.usEpaIndex;
            }
        }
        ForecastDayDto today = firstDay(f);
        if (today != null) {
            if (today.day != null) {
                s.rainChancePercent = today.day.chanceOfRain;
                s.snowChancePercent = today.day.chanceOfSnow;
            }
            if (today.astro != null) {
                s.sunrise = today.astro.sunrise;
                s.sunset = today.astro.sunset;
            }
        }
        return s;
    }

    @Nullable
    private WeatherSnapshot toHourSnapshot(ForecastResponse f, double lat, double lng, long epochMillis) {
        if (f.forecast == null || f.forecast.forecastDays == null) return null;
        long targetSec = epochMillis / 1000L;
        HourDto best = null;
        long bestDiff = Long.MAX_VALUE;
        ForecastDayDto bestDay = null;
        for (ForecastDayDto day : f.forecast.forecastDays) {
            if (day.hours == null) continue;
            for (HourDto h : day.hours) {
                long diff = Math.abs(h.timeEpoch - targetSec);
                if (diff < bestDiff) {
                    bestDiff = diff;
                    best = h;
                    bestDay = day;
                }
            }
        }
        if (best == null) return null;

        WeatherSnapshot s = new WeatherSnapshot();
        s.lat = lat;
        s.lng = lng;
        s.forTime = epochMillis;
        if (f.location != null) s.placeName = f.location.name;
        s.tempC = best.tempC;
        s.feelsLikeC = best.feelsLikeC;
        s.windKph = best.windKph;
        s.gustKph = best.gustKph;
        s.humidity = best.humidity;
        s.uvIndex = best.uv;
        s.visibilityKm = best.visKm;
        s.precipMm = best.precipMm;
        s.rainChancePercent = best.chanceOfRain;
        s.snowChancePercent = best.chanceOfSnow;
        if (best.condition != null) {
            s.conditionText = best.condition.text;
            s.conditionCode = best.condition.code;
        }
        if (f.current != null && f.current.airQuality != null) {
            s.aqiPm25 = f.current.airQuality.pm25;
            s.usEpaIndex = f.current.airQuality.usEpaIndex;
        }
        if (bestDay.astro != null) {
            s.sunrise = bestDay.astro.sunrise;
            s.sunset = bestDay.astro.sunset;
        }
        return s;
    }

    @Nullable
    private ForecastDayDto firstDay(ForecastResponse f) {
        if (f.forecast != null && f.forecast.forecastDays != null && !f.forecast.forecastDays.isEmpty()) {
            return f.forecast.forecastDays.get(0);
        }
        return null;
    }

    private void post(Callback cb, Result<WeatherSnapshot> result) {
        executors.postToMain(() -> cb.onResult(result));
    }
}
