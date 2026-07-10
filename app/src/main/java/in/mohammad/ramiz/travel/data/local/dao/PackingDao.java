package in.mohammad.ramiz.travel.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import in.mohammad.ramiz.travel.data.local.entity.PackingEntity;

import java.util.List;

@Dao
public interface PackingDao {

    @Insert
    void insertAll(List<PackingEntity> items);

    @Query("SELECT * FROM packing_items WHERE journeyId = :journeyId AND isDismissed = 0 ORDER BY confidence DESC")
    LiveData<List<PackingEntity>> observeForJourney(long journeyId);

    @Query("DELETE FROM packing_items WHERE journeyId = :journeyId")
    void deleteForJourney(long journeyId);

    @Query("UPDATE packing_items SET isPacked = :packed WHERE id = :id")
    void setPacked(long id, boolean packed);

    @Query("UPDATE packing_items SET isDismissed = 1 WHERE id = :id")
    void dismiss(long id);

    @Query("SELECT COUNT(*) FROM packing_items WHERE isPacked = 1 AND createdAt BETWEEN :from AND :to")
    int countPackedBetweenSync(long from, long to);
}

