package in.mohammad.ramiz.travel.util;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public final class FormatUtil {

    private FormatUtil() {
    }

    public static String duration(long seconds) {
        long h = TimeUnit.SECONDS.toHours(seconds);
        long m = TimeUnit.SECONDS.toMinutes(seconds) % 60;
        if (h > 0) return h + "h " + m + "m";
        return m + "m";
    }

    public static String distance(double meters) {
        if (meters >= 1000) return String.format(Locale.getDefault(), "%.1f km", meters / 1000.0);
        return Math.round(meters) + " m";
    }

    public static String clockTime(long epochMillis) {
        return new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date(epochMillis));
    }

    public static String dateShort(long epochMillis) {
        return new SimpleDateFormat("d MMM", Locale.getDefault()).format(new Date(epochMillis));
    }

    public static String temp(double celsius) {
        return Math.round(celsius) + "Â°";
    }
}

