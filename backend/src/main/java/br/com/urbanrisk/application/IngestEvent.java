package br.com.urbanrisk.application;

import br.com.urbanrisk.application.port.EventPublisher;
import br.com.urbanrisk.application.port.ReporterHasher;
import br.com.urbanrisk.application.port.RiskEventRepository;
import br.com.urbanrisk.domain.ConfidenceCalculator;
import br.com.urbanrisk.domain.EventSource;
import br.com.urbanrisk.domain.PriorityAnalysis;
import br.com.urbanrisk.domain.PriorityAnalyzer;
import br.com.urbanrisk.domain.RiskEvent;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Locale;

public class IngestEvent {

    public static final String COMMUNITY_SOURCE_REF = "community";

    private final RiskEventRepository repository;
    private final EventPublisher publisher;
    private final ReporterHasher hasher;
    private final Clock clock;

    public IngestEvent(RiskEventRepository repository, EventPublisher publisher, ReporterHasher hasher, Clock clock) {
        this.repository = repository;
        this.publisher = publisher;
        this.hasher = hasher;
        this.clock = clock;
    }

    public RiskEvent execute(IngestEventCommand cmd) {
        Instant now = clock.instant();
        RiskEvent event = new RiskEvent();
        event.setType(cmd.type());
        event.setTitle(cmd.title());
        event.setDescription(cmd.description() == null ? "" : cmd.description());
        event.setLatitude(cmd.latitude());
        event.setLongitude(cmd.longitude());
        event.setNeighborhood(cmd.neighborhood());
        event.setSeverity(cmd.severity());
        event.setSource(cmd.source());
        event.setOccurredAt(cmd.occurredAt() == null ? now : cmd.occurredAt());
        event.setCreatedAt(now);
        if (cmd.source() == EventSource.COMMUNITY) {
            event.setSourceRef(COMMUNITY_SOURCE_REF);
            if (cmd.contact() != null && !cmd.contact().isBlank()) {
                event.setReporterHash(hasher.hash(cmd.contact().strip().toLowerCase(Locale.ROOT)));
            }
        } else {
            event.setSourceRef(cmd.sourceRef());
        }

        // Same nearby set drives corroboration (confidence) and clustering (priority), per contract.
        Duration window = PriorityAnalyzer.CLUSTER_WINDOW;
        List<RiskEvent> candidates = repository.findSameTypeBetween(
                event.getType(), event.getOccurredAt().minus(window), event.getOccurredAt().plus(window));
        int nearby = PriorityAnalyzer.countNearby(event, candidates);
        double confidence = ConfidenceCalculator.calculate(event.getSource(), nearby);
        double ageHours = Duration.between(event.getOccurredAt(), now).toMillis() / 3_600_000.0;
        PriorityAnalysis analysis = PriorityAnalyzer.analyze(event.getSeverity(), confidence, nearby, ageHours);
        event.applyAnalysis(confidence, analysis);

        RiskEvent saved = repository.save(event);
        publisher.eventChanged(saved);
        if (saved.isAlert()) {
            publisher.alertRaised(saved);
        }
        return saved;
    }
}
