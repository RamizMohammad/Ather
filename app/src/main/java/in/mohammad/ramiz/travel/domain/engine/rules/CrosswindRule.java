package in.mohammad.ramiz.travel.domain.engine.rules;

import androidx.annotation.Nullable;

import in.mohammad.ramiz.travel.domain.model.Recommendation;
import in.mohammad.ramiz.travel.domain.model.TravelContext;
import in.mohammad.ramiz.travel.domain.model.WeatherSnapshot;

/** Wind > 35 km/h and riding a motorcycle -> heads-up warning. */
public class CrosswindRule implements RecommendationRule {

    private static final double THRESHOLD_KPH = 35;

    @Override
    public String type() {
        return "WIND_MOTORCYCLE";
    }

    @Nullable
    @Override
    public Recommendation evaluate(TravelContext ctx) {
        if (!ctx.isMotorcycle()) return null;
        WeatherSnapshot w = ctx.hasArrivalWeather() ? ctx.arrivalWeather : ctx.currentWeather;
        if (w == null) return null;
        double wind = Math.max(w.windKph, w.gustKph * 0.8);
        if (wind <= THRESHOLD_KPH) return null;

        return new Recommendation(type(),
                "Strong crosswinds on route",
                "Winds up to " + Math.round(Math.max(w.windKph, w.gustKph))
                        + " km/h. Reduce speed and keep a firm grip â€” gusts can push the bike.",
                Recommendation.Severity.WARNING,
                (int) Math.min(100, 50 + wind), null);
    }
}

