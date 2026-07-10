package in.mohammad.ramiz.travel.data.remote.dto.weather;

import androidx.annotation.Nullable;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/** WeatherAPI.com forecast container. */
public class ForecastDto {

    @Nullable
    @SerializedName("forecastday")
    public List<ForecastDayDto> forecastDays;
}
