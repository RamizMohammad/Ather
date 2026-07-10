package in.mohammad.ramiz.travel.data.local.entity;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/**
 * A journey from origin to destination. Status lifecycle:
 * PLANNED -> ACTIVE -> COMPLETED (or CANCELLED).
 */
@Entity(tableName = "journeys",
        indices = {@Index("status"), @Index("startedAt")})
public class JourneyEntity {

    public static final String STATUS_PLANNED = "PLANNED";
    public static final String STATUS_ACTIVE = "ACTIVE";
    public static final String STATUS_COMPLETED = "COMPLETED";
    public static final String STATUS_CANCELLED = "CANCELLED";

    public static final String MODE_CAR = "CAR";
    public static final String MODE_MOTORCYCLE = "MOTORCYCLE";

    @PrimaryKey(autoGenerate = true)
    public long id;

    public String title;

    public double originLat;
    public double originLng;
    public String originName;

    public double destLat;
    public double destLng;
    public String destName;

    /** CAR or MOTORCYCLE */
    public String transportMode;

    public String status;

    /** Encoded Google polyline of the chosen route. */
    public String routePolyline;

    public long plannedDepartureAt;
    public long startedAt;
    public long completedAt;

    /** Route metrics snapshot. */
    public int distanceMeters;
    public int durationSeconds;

    /** Live/accumulated stats. */
    @ColumnInfo(defaultValue = "0")
    public double travelledMeters;
    @ColumnInfo(defaultValue = "0")
    public double maxSpeedKmh;
    @ColumnInfo(defaultValue = "0")
    public double avgSpeedKmh;

    /** Weather summary encountered on the journey (short text, e.g. "Rain 12Â°C"). */
    public String weatherSummary;

    public long createdAt;
}

