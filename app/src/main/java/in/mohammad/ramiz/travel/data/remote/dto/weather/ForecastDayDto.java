package in.mohammad.ramiz.travel.data.remote.dto.weather;

import androidx.annotation.Nullable;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/** WeatherAPI.com one forecast day: daily summary + astro + hourly entries. */
public class ForecastDayDto {

    @SerializedName("date")
    public String date;

    @SerializedName("date_epoch")
    public long dateEpoch;

    @Nullable
    @SerializedName("day")
    public DayDto day;

    @Nullable
    @SerializedName("astro")
    public AstroDto astro;

    @Nullable
    @SerializedName("hour")
    public List<HourDto> hours;
}
