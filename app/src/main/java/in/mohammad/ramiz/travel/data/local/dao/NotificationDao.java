package in.mohammad.ramiz.travel.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import in.mohammad.ramiz.travel.data.local.entity.NotificationEntity;

import java.util.List;

@Dao
public interface NotificationDao {

    @Insert
    long insert(NotificationEntity notification);

    @Query("SELECT * FROM notifications ORDER BY shownAt DESC LIMIT 100")
    LiveData<List<NotificationEntity>> observeRecent();

    /** Cooldown check: when was this type last shown? */
    @Query("SELECT MAX(shownAt) FROM notifications WHERE type = :type")
    Long lastShownForTypeSync(String type);

    @Query("UPDATE notifications SET dismissed = 1 WHERE id = :id")
    void markDismissed(long id);

    @Query("DELETE FROM notifications WHERE shownAt < :olderThan")
    int deleteOlderThan(long olderThan);
}

