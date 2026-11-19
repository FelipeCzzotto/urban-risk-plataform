package br.com.urbanrisk.domain;

import java.time.Instant;
import java.util.UUID;

/** Pure domain model (no framework annotations). Persistence mapping lives in infrastructure. */
public class RiskEvent {

    private UUID id;
    private EventType type;
    private String title;
    private String description;
    private double latitude;
    private double longitude;
    private String neighborhood;
    private int severity;
    private EventSource source;
    private String sourceRef;
    private double confidence;
    private double priorityScore;
    private PriorityLevel priorityLevel;
    private EventStatus status = EventStatus.ACTIVE;
    private String reporterHash;
    private Instant occurredAt;
    private Instant createdAt;

    public void applyAnalysis(double confidence, PriorityAnalysis analysis) {
        this.confidence = confidence;
        this.priorityScore = analysis.score();
        this.priorityLevel = analysis.level();
    }

    public void resolve() {
        this.status = EventStatus.RESOLVED;
    }

    public boolean isAlert() {
        return status == EventStatus.ACTIVE && priorityLevel != null && priorityLevel.isAlert();
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public EventType getType() { return type; }
    public void setType(EventType type) { this.type = type; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public double getLatitude() { return latitude; }
    public void setLatitude(double latitude) { this.latitude = latitude; }
    public double getLongitude() { return longitude; }
    public void setLongitude(double longitude) { this.longitude = longitude; }
    public String getNeighborhood() { return neighborhood; }
    public void setNeighborhood(String neighborhood) { this.neighborhood = neighborhood; }
    public int getSeverity() { return severity; }
    public void setSeverity(int severity) { this.severity = severity; }
    public EventSource getSource() { return source; }
    public void setSource(EventSource source) { this.source = source; }
    public String getSourceRef() { return sourceRef; }
    public void setSourceRef(String sourceRef) { this.sourceRef = sourceRef; }
    public double getConfidence() { return confidence; }
    public void setConfidence(double confidence) { this.confidence = confidence; }
    public double getPriorityScore() { return priorityScore; }
    public void setPriorityScore(double priorityScore) { this.priorityScore = priorityScore; }
    public PriorityLevel getPriorityLevel() { return priorityLevel; }
    public void setPriorityLevel(PriorityLevel priorityLevel) { this.priorityLevel = priorityLevel; }
    public EventStatus getStatus() { return status; }
    public void setStatus(EventStatus status) { this.status = status; }
    public String getReporterHash() { return reporterHash; }
    public void setReporterHash(String reporterHash) { this.reporterHash = reporterHash; }
    public Instant getOccurredAt() { return occurredAt; }
    public void setOccurredAt(Instant occurredAt) { this.occurredAt = occurredAt; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
