package br.com.urbanrisk.application;

import br.com.urbanrisk.application.port.EventPublisher;
import br.com.urbanrisk.application.port.ReporterHasher;
import br.com.urbanrisk.application.port.RiskEventRepository;
import br.com.urbanrisk.domain.EventSource;
import br.com.urbanrisk.domain.EventStatus;
import br.com.urbanrisk.domain.EventType;
import br.com.urbanrisk.domain.PriorityLevel;
import br.com.urbanrisk.domain.RiskEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UseCasesTest {

    private static final Instant NOW = Instant.parse("2026-11-18T12:00:00Z");

    @Mock RiskEventRepository repository;
    @Mock EventPublisher publisher;
    @Mock ReporterHasher hasher;

    Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
    IngestEvent ingest;

    @BeforeEach
    void setUp() {
        ingest = new IngestEvent(repository, publisher, hasher, clock);
        lenient().when(repository.save(any())).thenAnswer(inv -> {
            RiskEvent e = inv.getArgument(0);
            if (e.getId() == null) e.setId(UUID.randomUUID());
            return e;
        });
    }

    private IngestEventCommand command(EventSource source, int severity, String contact) {
        return new IngestEventCommand(EventType.FLOOD, "Alagamento", "Rua alagada", -23.55, -46.63,
                "Sé", severity, source, "sensor-01", NOW, contact);
    }

    @Test
    void ingestSensorEventComputesScoreAndPublishesAlert() {
        when(repository.findSameTypeBetween(EventType.FLOOD, NOW.minus(Duration.ofHours(1)), NOW.plus(Duration.ofHours(1))))
                .thenReturn(List.of());

        RiskEvent saved = ingest.execute(command(EventSource.SENSOR, 5, null));

        // 0.45 + 0.25*0.9 + 0 + 0.10 = 0.775
        assertThat(saved.getConfidence()).isCloseTo(0.9, within(1e-9));
        assertThat(saved.getPriorityScore()).isCloseTo(77.5, within(1e-9));
        assertThat(saved.getPriorityLevel()).isEqualTo(PriorityLevel.HIGH);
        assertThat(saved.getStatus()).isEqualTo(EventStatus.ACTIVE);
        assertThat(saved.getSourceRef()).isEqualTo("sensor-01");
        assertThat(saved.getReporterHash()).isNull();
        verify(publisher).eventChanged(saved);
        verify(publisher).alertRaised(saved);
        verifyNoInteractions(hasher);
    }

    @Test
    void ingestCorroboratedEventRaisesConfidenceAndCluster() {
        RiskEvent nearby = new RiskEvent();
        nearby.setId(UUID.randomUUID());
        nearby.setType(EventType.FLOOD);
        nearby.setLatitude(-23.551);
        nearby.setLongitude(-46.63);
        nearby.setOccurredAt(NOW.minus(Duration.ofMinutes(10)));
        when(repository.findSameTypeBetween(any(), any(), any())).thenReturn(List.of(nearby, nearby));

        RiskEvent saved = ingest.execute(command(EventSource.COMMUNITY, 1, null));

        assertThat(saved.getConfidence()).isCloseTo(0.7, within(1e-9));
        // 0.45*0.2 + 0.25*0.7 + 0.20*0.4 + 0.10 = 0.445
        assertThat(saved.getPriorityScore()).isCloseTo(44.5, within(1e-9));
        assertThat(saved.getPriorityLevel()).isEqualTo(PriorityLevel.MEDIUM);
        verify(publisher).eventChanged(saved);
        verify(publisher, never()).alertRaised(any());
    }

    @Test
    void ingestCommunityHashesContactAndForcesSourceRef() {
        when(repository.findSameTypeBetween(any(), any(), any())).thenReturn(List.of());
        when(hasher.hash("maria@example.com")).thenReturn("abc123");

        RiskEvent saved = ingest.execute(command(EventSource.COMMUNITY, 2, "  Maria@Example.com "));

        assertThat(saved.getReporterHash()).isEqualTo("abc123");
        assertThat(saved.getSourceRef()).isEqualTo("community");
    }

    @Test
    void ingestBlankContactStoresNoHash() {
        when(repository.findSameTypeBetween(any(), any(), any())).thenReturn(List.of());

        RiskEvent saved = ingest.execute(command(EventSource.COMMUNITY, 2, "   "));

        assertThat(saved.getReporterHash()).isNull();
        verifyNoInteractions(hasher);
    }

    @Test
    void ingestNullOccurredAtDefaultsToNow() {
        when(repository.findSameTypeBetween(any(), any(), any())).thenReturn(List.of());
        IngestEventCommand cmd = new IngestEventCommand(EventType.HEAT_ISLAND, "Calor", "", -23.5, -46.6,
                "Centro", 3, EventSource.PUBLIC_DATA, "inmet", null, null);

        assertThat(ingest.execute(cmd).getOccurredAt()).isEqualTo(NOW);
    }

    @Test
    void listEventsDefaultsToActiveAndClampsSize() {
        ListEvents listEvents = new ListEvents(repository);
        PageResult<RiskEvent> page = new PageResult<>(List.of(), 0, 100, 0, 0);
        when(repository.search(new EventFilter(null, null, EventStatus.ACTIVE), 0, 100)).thenReturn(page);

        assertThat(listEvents.execute(null, null, null, -3, 5000)).isSameAs(page);
    }

    @Test
    void listAlertsDelegatesWithLimit() {
        RiskEvent e = new RiskEvent();
        when(repository.findActiveAlerts(ListAlerts.MAX_ALERTS)).thenReturn(List.of(e));

        assertThat(new ListAlerts(repository).execute()).containsExactly(e);
    }

    @Test
    void resolveMarksResolvedSavesAndPublishesEventOnly() {
        RiskEvent e = new RiskEvent();
        e.setId(UUID.randomUUID());
        e.setPriorityLevel(PriorityLevel.CRITICAL);
        when(repository.findById(e.getId())).thenReturn(Optional.of(e));

        RiskEvent resolved = new ResolveEvent(repository, publisher).execute(e.getId());

        assertThat(resolved.getStatus()).isEqualTo(EventStatus.RESOLVED);
        verify(repository).save(e);
        verify(publisher).eventChanged(e);
        verify(publisher, never()).alertRaised(any());
    }

    @Test
    void resolveUnknownIdThrowsNotFound() {
        UUID id = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> new ResolveEvent(repository, publisher).execute(id))
                .isInstanceOf(EventNotFoundException.class);
        verifyNoInteractions(publisher);
    }

    @Test
    void getEventUnknownIdThrowsNotFound() {
        UUID id = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> new GetEvent(repository).execute(id)).isInstanceOf(EventNotFoundException.class);
    }

    @Test
    void dashboardSummaryZeroFillsAllEnumKeys() {
        when(repository.countActiveByType()).thenReturn(Map.of(EventType.FLOOD, 3L));
        when(repository.countActiveByLevel()).thenReturn(Map.of(PriorityLevel.HIGH, 2L, PriorityLevel.LOW, 1L));
        when(repository.lastUpdatedAt()).thenReturn(Optional.of(NOW));

        DashboardSummary s = new GetDashboardSummary(repository, clock).execute();

        assertThat(s.totalActive()).isEqualTo(3);
        assertThat(s.byType()).containsEntry(EventType.FLOOD, 3L).containsEntry(EventType.HEAT_ISLAND, 0L)
                .containsEntry(EventType.SERVICE_OUTAGE, 0L);
        assertThat(s.byLevel()).hasSize(4).containsEntry(PriorityLevel.CRITICAL, 0L);
        assertThat(s.lastUpdatedAt()).isEqualTo(NOW);
    }

    @Test
    void dashboardSummaryEmptyDbUsesNow() {
        when(repository.countActiveByType()).thenReturn(Map.of());
        when(repository.countActiveByLevel()).thenReturn(Map.of());
        when(repository.lastUpdatedAt()).thenReturn(Optional.empty());

        DashboardSummary s = new GetDashboardSummary(repository, clock).execute();

        assertThat(s.totalActive()).isZero();
        assertThat(s.lastUpdatedAt()).isEqualTo(NOW);
    }

    @Test
    void ingestPassesSaveArgumentWithComputedFields() {
        when(repository.findSameTypeBetween(any(), any(), any())).thenReturn(List.of());
        ingest.execute(command(EventSource.PUBLIC_DATA, 3, null));

        ArgumentCaptor<RiskEvent> captor = ArgumentCaptor.forClass(RiskEvent.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getSource()).isEqualTo(EventSource.PUBLIC_DATA);
        assertThat(captor.getValue().getPriorityLevel()).isNotNull();
    }
}
