package in.mohammad.ramiz.travel.domain.model;

/**
 * Output of a RecommendationRule. severity drives notification priority:
 * INFO -> silent insight card, ADVICE -> default notification, WARNING -> heads-up.
 */
public class Recommendation {

    public enum Severity {INFO, ADVICE, WARNING}

    /** Stable type key used for notification cooldowns, e.g. "RAIN_UMBRELLA". */
    public final String type;
    public final String title;
    public final String message;
    public final Severity severity;
    /** 0..100 confidence, also reused by the packing engine. */
    public final int confidence;
    /** Suggested packing item, or null when the advice is not packable. */
    public final String packingItem;

    public Recommendation(String type, String title, String message,
                          Severity severity, int confidence, String packingItem) {
        this.type = type;
        this.title = title;
        this.message = message;
        this.severity = severity;
        this.confidence = confidence;
        this.packingItem = packingItem;
    }
}

