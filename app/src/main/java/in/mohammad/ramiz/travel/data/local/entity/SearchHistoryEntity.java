package in.mohammad.ramiz.travel.data.local.entity;

import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/** Destination search history, newest first, pruned to 50 entries by a cleanup worker. */
@Entity(tableName = "search_history", indices = {@Index("searchedAt")})
public class SearchHistoryEntity {

    @PrimaryKey(autoGenerate = true)
    public long id;

    public String query;
    public String resolvedName;
    public double lat;
    public double lng;
    public long searchedAt;
}

