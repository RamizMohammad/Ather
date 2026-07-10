package in.mohammad.ramiz.travel.data.remote.dto.weather;

import androidx.annotation.Nullable;

import com.google.gson.annotations.SerializedName;

/**
 * Response of WeatherAPI.com forecast.json: current conditions + multi-day
 * forecast (daily/astro/hourly) + air quality in a single call.
 */
public class ForecastResponse {

    @Nullable
    @SerializedName("location")
    public LocationDto location;

    @Nullable
    @SerializedName("current")
    public CurrentDto current;

    @Nullable
    @SerializedName("forecast")
    public ForecastDto forecast;
}
