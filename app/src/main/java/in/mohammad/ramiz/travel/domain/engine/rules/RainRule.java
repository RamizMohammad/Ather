package in.mohammad.ramiz.travel.domain.engine.rules;

import androidx.annotation.Nullable;

import in.mohammad.ramiz.travel.domain.model.Recommendation;
import in.mohammad.ramiz.travel.domain.model.TravelContext;
import in.mohammad.ramiz.travel.domain.model.WeatherSnapshot;

/** Rain probability > 60% at destination/arrival (or currently) -> umbrella. */
public class RainRule implements RecommendationRule {

    private static final double THRESHOLD_PERCENT = 60;

    @Override
    public String type() {
        return "RAIN_UMBRELLA";
    }

    @Nullable
    @Override
    public Recommendation evaluate(TravelContext ctx) {
        WeatherSnapshot w = ctx.hasArrivalWeather() ? ctx.arrivalWeather : ctx.currentWeather;
        if (w == null || w.rainChancePercent <= THRESHOLD_PERCENT) return null;

        int confidence = (int) Math.min(100, w.rainChancePercent + 10);
        String where = ctx.hasArrivalWeather() ? "at your destination" : "today";
        return new Recommendation(type(),
                "Carry an umbrella",
                Math.round(w.rainChancePercent) + "% chance of rain " + where
                        + (w.conditionText != null ? " â€” " + w.conditionText.toLowerCase() : "") + ".",
                Recommendation.Severity.ADVICE, confidence, "Umbrella");
    }
}

