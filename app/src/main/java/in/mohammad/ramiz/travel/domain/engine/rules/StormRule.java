package in.mohammad.ramiz.travel.domain.engine.rules;

import androidx.annotation.Nullable;

import in.mohammad.ramiz.travel.domain.model.Recommendation;
import in.mohammad.ramiz.travel.domain.model.TravelContext;
import in.mohammad.ramiz.travel.domain.model.WeatherSnapshot;

/**
 * Thunderstorm condition codes (WeatherAPI 1087, 1273-1282) -> heads-up warning,
 * stronger message for motorcycle riders.
 */
public class StormRule implements RecommendationRule {

    @Override
    public String type() {
        return "STORM_ALERT";
    }

    @Nullable
    @Override
    public Recommendation evaluate(TravelContext ctx) {
        WeatherSnapshot w = ctx.hasArrivalWeather() ? ctx.arrivalWeather : ctx.currentWeather;
        if (w == null || !isStorm(w.conditionCode)) return null;

        String message = ctx.isMotorcycle()
                ? "Thunderstorms along your journey. Riding is dangerous â€” consider delaying or taking a car."
                : "Thunderstorms expected. Drive carefully and expect delays.";
        return new Recommendation(type(), "Storm warning", message,
                Recommendation.Severity.WARNING, 95, null);
    }

    private boolean isStorm(int code) {
        return code == 1087 || (code >= 1273 && code <= 1282);
    }
}

