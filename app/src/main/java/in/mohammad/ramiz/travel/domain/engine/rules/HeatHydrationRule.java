package in.mohammad.ramiz.travel.domain.engine.rules;

import androidx.annotation.Nullable;

import in.mohammad.ramiz.travel.domain.model.Recommendation;
import in.mohammad.ramiz.travel.domain.model.TravelContext;
import in.mohammad.ramiz.travel.domain.model.WeatherSnapshot;

/** Destination temperature > 38Â°C -> water bottle + heat caution. */
public class HeatHydrationRule implements RecommendationRule {

    private static final double THRESHOLD_C = 38;

    @Override
    public String type() {
        return "HEAT_WATER";
    }

    @Nullable
    @Override
    public Recommendation evaluate(TravelContext ctx) {
        WeatherSnapshot w = ctx.hasArrivalWeather() ? ctx.arrivalWeather : ctx.currentWeather;
        if (w == null || w.tempC <= THRESHOLD_C) return null;

        return new Recommendation(type(),
                "Extreme heat â€” stay hydrated",
                Math.round(w.tempC) + "Â°C"
                        + (ctx.hasArrivalWeather() ? " expected on arrival" : " right now")
                        + ". Carry water and avoid midday sun.",
                Recommendation.Severity.ADVICE,
                (int) Math.min(100, 60 + (w.tempC - THRESHOLD_C) * 5), "Water bottle");
    }
}

