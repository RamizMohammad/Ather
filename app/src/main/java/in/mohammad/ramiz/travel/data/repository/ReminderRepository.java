package in.mohammad.ramiz.travel.data.repository;

import androidx.lifecycle.LiveData;

import in.mohammad.ramiz.travel.core.AppExecutors;
import in.mohammad.ramiz.travel.data.local.dao.ReminderDao;
import in.mohammad.ramiz.travel.data.local.entity.ReminderEntity;

import java.util.List;
import java.util.function.Consumer;

import javax.inject.Inject;
import javax.inject.Singleton;

/** Smart todo storage. Trigger evaluation lives in ReminderEngine. */
@Singleton
public class ReminderRepository {

    private final ReminderDao reminderDao;
    private final AppExecutors executors;

    @Inject
    public ReminderRepository(ReminderDao reminderDao, AppExecutors executors) {
        this.reminderDao = reminderDao;
        this.executors = executors;
    }

    public LiveData<List<ReminderEntity>> observeActive() {
        return reminderDao.observeActive();
    }

    public void add(ReminderEntity reminder) {
        executors.diskIO().execute(() -> {
            reminder.createdAt = System.currentTimeMillis();
            reminderDao.insert(reminder);
        });
    }

    public void markDone(long id) {
        executors.diskIO().execute(() -> reminderDao.markDone(id, System.currentTimeMillis()));
    }

    public void markNotified(long id) {
        executors.diskIO().execute(() -> reminderDao.markNotified(id));
    }

    public void delete(ReminderEntity reminder) {
        executors.diskIO().execute(() -> reminderDao.delete(reminder));
    }

    public void getPendingAsync(Consumer<List<ReminderEntity>> consumer) {
        executors.diskIO().execute(() -> {
            List<ReminderEntity> pending = reminderDao.getPendingSync();
            executors.postToMain(() -> consumer.accept(pending));
        });
    }

    /** Blocking accessor for workers/services already off the main thread. */
    public List<ReminderEntity> getPendingBlocking() {
        return reminderDao.getPendingSync();
    }

    public ReminderDao dao() {
        return reminderDao;
    }
}

