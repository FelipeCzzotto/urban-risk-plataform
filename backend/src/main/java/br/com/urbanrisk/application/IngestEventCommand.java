package br.com.urbanrisk.application;

import br.com.urbanrisk.domain.EventSource;
import br.com.urbanrisk.domain.EventType;

import java.time.Instant;

/** occurredAt null = now; contact only meaningful for COMMUNITY. */
public record IngestEventCommand(EventType type, String title, String description, double latitude, double longitude,
                                 String neighborhood, int severity, EventSource source, String sourceRef,
                                 Instant occurredAt, String contact) {

    @Override
    public String toString() {
        // never leak contact into logs
        return "IngestEventCommand[type=" + type + ", source=" + source + ", sourceRef=" + sourceRef + "]";
    }
}
