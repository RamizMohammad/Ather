package in.mohammad.ramiz.travel.worker;

import android.content.Context;
import android.location.Location;

import androidx.annotation.NonNull;
import androidx.hilt.work.HiltWorker;
import androidx.work.ListenableWorker;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import in.mohammad.ramiz.travel.data.local.entity.ReminderEntity;
import in.mohammad.ramiz.travel.data.repository.LocationRepository;
import in.mohammad.ramiz.travel.data.repository.ReminderRepository;
import in.mohammad.ramiz.travel.domain.engine.NotificationEngine;
import in.mohammad.ramiz.travel.domain.engine.ReminderEngine;

import java.util.List;

import dagger.assisted.Assisted;
import dagger.assisted.AssistedInject;

/**
 * Safety net for reminders while no navigation is running (app in background).
 * During navigation, NavigationService evaluates on every location tick instead.
 */
@HiltWorker
public class ReminderCheckWorker extends Worker {

    private final ReminderRepository reminderRepository;
    private final LocationRepository locationRepository;
    private final ReminderEngine reminderEngine;
    private final NotificationEngine notificationEngine;

    @AssistedInject
    public ReminderCheckWorker(@Assisted @NonNull Context context,
                               @Assisted @NonNull WorkerParameters params,
                               ReminderRepository reminderRepository,
                               LocationRepository locationRepository,
                               ReminderEngine reminderEngine,
                               NotificationEngine notificationEngine) {
        super(context, params);
        this.reminderRepository = reminderRepository;
        this.locationRepository = locationRepository;
        this.reminderEngine = reminderEngine;
        this.notificationEngine = notificationEngine;
    }

    @NonNull
    @Override
    public ListenableWorker.Result doWork() {
        Location loc = locationRepository.getCachedLocation();
        if (loc == null) return ListenableWorker.Result.success();

        List<ReminderEntity> pending = reminderRepository.getPendingBlocking();
        if (pending.isEmpty()) return ListenableWorker.Result.success();

        for (ReminderEngine.Evaluation eval : reminderEngine.evaluate(
                pending, loc.getLatitude(), loc.getLongitude(),
                loc.hasSpeed() ? loc.getSpeed() * 3.6 : 0,
                -1, Double.NaN, Double.NaN)) {
            reminderRepository.dao().markNotified(eval.reminder.id);
            notificationEngine.postReminder(eval.reminder.id, eval.reminder.title, eval.reasonText);
        }
        return ListenableWorker.Result.success();
    }
}

