package in.mohammad.ramiz.travel.domain.engine;

import android.Manifest;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;

import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import in.mohammad.ramiz.travel.R;
import in.mohammad.ramiz.travel.core.AppExecutors;
import in.mohammad.ramiz.travel.core.NotificationChannels;
import in.mohammad.ramiz.travel.data.local.dao.NotificationDao;
import in.mohammad.ramiz.travel.data.local.entity.NotificationEntity;
import in.mohammad.ramiz.travel.domain.model.Recommendation;
import in.mohammad.ramiz.travel.MainActivity;

import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.inject.Singleton;

import dagger.hilt.android.qualifiers.ApplicationContext;

/**
 * Single gateway for every notification Aether posts.
 * Anti-annoyance policy:
 *  - per-type cooldown (default 3h) checked against the local notification log
 *  - severity mapping: INFO -> silent channel, ADVICE -> default, WARNING -> heads-up
 *  - everything is logged to Room for the assistant timeline and statistics.
 */
@Singleton
public class NotificationEngine {

    private static final long DEFAULT_COOLDOWN_MS = TimeUnit.HOURS.toMillis(3);

    public static final String PRIORITY_SILENT = "SILENT";
    public static final String PRIORITY_DEFAULT = "DEFAULT";
    public static final String PRIORITY_HEADS_UP = "HEADS_UP";

    private final Context context;
    private final NotificationDao notificationDao;
    private final AppExecutors executors;

    @Inject
    public NotificationEngine(@ApplicationContext Context context,
                              NotificationDao notificationDao,
                              AppExecutors executors) {
        this.context = context;
        this.notificationDao = notificationDao;
        this.executors = executors;
    }

    /** Post a recommendation, honoring its cooldown. Safe to call from any thread. */
    public void postRecommendation(Recommendation rec, Long journeyId) {
        executors.diskIO().execute(() -> {
            if (isOnCooldown(rec.type, DEFAULT_COOLDOWN_MS)) return;
            String priority = mapPriority(rec.severity);
            log(rec.type, rec.title, rec.message, priority, journeyId);
            show(rec.type.hashCode(), rec.title, rec.message, channelFor(priority), priority);
        });
    }

    /** Post a reminder notification (no cooldown Ã¢â‚¬â€ user asked for it). */
    public void postReminder(long reminderId, String title, String message) {
        executors.diskIO().execute(() -> {
            log("REMINDER", title, message, PRIORITY_DEFAULT, null);
            show((int) (10000 + reminderId), title, message,
                    NotificationChannels.CHANNEL_REMINDERS, PRIORITY_DEFAULT);
        });
    }

    /** Arrival / journey lifecycle events, 30-minute cooldown. */
    public void postJourneyEvent(String type, String title, String message, Long journeyId) {
        executors.diskIO().execute(() -> {
            if (isOnCooldown(type, TimeUnit.MINUTES.toMillis(30))) return;
            log(type, title, message, PRIORITY_DEFAULT, journeyId);
            show(type.hashCode(), title, message,
                    NotificationChannels.CHANNEL_NAVIGATION, PRIORITY_DEFAULT);
        });
    }

    // ------------------------------------------------------------------

    private boolean isOnCooldown(String type, long cooldownMs) {
        Long last = notificationDao.lastShownForTypeSync(type);
        return last != null && System.currentTimeMillis() - last < cooldownMs;
    }

    private void log(String type, String title, String message, String priority, Long journeyId) {
        NotificationEntity e = new NotificationEntity();
        e.type = type;
        e.title = title;
        e.message = message;
        e.priority = priority;
        e.journeyId = journeyId;
        e.shownAt = System.currentTimeMillis();
        notificationDao.insert(e);
    }

    private String mapPriority(Recommendation.Severity severity) {
        switch (severity) {
            case WARNING:
                return PRIORITY_HEADS_UP;
            case ADVICE:
                return PRIORITY_DEFAULT;
            default:
                return PRIORITY_SILENT;
        }
    }

    private String channelFor(String priority) {
        switch (priority) {
            case PRIORITY_HEADS_UP:
                return NotificationChannels.CHANNEL_ALERTS;
            case PRIORITY_SILENT:
                return NotificationChannels.CHANNEL_INSIGHTS;
            default:
                return NotificationChannels.CHANNEL_REMINDERS;
        }
    }

    private void show(int id, String title, String message, String channel, String priority) {
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            return; // Logged to Room anyway; timeline still shows it in-app.
        }
        Intent intent = new Intent(context, MainActivity.class);
        PendingIntent pending = PendingIntent.getActivity(context, id, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, channel)
                .setSmallIcon(R.drawable.ic_stat_aether)
                .setContentTitle(title)
                .setContentText(message)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(message))
                .setContentIntent(pending)
                .setAutoCancel(true)
                .setPriority(PRIORITY_HEADS_UP.equals(priority)
                        ? NotificationCompat.PRIORITY_HIGH
                        : PRIORITY_SILENT.equals(priority)
                        ? NotificationCompat.PRIORITY_LOW
                        : NotificationCompat.PRIORITY_DEFAULT);

        NotificationManagerCompat.from(context).notify(id, builder.build());
    }

    /** Builds (without posting) the persistent navigation notification for the foreground service. */
    public Notification buildNavigationNotification(String title, String text) {
        Intent intent = new Intent(context, MainActivity.class);
        PendingIntent pending = PendingIntent.getActivity(context, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        return new NotificationCompat.Builder(context, NotificationChannels.CHANNEL_NAVIGATION)
                .setSmallIcon(R.drawable.ic_stat_aether)
                .setContentTitle(title)
                .setContentText(text)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setContentIntent(pending)
                .build();
    }
}

