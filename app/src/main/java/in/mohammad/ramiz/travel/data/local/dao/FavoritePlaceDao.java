package in.mohammad.ramiz.travel.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import in.mohammad.ramiz.travel.data.local.entity.FavoritePlaceEntity;

import java.util.List;

@Dao
public interface FavoritePlaceDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(FavoritePlaceEntity place);

    @Delete
    void delete(FavoritePlaceEntity place);

    @Query("SELECT * FROM favorite_places ORDER BY type, name")
    LiveData<List<FavoritePlaceEntity>> observeAll();

    @Query("SELECT * FROM favorite_places WHERE type = :type LIMIT 1")
    FavoritePlaceEntity getByTypeSync(String type);
}

