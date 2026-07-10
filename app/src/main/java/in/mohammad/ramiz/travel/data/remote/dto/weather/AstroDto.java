package in.mohammad.ramiz.travel.data.remote.dto.weather;

import com.google.gson.annotations.SerializedName;

/** WeatherAPI.com astronomy block (times as "06:12 AM" strings). */
public class AstroDto {

    @SerializedName("sunrise")
    public String sunrise;

    @SerializedName("sunset")
    public String sunset;

    @SerializedName("moonrise")
    public String moonrise;

    @SerializedName("moonset")
    public String moonset;

    @SerializedName("moon_phase")
    public String moonPhase;
}
