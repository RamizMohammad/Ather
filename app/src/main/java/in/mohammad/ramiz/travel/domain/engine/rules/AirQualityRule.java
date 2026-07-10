package in.mohammad.ramiz.travel.domain.engine.rules;

import androidx.annotation.Nullable;

import in.mohammad.ramiz.travel.domain.model.Recommendation;
import in.mohammad.ramiz.travel.domain.model.TravelContext;
import in.mohammad.ramiz.travel.domain.model.WeatherSnapshot;

/** PM2.5 above ~unhealthy level (AQI > 200 equivalent) -> face mask. */
public class AirQualityRule implements RecommendationRule {

    /** PM2.5 Âµg/mÂ³ roughly corresponding to US AQI 200. */
    private static final double PM25_THRESHOLD = 150;

    @Override
    public String type() {
        return "AQI_MASK";
    }

    @Nullable
    @Override
    public Recommendation evaluate(TravelContext ctx) {
        WeatherSnapshot w = ctx.hasArrivalWeather() ? ctx.arrivalWeather : ctx.currentWeather;
        if (w == null) return null;
        boolean bad = w.aqiPm25 > PM25_THRESHOLD || w.usEpaIndex >= 5;
        if (!bad) return null;

        return new Recommendation(type(),
                "Poor air quality",
                "Air quality is unhealthy"
                        + (ctx.hasArrivalWeather() ? " at your destination" : "")
                        + ". Wearing a mask outdoors is recommended.",
                Recommendation.Severity.ADVICE, 85, "Face mask");
    }
}

