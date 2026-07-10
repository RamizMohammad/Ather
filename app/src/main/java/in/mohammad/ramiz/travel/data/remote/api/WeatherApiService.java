package in.mohammad.ramiz.travel.data.remote.api;

import in.mohammad.ramiz.travel.data.remote.dto.weather.ForecastResponse;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

/**
 * WeatherAPI.com. forecast.json returns current conditions + hourly forecast + astro
 * (sunrise/sunset) + air quality in a single call, which keeps quota usage minimal.
 */
public interface WeatherApiService {

    @GET("forecast.json")
    Call<ForecastResponse> getForecast(
            @Query("key") String apiKey,
            @Query("q") String latLng,          // "48.85,2.35"
            @Query("days") int days,            // 1..3
            @Query("aqi") String aqi,           // "yes"
            @Query("alerts") String alerts);    // "yes"
}
