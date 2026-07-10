package in.mohammad.ramiz.travel.domain.engine;

import androidx.annotation.NonNull;

import in.mohammad.ramiz.travel.domain.model.Recommendation;
import in.mohammad.ramiz.travel.domain.model.TravelContext;
import in.mohammad.ramiz.travel.domain.engine.rules.RecommendationRule;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Rule-based decision engine (no ML). Iterates over the injected rule registry,
 * collects firing recommendations and orders them: WARNING first, then by confidence.
 * The engine itself never changes when new rules are added (Open/Closed).
 */
@Singleton
public class RecommendationEngine {

    private final List<RecommendationRule> rules;

    @Inject
    public RecommendationEngine(List<RecommendationRule> rules) {
        this.rules = rules;
    }

    @NonNull
    public List<Recommendation> evaluate(@NonNull TravelContext context) {
        List<Recommendation> out = new ArrayList<>();
        for (RecommendationRule rule : rules) {
            try {
                Recommendation r = rule.evaluate(context);
                if (r != null) out.add(r);
            } catch (Exception ignored) {
                // A broken rule must never take down the engine.
            }
        }
        out.sort(Comparator
                .comparing((Recommendation r) -> r.severity != Recommendation.Severity.WARNING)
                .thenComparing(r -> -r.confidence));
        return out;
    }
}

