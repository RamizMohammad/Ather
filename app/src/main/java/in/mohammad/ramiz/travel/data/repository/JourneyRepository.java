package in.mohammad.ramiz.travel.data.repository;

import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;

import in.mohammad.ramiz.travel.core.AppExecutors;
import in.mohammad.ramiz.travel.data.local.dao.JourneyDao;
import in.mohammad.ramiz.travel.data.local.entity.JourneyEntity;

import java.util.List;
import java.util.function.Consumer;

import javax.inject.Inject;
import javax.inject.Singleton;

/** Journey lifecycle + history. Pure local data; no network involved. */
@Singleton
public class JourneyRepository {

    private final JourneyDao journeyDao;
    private final AppExecutors executors;

    @Inject
    public JourneyRepository(JourneyDao journeyDao, AppExecutors executors) {
        this.journeyDao = journeyDao;
        this.executors = executors;
    }

    public LiveData<JourneyEntity> observeActive() {
        return journeyDao.observeActive();
    }

    public LiveData<List<JourneyEntity>> observeHistory() {
        return journeyDao.observeHistory();
    }

    public LiveData<JourneyEntity> observeById(long id) {
        return journeyDao.observeById(id);
    }

    public void create(JourneyEntity journey, Consumer<Long> onCreated) {
        executors.diskIO().execute(() -> {
            journey.createdAt = System.currentTimeMillis();
            journey.status = JourneyEntity.STATUS_PLANNED;
            long id = journeyDao.insert(journey);
            executors.postToMain(() -> onCreated.accept(id));
        });
    }

    public void start(long journeyId) {
        executors.diskIO().execute(() -> {
            JourneyEntity j = journeyDao.getByIdSync(journeyId);
            if (j == null) return;
            j.status = JourneyEntity.STATUS_ACTIVE;
            j.startedAt = System.currentTimeMillis();
            journeyDao.update(j);
        });
    }

    /** Called by NavigationService with accumulated live stats. */
    public void updateLiveStats(long journeyId, double travelledMeters,
                                double maxSpeedKmh, double avgSpeedKmh) {
        executors.diskIO().execute(() -> {
            JourneyEntity j = journeyDao.getByIdSync(journeyId);
            if (j == null) return;
            j.travelledMeters = travelledMeters;
            j.maxSpeedKmh = maxSpeedKmh;
            j.avgSpeedKmh = avgSpeedKmh;
            journeyDao.update(j);
        });
    }

    public void complete(long journeyId, @Nullable String weatherSummary) {
        executors.diskIO().execute(() -> {
            JourneyEntity j = journeyDao.getByIdSync(journeyId);
            if (j == null) return;
            j.status = JourneyEntity.STATUS_COMPLETED;
            j.completedAt = System.currentTimeMillis();
            if (weatherSummary != null) j.weatherSummary = weatherSummary;
            journeyDao.update(j);
        });
    }

    public void cancel(long journeyId) {
        executors.diskIO().execute(() -> journeyDao.updateStatus(journeyId, JourneyEntity.STATUS_CANCELLED));
    }

    public void getActiveAsync(Consumer<JourneyEntity> consumer) {
        executors.diskIO().execute(() -> {
            JourneyEntity j = journeyDao.getActiveSync();
            executors.postToMain(() -> consumer.accept(j));
        });
    }

    public JourneyDao dao() {
        return journeyDao;
    }
}

