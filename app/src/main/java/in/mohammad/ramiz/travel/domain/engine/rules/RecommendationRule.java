package in.mohammad.ramiz.travel.domain.engine.rules;

import androidx.annotation.Nullable;

import in.mohammad.ramiz.travel.domain.model.Recommendation;
import in.mohammad.ramiz.travel.domain.model.TravelContext;

/**
 * One travel condition check. Implementations must be stateless and side-effect free.
 * To add behaviour, add a new class and register it in AppModule - never edit existing rules.
 */
public interface RecommendationRule {

    /** Stable identifier, doubles as the notification cooldown key. */
    String type();

    /** Returns a recommendation when the condition applies, otherwise null. */
    @Nullable
    Recommendation evaluate(TravelContext context);
}

