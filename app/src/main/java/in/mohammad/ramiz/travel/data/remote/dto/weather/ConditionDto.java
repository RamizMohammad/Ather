package in.mohammad.ramiz.travel.data.remote.dto.weather;

import com.google.gson.annotations.SerializedName;

/** WeatherAPI.com condition descriptor (text + numeric code + icon). */
public class ConditionDto {

    @SerializedName("text")
    public String text;

    @SerializedName("icon")
    public String icon;

    @SerializedName("code")
    public int code;
}
