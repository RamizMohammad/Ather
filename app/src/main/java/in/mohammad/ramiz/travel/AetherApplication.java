package in.mohammad.ramiz.travel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.hilt.work.HiltWorkerFactory;
import androidx.work.Configuration;

import in.mohammad.ramiz.travel.core.NotificationChannels;
import in.mohammad.ramiz.travel.worker.WorkScheduler;
import com.mappls.sdk.maps.Mappls;

import javax.inject.Inject;

import dagger.hilt.android.HiltAndroidApp;

/**
 * Application entry point.
 * Initializes the Mappls (MapmyIndia) SDK, notification channels,
 * Hilt-aware WorkManager and periodic background work.
 */
@HiltAndroidApp
public class AetherApplication extends Application implements Configuration.Provider {

    @Inject
    HiltWorkerFactory workerFactory;

    @Inject
    WorkScheduler workScheduler;

    @Override
    public void onCreate() {
        super.onCreate();

        // Mappls SDK (map tiles, routing, search). Auth is handled by the
        // <appId>.a.conf / <appId>.a.olf configuration files in the app module
        // (processed by the Mappls Services gradle plugin) - no API keys needed.
        Mappls.getInstance(getApplicationContext());

        NotificationChannels.createAll(this);
        workScheduler.schedulePeriodicWork();
    }

    @NonNull
    @Override
    public Configuration getWorkManagerConfiguration() {
        return new Configuration.Builder()
                .setWorkerFactory(workerFactory)
                .build();
    }
}

