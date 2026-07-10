package in.mohammad.ramiz.travel.data.remote.dto.weather;

import com.google.gson.annotations.SerializedName;

/** WeatherAPI.com resolved location block. */
public class LocationDto {

    @SerializedName("name")
    public String name;

    @SerializedName("region")
    public String region;

    @SerializedName("country")
    public String country;

    @SerializedName("lat")
    public double lat;

    @SerializedName("lon")
    public double lon;

    @SerializedName("tz_id")
    public String tzId;

    @SerializedName("localtime_epoch")
    public long localtimeEpoch;
}
