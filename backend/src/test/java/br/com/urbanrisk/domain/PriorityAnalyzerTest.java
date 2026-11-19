package br.com.urbanrisk.domain;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class PriorityAnalyzerTest {

    private static final Instant NOW = Instant.parse("2026-11-18T12:00:00Z");
    // Praça da Sé, São Paulo
    private static final double LAT = -23.5505;
    private static final double LNG = -46.6333;

    @Test
    void maxInputsGiveHundred() {
        PriorityAnalysis a = PriorityAnalyzer.analyze(5, 1.0, 5, 0);
        assertThat(a.score()).isCloseTo(100.0, within(1e-9));
        assertThat(a.level()).isEqualTo(PriorityLevel.CRITICAL);
    }

    @Test
    void appliesWeightedFormula() {
        // 0.45*(3/5) + 0.25*0.9 + 0.20*(2/5) + 0.10*exp(-1) = 0.27 + 0.225 + 0.08 + 0.0367879
        PriorityAnalysis a = PriorityAnalyzer.analyze(3, 0.9, 2, 6);
        assertThat(a.score()).isCloseTo(61.17879, within(1e-4));
        assertThat(a.level()).isEqualTo(PriorityLevel.HIGH);
    }

    @Test
    void clusterFactorCapsAtFiveNearbyEvents() {
        assertThat(PriorityAnalyzer.analyze(1, 0.5, 5, 0).score())
                .isCloseTo(PriorityAnalyzer.analyze(1, 0.5, 40, 0).score(), within(1e-9));
    }

    @Test
    void recencyDecaysWithAge() {
        double fresh = PriorityAnalyzer.analyze(3, 0.8, 0, 0).score();
        double sixHours = PriorityAnalyzer.analyze(3, 0.8, 0, 6).score();
        double old = PriorityAnalyzer.analyze(3, 0.8, 0, 1000).score();
        assertThat(fresh - sixHours).isCloseTo(10 * (1 - Math.exp(-1)), within(1e-9));
        assertThat(old).isCloseTo(fresh - 10, within(1e-6));
    }

    @Test
    void futureOccurrenceTreatedAsNow() {
        assertThat(PriorityAnalyzer.analyze(3, 0.8, 0, -5).score())
                .isCloseTo(PriorityAnalyzer.analyze(3, 0.8, 0, 0).score(), within(1e-9));
    }

    @ParameterizedTest
    @CsvSource({"0,LOW", "39.99,LOW", "40,MEDIUM", "59.99,MEDIUM", "60,HIGH", "79.99,HIGH", "80,CRITICAL", "100,CRITICAL"})
    void levelThresholds(double score, PriorityLevel expected) {
        assertThat(PriorityLevel.fromScore(score)).isEqualTo(expected);
    }

    @Test
    void countsOnlySameTypeWithinOneKmAndOneHour() {
        RiskEvent target = event(EventType.FLOOD, LAT, LNG, NOW);
        List<RiskEvent> candidates = List.of(
                event(EventType.FLOOD, LAT + 0.005, LNG, NOW.minus(Duration.ofMinutes(30))), // ~0.56 km: counts
                event(EventType.FLOOD, LAT, LNG + 0.009, NOW.plus(Duration.ofMinutes(59))),  // ~0.92 km: counts
                event(EventType.FLOOD, LAT + 0.02, LNG, NOW),                                // ~2.2 km: out
                event(EventType.FLOOD, LAT, LNG, NOW.minus(Duration.ofMinutes(61))),         // too old
                event(EventType.HEAT_ISLAND, LAT, LNG, NOW));                                // other type
        assertThat(PriorityAnalyzer.countNearby(target, candidates)).isEqualTo(2);
    }

    @Test
    void excludesTargetItselfFromCluster() {
        RiskEvent target = event(EventType.FLOOD, LAT, LNG, NOW);
        assertThat(PriorityAnalyzer.countNearby(target, List.of(target))).isZero();
    }

    @Test
    void haversineKnownDistance() {
        // Sé to Paulista/MASP is roughly 2.6 km
        assertThat(PriorityAnalyzer.haversineKm(-23.5505, -46.6333, -23.5614, -46.6559)).isBetween(2.4, 2.8);
    }

    private static RiskEvent event(EventType type, double lat, double lng, Instant occurredAt) {
        RiskEvent e = new RiskEvent();
        e.setType(type);
        e.setLatitude(lat);
        e.setLongitude(lng);
        e.setOccurredAt(occurredAt);
        return e;
    }
}
