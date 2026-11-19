package br.com.urbanrisk.application.port;

import br.com.urbanrisk.application.EventFilter;
import br.com.urbanrisk.application.PageResult;
import br.com.urbanrisk.domain.EventType;
import br.com.urbanrisk.domain.PriorityLevel;
import br.com.urbanrisk.domain.RiskEvent;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public interface RiskEventRepository {

    RiskEvent save(RiskEvent event);

    Optional<RiskEvent> findById(UUID id);

    /** Same-type events with occurredAt in [from, to]; spatial filter happens in the domain. */
    List<RiskEvent> findSameTypeBetween(EventType type, Instant from, Instant to);

    /** Sorted by priorityScore desc, occurredAt desc. */
    PageResult<RiskEvent> search(EventFilter filter, int page, int size);

    /** ACTIVE + HIGH|CRITICAL, sorted by priorityScore desc. */
    List<RiskEvent> findActiveAlerts(int limit);

    Map<EventType, Long> countActiveByType();

    Map<PriorityLevel, Long> countActiveByLevel();

    Optional<Instant> lastUpdatedAt();
}
