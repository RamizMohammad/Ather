package in.mohammad.ramiz.travel.data.local.entity;

import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/** Saved place: Home, Work, or custom favorites. */
@Entity(tableName = "favorite_places", indices = {@Index(value = {"lat", "lng"}, unique = true)})
public class FavoritePlaceEntity {

    public static final String TYPE_HOME = "HOME";
    public static final String TYPE_WORK = "WORK";
    public static final String TYPE_CUSTOM = "CUSTOM";

    @PrimaryKey(autoGenerate = true)
    public long id;

    public String name;
    public String address;
    public double lat;
    public double lng;
    public String type;
    public long createdAt;
}

