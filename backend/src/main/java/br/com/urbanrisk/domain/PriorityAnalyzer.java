package br.com.urbanrisk.domain;

import java.time.Duration;
import java.util.Collection;

/**
 * score = 100 * clamp(0.45*(severity/5) + 0.25*confidence + 0.20*clusterFactor + 0.10*recency)
 * clusterFactor = min(1, nearbySameTypeLast1h / 5), recency = exp(-ageHours / 6).
 */
public final class PriorityAnalyzer {

    public static final double CLUSTER_RADIUS_KM = 1.0;
    public static final Duration CLUSTER_WINDOW = Duration.ofHours(1);
    private static final double EARTH_RADIUS_KM = 6371.0088;

    private PriorityAnalyzer() {
    }

    public static PriorityAnalysis analyze(int severity, double confidence, int nearbySameType, double ageHours) {
        double clusterFactor = Math.min(1.0, nearbySameType / 5.0);
        double recency = Math.exp(-Math.max(0, ageHours) / 6.0);
        double raw = 0.45 * (severity / 5.0) + 0.25 * confidence + 0.20 * clusterFactor + 0.10 * recency;
        double score = 100 * Math.clamp(raw, 0.0, 1.0);
        return new PriorityAnalysis(score, PriorityLevel.fromScore(score));
    }

    /** Same-type events within 1km whose occurredAt is within 1h (either side) of the target. Target excluded. */
    public static int countNearby(RiskEvent target, Collection<RiskEvent> candidates) {
        return (int) candidates.stream()
                .filter(c -> c != target && (c.getId() == null || !c.getId().equals(target.getId())))
                .filter(c -> c.getType() == target.getType())
                .filter(c -> Duration.between(c.getOccurredAt(), target.getOccurredAt()).abs().compareTo(CLUSTER_WINDOW) <= 0)
                .filter(c -> haversineKm(target.getLatitude(), target.getLongitude(), c.getLatitude(), c.getLongitude()) <= CLUSTER_RADIUS_KM)
                .count();
    }

    public static double haversineKm(double lat1, double lng1, double lat2, double lng2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double a = Math.pow(Math.sin(dLat / 2), 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) * Math.pow(Math.sin(dLng / 2), 2);
        return 2 * EARTH_RADIUS_KM * Math.asin(Math.sqrt(a));
    }
}
