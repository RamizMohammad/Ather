package in.mohammad.ramiz.travel.worker;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.hilt.work.HiltWorker;
import androidx.work.ListenableWorker;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import in.mohammad.ramiz.travel.data.local.AetherDatabase;
import in.mohammad.ramiz.travel.data.local.entity.JourneyEntity;
import in.mohammad.ramiz.travel.data.local.entity.StatisticsEntity;

import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import dagger.assisted.Assisted;
import dagger.assisted.AssistedInject;

/** Recomputes today's aggregate row from completed journeys, reminders and packing. */
@HiltWorker
public class StatisticsWorker extends Worker {

    private final AetherDatabase db;

    @AssistedInject
    public StatisticsWorker(@Assisted @NonNull Context context,
                            @Assisted @NonNull WorkerParameters params,
                            AetherDatabase db) {
        super(context, params);
        this.db = db;
    }

    @NonNull
    @Override
    public ListenableWorker.Result doWork() {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        long dayStart = cal.getTimeInMillis();
        long dayEnd = dayStart + 24L * 3600 * 1000;

        int dayKey = Integer.parseInt(String.format(Locale.US, "%tY%<tm%<td", cal));

        List<JourneyEntity> journeys = db.journeyDao().getCompletedBetweenSync(dayStart, dayEnd);
        double distance = 0;
        long travelTime = 0;
        double speedSum = 0;
        for (JourneyEntity j : journeys) {
            distance += j.travelledMeters;
            travelTime += Math.max(0, (j.completedAt - j.startedAt) / 1000);
            speedSum += j.avgSpeedKmh;
        }

        StatisticsEntity stats = new StatisticsEntity();
        stats.dayKey = dayKey;
        stats.journeysCompleted = journeys.size();
        stats.distanceMeters = distance;
        stats.travelTimeSeconds = travelTime;
        stats.avgSpeedKmh = journeys.isEmpty() ? 0 : speedSum / journeys.size();
        stats.remindersCompleted = db.reminderDao().countCompletedBetweenSync(dayStart, dayEnd);
        stats.itemsPacked = db.packingDao().countPackedBetweenSync(dayStart, dayEnd);
        stats.updatedAt = System.currentTimeMillis();
        db.statisticsDao().upsert(stats);
        return ListenableWorker.Result.success();
    }
}

