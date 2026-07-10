package in.mohammad.ramiz.travel.data.remote.dto.weather;

import androidx.annotation.Nullable;

import com.google.gson.annotations.SerializedName;

/** WeatherAPI.com daily summary block. */
public class DayDto {

    @SerializedName("maxtemp_c")
    public double maxTempC;

    @SerializedName("mintemp_c")
    public double minTempC;

    @SerializedName("avgtemp_c")
    public double avgTempC;

    @SerializedName("maxwind_kph")
    public double maxWindKph;

    @SerializedName("totalprecip_mm")
    public double totalPrecipMm;

    @SerializedName("avgvis_km")
    public double avgVisKm;

    @SerializedName("avghumidity")
    public double avgHumidity;

    @SerializedName("daily_chance_of_rain")
    public double chanceOfRain;

    @SerializedName("daily_chance_of_snow")
    public double chanceOfSnow;

    @SerializedName("daily_will_it_rain")
    public int willItRain;

    @SerializedName("daily_will_it_snow")
    public int willItSnow;

    @SerializedName("uv")
    public double uv;

    @Nullable
    @SerializedName("condition")
    public ConditionDto condition;
}
