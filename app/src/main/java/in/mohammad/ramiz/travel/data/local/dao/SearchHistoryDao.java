package in.mohammad.ramiz.travel.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import in.mohammad.ramiz.travel.data.local.entity.SearchHistoryEntity;

import java.util.List;

@Dao
public interface SearchHistoryDao {

    @Insert
    void insert(SearchHistoryEntity entry);

    @Query("SELECT * FROM search_history ORDER BY searchedAt DESC LIMIT 10")
    LiveData<List<SearchHistoryEntity>> observeRecent();

    @Query("DELETE FROM search_history WHERE id NOT IN (SELECT id FROM search_history ORDER BY searchedAt DESC LIMIT 50)")
    void pruneToLimit();

    @Query("DELETE FROM search_history")
    void clearAll();
}

