package in.mohammad.ramiz.travel.data.remote.dto.weather;

import androidx.annotation.Nullable;

import com.google.gson.annotations.SerializedName;

/** WeatherAPI.com hourly forecast entry. */
public class HourDto {

    @SerializedName("time_epoch")
    public long timeEpoch;

    @SerializedName("temp_c")
    public double tempC;

    @SerializedName("feelslike_c")
    public double feelsLikeC;

    @SerializedName("wind_kph")
    public double windKph;

    @SerializedName("gust_kph")
    public double gustKph;

    @SerializedName("wind_degree")
    public double windDegree;

    @SerializedName("humidity")
    public double humidity;

    @SerializedName("cloud")
    public double cloud;

    @SerializedName("uv")
    public double uv;

    @SerializedName("vis_km")
    public double visKm;

    @SerializedName("precip_mm")
    public double precipMm;

    @SerializedName("chance_of_rain")
    public double chanceOfRain;

    @SerializedName("chance_of_snow")
    public double chanceOfSnow;

    @SerializedName("will_it_rain")
    public int willItRain;

    @SerializedName("will_it_snow")
    public int willItSnow;

    @SerializedName("is_day")
    public int isDay;

    @Nullable
    @SerializedName("condition")
    public ConditionDto condition;
}
