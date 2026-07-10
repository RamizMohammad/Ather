package in.mohammad.ramiz.travel.core;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;

import in.mohammad.ramiz.travel.R;

/**
 * Manages all notification channels for the app.
 * Required for Android 8.0 (API 26) and above.
 */
public class NotificationChannels {

    public static final String CHANNEL_REMINDERS = "reminders";
    public static final String CHANNEL_NAVIGATION = "navigation";
    public static final String CHANNEL_ALERTS = "alerts";
    public static final String CHANNEL_INSIGHTS = "insights";

    public static void createAll(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager nm = context.getSystemService(NotificationManager.class);
            if (nm == null) return;

            NotificationChannel reminders = new NotificationChannel(
                    CHANNEL_REMINDERS,
                    "Reminders",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            reminders.setDescription("Time-sensitive travel reminders");

            NotificationChannel navigation = new NotificationChannel(
                    CHANNEL_NAVIGATION,
                    "Navigation",
                    NotificationManager.IMPORTANCE_LOW
            );
            navigation.setDescription("Active turn-by-turn guidance");

            NotificationChannel alerts = new NotificationChannel(
                    CHANNEL_ALERTS,
                    "Weather & Safety Alerts",
                    NotificationManager.IMPORTANCE_HIGH
            );
            alerts.setDescription("Urgent weather or safety warnings");

            NotificationChannel insights = new NotificationChannel(
                    CHANNEL_INSIGHTS,
                    "Travel Insights",
                    NotificationManager.IMPORTANCE_MIN
            );
            insights.setDescription("Non-intrusive travel tips and stats");

            nm.createNotificationChannel(reminders);
            nm.createNotificationChannel(navigation);
            nm.createNotificationChannel(alerts);
            nm.createNotificationChannel(insights);
        }
    }
}
