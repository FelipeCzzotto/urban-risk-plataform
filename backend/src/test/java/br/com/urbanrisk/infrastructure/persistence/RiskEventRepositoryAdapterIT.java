package br.com.urbanrisk.infrastructure.persistence;

import br.com.urbanrisk.application.EventFilter;
import br.com.urbanrisk.application.PageResult;
import br.com.urbanrisk.domain.EventSource;
import br.com.urbanrisk.domain.EventStatus;
import br.com.urbanrisk.domain.EventType;
import br.com.urbanrisk.domain.PriorityLevel;
import br.com.urbanrisk.domain.RiskEvent;
import br.com.urbanrisk.infrastructure.PostgresContainerConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({PostgresContainerConfig.class, RiskEventRepositoryAdapter.class})
@TestPropertySource(properties = "spring.jpa.hibernate.ddl-auto=validate")
class RiskEventRepositoryAdapterIT {

    private static final Instant T = Instant.parse("2026-11-18T12:00:00Z");

    @Autowired RiskEventRepositoryAdapter adapter;
    @Autowired RiskEventJpaRepository jpa;

    @BeforeEach
    void clean() {
        jpa.deleteAll();
    }

    private RiskEvent event(EventType type, double score, PriorityLevel level, Instant occurredAt) {
        RiskEvent e = new RiskEvent();
        e.setType(type);
        e.setTitle("t");
        e.setDescription("");
        e.setLatitude(-23.55);
        e.setLongitude(-46.63);
        e.setNeighborhood("Sé");
        e.setSeverity(3);
        e.setSource(EventSource.COMMUNITY);
        e.setSourceRef("community");
        e.setConfidence(0.5);
        e.setPriorityScore(score);
        e.setPriorityLevel(level);
        e.setReporterHash("h");
        e.setOccurredAt(occurredAt);
        return e;
    }

    @Test
    void saveRoundTripsAllFieldsAndAssignsIdAndCreatedAt() {
        RiskEvent saved = adapter.save(event(EventType.FLOOD, 61.5, PriorityLevel.HIGH, T));

        RiskEvent found = adapter.findById(saved.getId()).orElseThrow();
        assertThat(found.getId()).isNotNull();
        assertThat(found.getCreatedAt()).isNotNull();
        assertThat(found.getSeverity()).isEqualTo(3);
        assertThat(found.getPriorityScore()).isEqualTo(61.5);
        assertThat(found.getReporterHash()).isEqualTo("h");
        assertThat(found.getStatus()).isEqualTo(EventStatus.ACTIVE);
        assertThat(found.getOccurredAt()).isEqualTo(T);
    }

    @Test
    void saveExistingUpdatesStatus() {
        RiskEvent saved = adapter.save(event(EventType.FLOOD, 50, PriorityLevel.MEDIUM, T));
        saved.resolve();
        adapter.save(saved);

        assertThat(adapter.findById(saved.getId()).orElseThrow().getStatus()).isEqualTo(EventStatus.RESOLVED);
        assertThat(jpa.count()).isEqualTo(1);
    }

    @Test
    void searchFiltersAndSortsByScoreThenOccurredAt() {
        adapter.save(event(EventType.FLOOD, 50, PriorityLevel.MEDIUM, T.minus(2, ChronoUnit.HOURS)));
        adapter.save(event(EventType.FLOOD, 50, PriorityLevel.MEDIUM, T));
        adapter.save(event(EventType.FLOOD, 90, PriorityLevel.CRITICAL, T.minus(5, ChronoUnit.HOURS)));
        adapter.save(event(EventType.HEAT_ISLAND, 99, PriorityLevel.CRITICAL, T));
        RiskEvent resolved = event(EventType.FLOOD, 95, PriorityLevel.CRITICAL, T);
        resolved.resolve();
        adapter.save(resolved);

        PageResult<RiskEvent> page = adapter.search(new EventFilter(EventType.FLOOD, null, EventStatus.ACTIVE), 0, 2);

        assertThat(page.totalElements()).isEqualTo(3);
        assertThat(page.totalPages()).isEqualTo(2);
        assertThat(page.content()).extracting(RiskEvent::getPriorityScore).containsExactly(90.0, 50.0);
        assertThat(page.content().get(1).getOccurredAt()).isEqualTo(T);

        assertThat(adapter.search(new EventFilter(null, PriorityLevel.CRITICAL, null), 0, 10).totalElements()).isEqualTo(3);
    }

    @Test
    void activeAlertsOnlyHighAndCriticalSortedAndLimited() {
        adapter.save(event(EventType.FLOOD, 65, PriorityLevel.HIGH, T));
        adapter.save(event(EventType.FLOOD, 85, PriorityLevel.CRITICAL, T));
        adapter.save(event(EventType.FLOOD, 45, PriorityLevel.MEDIUM, T));
        RiskEvent resolved = event(EventType.FLOOD, 99, PriorityLevel.CRITICAL, T);
        resolved.resolve();
        adapter.save(resolved);

        assertThat(adapter.findActiveAlerts(50)).extracting(RiskEvent::getPriorityScore).containsExactly(85.0, 65.0);
        assertThat(adapter.findActiveAlerts(1)).hasSize(1);
    }

    @Test
    void findSameTypeBetweenRespectsWindowAndType() {
        adapter.save(event(EventType.FLOOD, 10, PriorityLevel.LOW, T.minus(30, ChronoUnit.MINUTES)));
        adapter.save(event(EventType.FLOOD, 10, PriorityLevel.LOW, T.minus(2, ChronoUnit.HOURS)));
        adapter.save(event(EventType.SERVICE_OUTAGE, 10, PriorityLevel.LOW, T));

        List<RiskEvent> found = adapter.findSameTypeBetween(EventType.FLOOD, T.minus(1, ChronoUnit.HOURS), T.plus(1, ChronoUnit.HOURS));
        assertThat(found).hasSize(1);
    }

    @Test
    void countsAndLastUpdated() {
        adapter.save(event(EventType.FLOOD, 65, PriorityLevel.HIGH, T));
        adapter.save(event(EventType.FLOOD, 10, PriorityLevel.LOW, T));
        adapter.save(event(EventType.HEAT_ISLAND, 10, PriorityLevel.LOW, T));
        RiskEvent resolved = event(EventType.SERVICE_OUTAGE, 10, PriorityLevel.LOW, T);
        resolved.resolve();
        adapter.save(resolved);

        assertThat(adapter.countActiveByType()).containsEntry(EventType.FLOOD, 2L).containsEntry(EventType.HEAT_ISLAND, 1L)
                .doesNotContainKey(EventType.SERVICE_OUTAGE);
        assertThat(adapter.countActiveByLevel()).containsEntry(PriorityLevel.LOW, 2L).containsEntry(PriorityLevel.HIGH, 1L);
        assertThat(adapter.lastUpdatedAt()).isPresent();
    }
}
