package in.mohammad.ramiz.travel.domain.engine.rules;

import androidx.annotation.Nullable;

import in.mohammad.ramiz.travel.domain.model.Recommendation;
import in.mohammad.ramiz.travel.domain.model.TravelContext;
import in.mohammad.ramiz.travel.domain.model.WeatherSnapshot;

/** UV index > 8 -> sunglasses + sunscreen. */
public class UvRule implements RecommendationRule {

    private static final double THRESHOLD = 8;

    @Override
    public String type() {
        return "UV_HIGH";
    }

    @Nullable
    @Override
    public Recommendation evaluate(TravelContext ctx) {
        WeatherSnapshot w = ctx.hasArrivalWeather() ? ctx.arrivalWeather : ctx.currentWeather;
        if (w == null || w.uvIndex <= THRESHOLD) return null;

        return new Recommendation(type(),
                "Extreme UV today",
                "UV index is " + Math.round(w.uvIndex)
                        + ". Sunglasses and sunscreen are strongly recommended.",
                Recommendation.Severity.ADVICE,
                (int) Math.min(100, 60 + w.uvIndex * 4), "Sunglasses & sunscreen");
    }
}

