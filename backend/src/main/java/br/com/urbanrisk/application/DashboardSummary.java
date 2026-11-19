package br.com.urbanrisk.application;

import br.com.urbanrisk.domain.EventType;
import br.com.urbanrisk.domain.PriorityLevel;

import java.time.Instant;
import java.util.Map;

public record DashboardSummary(long totalActive, Map<EventType, Long> byType, Map<PriorityLevel, Long> byLevel,
                               Instant lastUpdatedAt) {
}
