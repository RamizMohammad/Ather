package in.mohammad.ramiz.travel.domain.engine;

import androidx.annotation.NonNull;

import in.mohammad.ramiz.travel.data.local.entity.JourneyEntity;
import in.mohammad.ramiz.travel.data.local.entity.PackingEntity;
import in.mohammad.ramiz.travel.domain.model.Recommendation;
import in.mohammad.ramiz.travel.domain.model.TravelContext;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Generates the packing list for a journey.
 * Sources: 1) packable outputs of the rule engine (weather/temperature driven),
 * 2) transport-mode items, 3) duration-driven items, 4) precautionary baseline.
 * Each item carries a confidence score and a human-readable reason.
 */
@Singleton
public class PackingEngine {

    private final RecommendationEngine recommendationEngine;

    @Inject
    public PackingEngine(RecommendationEngine recommendationEngine) {
        this.recommendationEngine = recommendationEngine;
    }

    @NonNull
    public List<PackingEntity> generate(long journeyId, @NonNull TravelContext ctx) {
        List<PackingEntity> items = new ArrayList<>();
        Set<String> added = new HashSet<>();
        long now = System.currentTimeMillis();

        // 1. Weather/temperature-driven items from the rule engine.
        for (Recommendation rec : recommendationEngine.evaluate(ctx)) {
            if (rec.packingItem == null || !added.add(rec.packingItem)) continue;
            String source = rec.type.startsWith("TEMP") || rec.type.startsWith("HEAT")
                    ? PackingEntity.SOURCE_TEMPERATURE : PackingEntity.SOURCE_WEATHER;
            items.add(item(journeyId, rec.packingItem, rec.message, source, rec.confidence, now));
        }

        // 2. Transport-mode items.
        if (JourneyEntity.MODE_MOTORCYCLE.equals(ctx.transportMode)) {
            if (added.add("Helmet")) {
                items.add(item(journeyId, "Helmet", "Required riding gear.",
                        PackingEntity.SOURCE_MODE, 100, now));
            }
            if (added.add("Riding gloves")) {
                items.add(item(journeyId, "Riding gloves", "Grip and protection for the ride.",
                        PackingEntity.SOURCE_MODE, 90, now));
            }
            boolean wet = ctx.arrivalWeather != null && ctx.arrivalWeather.rainChancePercent > 40;
            if (wet && added.add("Visor cleaner")) {
                items.add(item(journeyId, "Visor cleaner",
                        "Rain likely â€” keep the visor clear.", PackingEntity.SOURCE_MODE, 75, now));
            }
        }

        // 3. Duration-driven items.
        long hours = TimeUnit.SECONDS.toHours(ctx.routeDurationSeconds);
        if (hours >= 2 && added.add("Power bank")) {
            items.add(item(journeyId, "Power bank",
                    "Journey over " + hours + " hours â€” navigation drains battery.",
                    PackingEntity.SOURCE_DURATION, (int) Math.min(95, 60 + hours * 8), now));
        }
        if (hours >= 3 && added.add("Snacks & water")) {
            items.add(item(journeyId, "Snacks & water",
                    "Long trip â€” no guaranteed stops on route.",
                    PackingEntity.SOURCE_DURATION, 70, now));
        }

        // 4. Precautionary baseline (low confidence, still visible).
        if (ctx.arrivalWeather != null
                && ctx.arrivalWeather.rainChancePercent > 10
                && ctx.arrivalWeather.rainChancePercent <= 60
                && added.add("Umbrella")) {
            items.add(item(journeyId, "Umbrella",
                    "Only a " + Math.round(ctx.arrivalWeather.rainChancePercent)
                            + "% chance of rain. Optional, but recommended if you prefer to be fully prepared.",
                    PackingEntity.SOURCE_PRECAUTION,
                    (int) (20 + ctx.arrivalWeather.rainChancePercent / 2), now));
        }
        if (added.add("Phone charger")) {
            items.add(item(journeyId, "Phone charger", "Standard travel item.",
                    PackingEntity.SOURCE_PRECAUTION, 40, now));
        }

        return items;
    }

    private PackingEntity item(long journeyId, String name, String reason,
                               String source, int confidence, long now) {
        PackingEntity e = new PackingEntity();
        e.journeyId = journeyId;
        e.itemName = name;
        e.reason = reason;
        e.source = source;
        e.confidence = Math.max(0, Math.min(100, confidence));
        e.createdAt = now;
        return e;
    }
}

