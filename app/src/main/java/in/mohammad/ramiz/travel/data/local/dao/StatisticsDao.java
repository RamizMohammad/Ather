package in.mohammad.ramiz.travel.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import in.mohammad.ramiz.travel.data.local.entity.StatisticsEntity;

import java.util.List;

@Dao
public interface StatisticsDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void upsert(StatisticsEntity stats);

    @Query("SELECT * FROM statistics WHERE dayKey = :dayKey LIMIT 1")
    StatisticsEntity getByDaySync(int dayKey);

    @Query("SELECT * FROM statistics ORDER BY dayKey DESC LIMIT 30")
    LiveData<List<StatisticsEntity>> observeLast30Days();

    @Query("SELECT SUM(distanceMeters) FROM statistics")
    LiveData<Double> observeTotalDistance();

    @Query("SELECT SUM(journeysCompleted) FROM statistics")
    LiveData<Integer> observeTotalJourneys();
}

