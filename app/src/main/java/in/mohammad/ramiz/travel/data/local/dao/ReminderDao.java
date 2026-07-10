package in.mohammad.ramiz.travel.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import in.mohammad.ramiz.travel.data.local.entity.ReminderEntity;

import java.util.List;

@Dao
public interface ReminderDao {

    @Insert
    long insert(ReminderEntity reminder);

    @Update
    void update(ReminderEntity reminder);

    @Delete
    void delete(ReminderEntity reminder);

    @Query("SELECT * FROM reminders WHERE isDone = 0 ORDER BY createdAt DESC")
    LiveData<List<ReminderEntity>> observeActive();

    @Query("SELECT * FROM reminders WHERE isDone = 0 AND isNotified = 0")
    List<ReminderEntity> getPendingSync();

    @Query("SELECT * FROM reminders WHERE journeyId = :journeyId")
    List<ReminderEntity> getForJourneySync(long journeyId);

    @Query("UPDATE reminders SET isDone = 1, completedAt = :at WHERE id = :id")
    void markDone(long id, long at);

    @Query("UPDATE reminders SET isNotified = 1 WHERE id = :id")
    void markNotified(long id);

    @Query("SELECT COUNT(*) FROM reminders WHERE isDone = 1 AND completedAt BETWEEN :from AND :to")
    int countCompletedBetweenSync(long from, long to);
}

