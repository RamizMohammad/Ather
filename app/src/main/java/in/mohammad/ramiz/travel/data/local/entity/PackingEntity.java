package in.mohammad.ramiz.travel.data.local.entity;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/**
 * One packing suggestion generated for a journey.
 * confidence: 0..100 - how strongly the engine believes this item is needed.
 * reason: human-readable explanation shown in the UI ("Temperatures drop to 8Â°C by 7 PM").
 * source: WEATHER_DRIVEN / TEMPERATURE_DRIVEN / PRECAUTIONARY / MODE_DRIVEN / DURATION_DRIVEN.
 */
@Entity(tableName = "packing_items",
        foreignKeys = @ForeignKey(
                entity = JourneyEntity.class,
                parentColumns = "id",
                childColumns = "journeyId",
                onDelete = ForeignKey.CASCADE),
        indices = {@Index("journeyId")})
public class PackingEntity {

    public static final String SOURCE_WEATHER = "WEATHER_DRIVEN";
    public static final String SOURCE_TEMPERATURE = "TEMPERATURE_DRIVEN";
    public static final String SOURCE_PRECAUTION = "PRECAUTIONARY";
    public static final String SOURCE_MODE = "MODE_DRIVEN";
    public static final String SOURCE_DURATION = "DURATION_DRIVEN";

    @PrimaryKey(autoGenerate = true)
    public long id;

    public long journeyId;

    public String itemName;
    public String reason;
    public String source;

    /** 0..100 */
    public int confidence;

    public boolean isPacked;
    public boolean isDismissed;

    public long createdAt;
}

