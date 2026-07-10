package in.mohammad.ramiz.travel.data.local.entity;

import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/**
 * Log of every notification Aether has shown. Used for:
 * 1. the in-app assistant timeline, 2. cooldown suppression (same type not repeated
 * within its cooldown window), 3. statistics.
 */
@Entity(tableName = "notifications", indices = {@Index("type"), @Index("shownAt")})
public class NotificationEntity {

    @PrimaryKey(autoGenerate = true)
    public long id;

    /** e.g. RAIN_UMBRELLA, UV_HIGH, TRAFFIC_HEAVY, REMINDER, ARRIVAL, STORM */
    public String type;

    public String title;
    public String message;

    /** SILENT / DEFAULT / HEADS_UP */
    public String priority;

    public Long journeyId;

    public boolean dismissed;

    public long shownAt;
}

