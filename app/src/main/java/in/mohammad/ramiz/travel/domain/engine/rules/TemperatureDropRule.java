package in.mohammad.ramiz.travel.domain.engine.rules;

import androidx.annotation.Nullable;

import in.mohammad.ramiz.travel.domain.model.Recommendation;
import in.mohammad.ramiz.travel.domain.model.TravelContext;

/** Destination is >= 12Â°C colder than here -> jacket. */
public class TemperatureDropRule implements RecommendationRule {

    private static final double THRESHOLD_C = 12;

    @Override
    public String type() {
        return "TEMP_DROP_JACKET";
    }

    @Nullable
    @Override
    public Recommendation evaluate(TravelContext ctx) {
        if (ctx.currentWeather == null || !ctx.hasArrivalWeather()) return null;
        double diff = ctx.currentWeather.tempC - ctx.arrivalWeather.tempC;
        if (diff < THRESHOLD_C) return null;

        return new Recommendation(type(),
                "Pack a jacket",
                "It will be " + Math.round(diff) + "Â°C colder at "
                        + (ctx.arrivalWeather.placeName != null ? ctx.arrivalWeather.placeName : "your destination")
                        + " (" + Math.round(ctx.arrivalWeather.tempC) + "Â°C on arrival).",
                Recommendation.Severity.ADVICE,
                (int) Math.min(100, 55 + diff * 3), "Jacket");
    }
}

