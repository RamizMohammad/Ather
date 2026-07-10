package in.mohammad.ramiz.travel.worker;

import android.content.Context;

import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.inject.Singleton;

import dagger.hilt.android.qualifiers.ApplicationContext;

/**
 * Registers all periodic background work. Battery policy:
 * network-bound work requires CONNECTED + battery-not-low; cleanup runs while idle-friendly.
 */
@Singleton
public class WorkScheduler {

    private final Context context;

    @Inject
    public WorkScheduler(@ApplicationContext Context context) {
        this.context = context;
    }

    public void schedulePeriodicWork() {
        WorkManager wm = WorkManager.getInstance(context);

        Constraints networkConstraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .setRequiresBatteryNotLow(true)
                .build();

        wm.enqueueUniquePeriodicWork("weather_refresh",
                ExistingPeriodicWorkPolicy.KEEP,
                new PeriodicWorkRequest.Builder(WeatherRefreshWorker.class, 30, TimeUnit.MINUTES)
                        .setConstraints(networkConstraints)
                        .build());

        wm.enqueueUniquePeriodicWork("reminder_check",
                ExistingPeriodicWorkPolicy.KEEP,
                new PeriodicWorkRequest.Builder(ReminderCheckWorker.class, 15, TimeUnit.MINUTES)
                        .build());

        wm.enqueueUniquePeriodicWork("cache_cleanup",
                ExistingPeriodicWorkPolicy.KEEP,
                new PeriodicWorkRequest.Builder(CacheCleanupWorker.class, 12, TimeUnit.HOURS)
                        .build());

        wm.enqueueUniquePeriodicWork("statistics",
                ExistingPeriodicWorkPolicy.KEEP,
                new PeriodicWorkRequest.Builder(StatisticsWorker.class, 6, TimeUnit.HOURS)
                        .build());
    }
}

