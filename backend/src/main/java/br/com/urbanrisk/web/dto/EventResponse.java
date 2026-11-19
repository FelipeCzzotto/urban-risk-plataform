package br.com.urbanrisk.web.dto;

import br.com.urbanrisk.domain.EventSource;
import br.com.urbanrisk.domain.EventStatus;
import br.com.urbanrisk.domain.EventType;
import br.com.urbanrisk.domain.PriorityLevel;
import br.com.urbanrisk.domain.RiskEvent;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

@Schema(name = "EventDTO", description = "Risk event. Reporter identity (hash/contact) is never exposed.")
public record EventResponse(
        UUID id,
        EventType type,
        String title,
        String description,
        double latitude,
        double longitude,
        String neighborhood,
        int severity,
        EventSource source,
        String sourceRef,
        @Schema(description = "0..1, from source and corroboration") double confidence,
        @Schema(description = "0..100, weighted severity/confidence/cluster/recency") double priorityScore,
        PriorityLevel priorityLevel,
        EventStatus status,
        Instant occurredAt,
        Instant createdAt) {

    public static EventResponse from(RiskEvent e) {
        return new EventResponse(e.getId(), e.getType(), e.getTitle(), e.getDescription(), e.getLatitude(),
                e.getLongitude(), e.getNeighborhood(), e.getSeverity(), e.getSource(), e.getSourceRef(),
                e.getConfidence(), e.getPriorityScore(), e.getPriorityLevel(), e.getStatus(), e.getOccurredAt(),
                e.getCreatedAt());
    }
}
