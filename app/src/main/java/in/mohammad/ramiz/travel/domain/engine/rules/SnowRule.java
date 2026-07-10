package in.mohammad.ramiz.travel.domain.engine.rules;

import androidx.annotation.Nullable;

import in.mohammad.ramiz.travel.domain.model.Recommendation;
import in.mohammad.ramiz.travel.domain.model.TravelContext;
import in.mohammad.ramiz.travel.domain.model.WeatherSnapshot;

/** Snow chance > 50% -> warm gear + traction warning. */
public class SnowRule implements RecommendationRule {

    private static final double THRESHOLD_PERCENT = 50;

    @Override
    public String type() {
        return "SNOW_GEAR";
    }

    @Nullable
    @Override
    public Recommendation evaluate(TravelContext ctx) {
        WeatherSnapshot w = ctx.hasArrivalWeather() ? ctx.arrivalWeather : ctx.currentWeather;
        if (w == null || w.snowChancePercent <= THRESHOLD_PERCENT) return null;

        return new Recommendation(type(),
                "Snow expected",
                Math.round(w.snowChancePercent) + "% chance of snow"
                        + (ctx.hasArrivalWeather() ? " at your destination" : "")
                        + ". Pack gloves and warm layers; roads may be slippery.",
                Recommendation.Severity.ADVICE,
                (int) Math.min(100, w.snowChancePercent + 15), "Gloves & warm layers");
    }
}

