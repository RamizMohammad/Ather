package in.mohammad.ramiz.travel.domain.engine.rules;

import androidx.annotation.Nullable;

import in.mohammad.ramiz.travel.domain.model.Recommendation;
import in.mohammad.ramiz.travel.domain.model.TravelContext;
import in.mohammad.ramiz.travel.domain.model.WeatherSnapshot;

/** Visibility < 2 km -> low-visibility warning. */
public class FogRule implements RecommendationRule {

    private static final double THRESHOLD_KM = 2;

    @Override
    public String type() {
        return "FOG_VISIBILITY";
    }

    @Nullable
    @Override
    public Recommendation evaluate(TravelContext ctx) {
        WeatherSnapshot w = ctx.hasArrivalWeather() ? ctx.arrivalWeather : ctx.currentWeather;
        if (w == null || w.visibilityKm <= 0 || w.visibilityKm >= THRESHOLD_KM) return null;

        return new Recommendation(type(),
                "Low visibility ahead",
                "Visibility down to " + w.visibilityKm + " km. Use fog lights and keep extra distance.",
                Recommendation.Severity.WARNING, 90, null);
    }
}

