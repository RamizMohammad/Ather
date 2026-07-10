package in.mohammad.ramiz.travel.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import in.mohammad.ramiz.travel.data.local.entity.JourneyEntity;

import java.util.List;

@Dao
public interface JourneyDao {

    @Insert
    long insert(JourneyEntity journey);

    @Update
    void update(JourneyEntity journey);

    @Query("SELECT * FROM journeys WHERE id = :id")
    JourneyEntity getByIdSync(long id);

    @Query("SELECT * FROM journeys WHERE id = :id")
    LiveData<JourneyEntity> observeById(long id);

    @Query("SELECT * FROM journeys WHERE status = 'ACTIVE' LIMIT 1")
    JourneyEntity getActiveSync();

    @Query("SELECT * FROM journeys WHERE status = 'ACTIVE' LIMIT 1")
    LiveData<JourneyEntity> observeActive();

    @Query("SELECT * FROM journeys WHERE status = 'COMPLETED' ORDER BY completedAt DESC")
    LiveData<List<JourneyEntity>> observeHistory();

    @Query("SELECT * FROM journeys WHERE status = 'PLANNED' ORDER BY plannedDepartureAt ASC")
    LiveData<List<JourneyEntity>> observePlanned();

    @Query("UPDATE journeys SET status = :status WHERE id = :id")
    void updateStatus(long id, String status);

    @Query("DELETE FROM journeys WHERE status = 'CANCELLED' AND createdAt < :olderThan")
    int deleteCancelledOlderThan(long olderThan);

    @Query("SELECT * FROM journeys WHERE status = 'COMPLETED' AND completedAt BETWEEN :from AND :to")
    List<JourneyEntity> getCompletedBetweenSync(long from, long to);
}

