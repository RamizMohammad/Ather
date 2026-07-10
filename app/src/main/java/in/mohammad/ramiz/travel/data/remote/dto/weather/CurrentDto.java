package in.mohammad.ramiz.travel.data.remote.dto.weather;

import androidx.annotation.Nullable;

import com.google.gson.annotations.SerializedName;

/** WeatherAPI.com current conditions block. */
public class CurrentDto {

    @SerializedName("last_updated_epoch")
    public long lastUpdatedEpoch;

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

    @SerializedName("wind_dir")
    public String windDir;

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

    @SerializedName("pressure_mb")
    public double pressureMb;

    @SerializedName("is_day")
    public int isDay;

    @Nullable
    @SerializedName("condition")
    public ConditionDto condition;

    @Nullable
    @SerializedName("air_quality")
    public AirQualityDto airQuality;
}
