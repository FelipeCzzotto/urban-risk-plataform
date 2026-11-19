package br.com.urbanrisk.infrastructure.persistence;

import br.com.urbanrisk.domain.EventSource;
import br.com.urbanrisk.domain.EventStatus;
import br.com.urbanrisk.domain.EventType;
import br.com.urbanrisk.domain.PriorityLevel;
import br.com.urbanrisk.domain.RiskEvent;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.Generated;

import java.time.Instant;
import java.util.UUID;

/** Mirrors V1__create_risk_events.sql (ddl-auto=validate). */
@Entity
@Table(name = "risk_events")
public class RiskEventEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "text")
    private EventType type;

    @Column(nullable = false, columnDefinition = "text")
    private String title;

    @Column(nullable = false, columnDefinition = "text")
    private String description;

    @Column(nullable = false)
    private double latitude;

    @Column(nullable = false)
    private double longitude;

    @Column(nullable = false, columnDefinition = "text")
    private String neighborhood;

    @Column(nullable = false)
    private short severity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "text")
    private EventSource source;

    @Column(name = "source_ref", nullable = false, columnDefinition = "text")
    private String sourceRef;

    @Column(nullable = false)
    private double confidence;

    @Column(name = "priority_score", nullable = false)
    private double priorityScore;

    @Enumerated(EnumType.STRING)
    @Column(name = "priority_level", nullable = false, columnDefinition = "text")
    private PriorityLevel priorityLevel;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "text")
    private EventStatus status;

    @Column(name = "reporter_hash", columnDefinition = "text")
    private String reporterHash;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    /** DB default now(); read back after insert. */
    @Generated
    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private Instant createdAt;

    protected RiskEventEntity() {
    }

    static RiskEventEntity fromDomain(RiskEvent e) {
        RiskEventEntity x = new RiskEventEntity();
        x.id = e.getId();
        x.type = e.getType();
        x.title = e.getTitle();
        x.description = e.getDescription();
        x.latitude = e.getLatitude();
        x.longitude = e.getLongitude();
        x.neighborhood = e.getNeighborhood();
        x.severity = (short) e.getSeverity();
        x.source = e.getSource();
        x.sourceRef = e.getSourceRef();
        x.confidence = e.getConfidence();
        x.priorityScore = e.getPriorityScore();
        x.priorityLevel = e.getPriorityLevel();
        x.status = e.getStatus();
        x.reporterHash = e.getReporterHash();
        x.occurredAt = e.getOccurredAt();
        x.createdAt = e.getCreatedAt();
        return x;
    }

    RiskEvent toDomain() {
        RiskEvent e = new RiskEvent();
        e.setId(id);
        e.setType(type);
        e.setTitle(title);
        e.setDescription(description);
        e.setLatitude(latitude);
        e.setLongitude(longitude);
        e.setNeighborhood(neighborhood);
        e.setSeverity(severity);
        e.setSource(source);
        e.setSourceRef(sourceRef);
        e.setConfidence(confidence);
        e.setPriorityScore(priorityScore);
        e.setPriorityLevel(priorityLevel);
        e.setStatus(status);
        e.setReporterHash(reporterHash);
        e.setOccurredAt(occurredAt);
        e.setCreatedAt(createdAt);
        return e;
    }

    public String getReporterHash() { return reporterHash; }
    public String getSourceRef() { return sourceRef; }
    public String getDescription() { return description; }
}
