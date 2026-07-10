package in.mohammad.ramiz.travel.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.content.ContextCompat;

import in.mohammad.ramiz.travel.R;
import com.mappls.sdk.maps.MapplsMap;
import com.mappls.sdk.maps.annotations.Icon;
import com.mappls.sdk.maps.annotations.IconFactory;
import com.mappls.sdk.maps.annotations.Polyline;
import com.mappls.sdk.maps.annotations.PolylineOptions;
import com.mappls.sdk.maps.geometry.LatLng;

import java.util.List;

/**
 * Aether look & feel for Mappls maps: night style (matches the OLED palette),
 * a two-layer accent route line and vector marker icons.
 */
public final class MapUiUtil {

    /** Mappls preset night theme; falls back to the default style if unavailable. */
    private static final String STYLE_NIGHT = "standard-night";

    private MapUiUtil() {
    }

    /** Apply the app's preferred (night) map style. Safe to call on every map. */
    public static void applyAetherStyle(@NonNull MapplsMap map) {
        try {
            map.setMapplsStyle(STYLE_NIGHT);
        } catch (Exception ignored) {
            // Style not available on this account: SDK keeps the default style.
        }
    }

    /**
     * Draws the route as a dark casing with the azure line on top.
     *
     * @return the top (colored) polyline so callers can animate it, or null.
     */
    @Nullable
    public static Polyline drawStyledRoute(@NonNull Context context, @NonNull MapplsMap map,
                                           @NonNull List<LatLng> path) {
        if (path.isEmpty()) return null;
        map.addPolyline(new PolylineOptions()
                .addAll(path)
                .color(ContextCompat.getColor(context, R.color.route_casing))
                .width(11f));
        return map.addPolyline(new PolylineOptions()
                .addAll(path)
                .color(ContextCompat.getColor(context, R.color.route_line))
                .width(6f));
    }

    /** Vector drawable -> Mappls marker icon. Null when the drawable is missing. */
    @Nullable
    public static Icon markerIcon(@NonNull Context context, @DrawableRes int resId) {
        Drawable drawable = AppCompatResources.getDrawable(context, resId);
        if (drawable == null) return null;
        int w = Math.max(1, drawable.getIntrinsicWidth());
        int h = Math.max(1, drawable.getIntrinsicHeight());
        Bitmap bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, w, h);
        drawable.draw(canvas);
        return IconFactory.getInstance(context).fromBitmap(bitmap);
    }
}
