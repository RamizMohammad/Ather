package in.mohammad.ramiz.travel.data.remote.dto.weather;

import com.google.gson.annotations.SerializedName;

/** WeatherAPI.com air quality block (concentrations in ug/m3 + US EPA index 1..6). */
public class AirQualityDto {

    @SerializedName("co")
    public double co;

    @SerializedName("no2")
    public double no2;

    @SerializedName("o3")
    public double o3;

    @SerializedName("so2")
    public double so2;

    @SerializedName("pm2_5")
    public double pm25;

    @SerializedName("pm10")
    public double pm10;

    /** 1 Good .. 6 Hazardous. */
    @SerializedName("us-epa-index")
    public int usEpaIndex;
}
