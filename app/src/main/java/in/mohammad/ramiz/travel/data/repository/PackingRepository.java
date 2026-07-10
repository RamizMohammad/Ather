package in.mohammad.ramiz.travel.data.repository;

import androidx.lifecycle.LiveData;

import in.mohammad.ramiz.travel.core.AppExecutors;
import in.mohammad.ramiz.travel.data.local.dao.PackingDao;
import in.mohammad.ramiz.travel.data.local.entity.PackingEntity;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

/** Persistence for packing suggestions; generation lives in PackingEngine. */
@Singleton
public class PackingRepository {

    private final PackingDao packingDao;
    private final AppExecutors executors;

    @Inject
    public PackingRepository(PackingDao packingDao, AppExecutors executors) {
        this.packingDao = packingDao;
        this.executors = executors;
    }

    public LiveData<List<PackingEntity>> observeForJourney(long journeyId) {
        return packingDao.observeForJourney(journeyId);
    }

    /** Replace previous suggestions for the journey with a fresh generation. */
    public void replaceForJourney(long journeyId, List<PackingEntity> items) {
        executors.diskIO().execute(() -> {
            packingDao.deleteForJourney(journeyId);
            packingDao.insertAll(items);
        });
    }

    public void setPacked(long id, boolean packed) {
        executors.diskIO().execute(() -> packingDao.setPacked(id, packed));
    }

    public void dismiss(long id) {
        executors.diskIO().execute(() -> packingDao.dismiss(id));
    }
}

