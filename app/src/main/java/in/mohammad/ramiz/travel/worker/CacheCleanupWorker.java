package in.mohammad.ramiz.travel.worker;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.hilt.work.HiltWorker;
import androidx.work.ListenableWorker;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import in.mohammad.ramiz.travel.data.local.AetherDatabase;

import java.util.concurrent.TimeUnit;

import dagger.assisted.Assisted;
import dagger.assisted.AssistedInject;

/** Twice a day: purge expired weather, old notifications, stale cancelled journeys, extra search rows. */
@HiltWorker
public class CacheCleanupWorker extends Worker {

    private final AetherDatabase db;

    @AssistedInject
    public CacheCleanupWorker(@Assisted @NonNull Context context,
                              @Assisted @NonNull WorkerParameters params,
                              AetherDatabase db) {
        super(context, params);
        this.db = db;
    }

    @NonNull
    @Override
    public ListenableWorker.Result doWork() {
        long now = System.currentTimeMillis();
        db.weatherCacheDao().deleteExpired(now);
        db.notificationDao().deleteOlderThan(now - TimeUnit.DAYS.toMillis(30));
        db.journeyDao().deleteCancelledOlderThan(now - TimeUnit.DAYS.toMillis(7));
        db.searchHistoryDao().pruneToLimit();
        return ListenableWorker.Result.success();
    }
}

