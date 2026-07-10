package in.mohammad.ramiz.travel.data.local.entity;

import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/**
 * Aggregated statistics per calendar day (yyyyMMdd key), recomputed by StatisticsWorker.
 * Keeping daily rows lets the UI build weekly/monthly charts with simple SUM queries.
 */
@Entity(tableName = "statistics", indices = {@Index(value = "dayKey", unique = true)})
public class StatisticsEntity {

    @PrimaryKey(autoGenerate = true)
    public long id;

    /** e.g. 20260709 */
    public int dayKey;

    public int journeysCompleted;
    public double distanceMeters;
    public long travelTimeSeconds;
    public double avgSpeedKmh;
    public int remindersCompleted;
    public int itemsPacked;

    public long updatedAt;
}

