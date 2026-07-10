package in.mohammad.ramiz.travel.data.local.entity;

import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/**
 * Cached WeatherAPI.com forecast response for a rounded coordinate cell.
 * cellKey = round(lat,2) + "," + round(lng,2)  (~1.1 km grid) so nearby
 * requests share one cache row. rawJson holds the full forecast payload;
 * hot fields are denormalized for fast rule evaluation without JSON parsing.
 */
@Entity(tableName = "weather_cache", indices = {@Index(value = "cellKey", unique = true)})
public class WeatherCacheEntity {

    @PrimaryKey(autoGenerate = true)
    public long id;

    public String cellKey;

    public double lat;
    public double lng;

    /** Full Gson payload of ForecastResponse for offline re-hydration. */
    public String rawJson;

    /** Denormalized "hot" fields. */
    public double tempC;
    public double feelsLikeC;
    public double windKph;
    public double uvIndex;
    public double humidity;
    public double visibilityKm;
    public int conditionCode;
    public String conditionText;
    public double rainChance;
    public double aqiPm25;

    public long fetchedAt;
    public long expiresAt;
}

