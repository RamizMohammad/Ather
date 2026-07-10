package in.mohammad.ramiz.travel.data.local.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import in.mohammad.ramiz.travel.data.local.entity.WeatherCacheEntity;

@Dao
public interface WeatherCacheDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void upsert(WeatherCacheEntity entity);

    @Query("SELECT * FROM weather_cache WHERE cellKey = :cellKey LIMIT 1")
    WeatherCacheEntity getByCellSync(String cellKey);

    @Query("DELETE FROM weather_cache WHERE expiresAt < :now")
    int deleteExpired(long now);

    @Query("DELETE FROM weather_cache")
    void clearAll();
}

