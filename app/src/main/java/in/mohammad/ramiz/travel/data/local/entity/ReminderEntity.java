package in.mohammad.ramiz.travel.data.local.entity;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/**
 * A location-based smart todo. Trigger types:
 * MINUTES_BEFORE_ARRIVAL (uses live ETA), ON_ARRIVAL, AFTER_LEAVING, RADIUS (geofence-like distance check).
 * Optionally attached to a journey.
 */
@Entity(tableName = "reminders",
        foreignKeys = @ForeignKey(
                entity = JourneyEntity.class,
                parentColumns = "id",
                childColumns = "journeyId",
                onDelete = ForeignKey.SET_NULL),
        indices = {@Index("journeyId"), @Index("isDone"), @Index("triggerType")})
public class ReminderEntity {

    public static final String TRIGGER_MINUTES_BEFORE = "MINUTES_BEFORE_ARRIVAL";
    public static final String TRIGGER_ON_ARRIVAL = "ON_ARRIVAL";
    public static final String TRIGGER_AFTER_LEAVING = "AFTER_LEAVING";
    public static final String TRIGGER_RADIUS = "RADIUS";

    @PrimaryKey(autoGenerate = true)
    public long id;

    public String title;
    public String note;

    public double placeLat;
    public double placeLng;
    public String placeName;

    public String triggerType;

    /** For MINUTES_BEFORE_ARRIVAL: 2, 5 or 10. */
    public int minutesBefore;

    /** For RADIUS trigger, meters. */
    public int radiusMeters;

    public Long journeyId;

    public boolean isDone;
    public boolean isNotified;

    public long createdAt;
    public long completedAt;
}

