package br.com.urbanrisk.domain;

/** Base confidence per source, +0.1 per corroborating same-type event within 1km/1h, capped at 1.0. */
public final class ConfidenceCalculator {

    private ConfidenceCalculator() {
    }

    public static double calculate(EventSource source, int corroboratingEvents) {
        if (corroboratingEvents < 0) {
            throw new IllegalArgumentException("corroboratingEvents must be >= 0");
        }
        return Math.min(1.0, source.baseConfidence() + 0.1 * corroboratingEvents);
    }
}
