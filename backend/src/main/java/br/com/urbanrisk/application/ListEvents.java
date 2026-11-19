package br.com.urbanrisk.application;

import br.com.urbanrisk.application.port.RiskEventRepository;
import br.com.urbanrisk.domain.EventStatus;
import br.com.urbanrisk.domain.EventType;
import br.com.urbanrisk.domain.PriorityLevel;
import br.com.urbanrisk.domain.RiskEvent;

public class ListEvents {

    public static final int MAX_PAGE_SIZE = 100;

    private final RiskEventRepository repository;

    public ListEvents(RiskEventRepository repository) {
        this.repository = repository;
    }

    /** status null defaults to ACTIVE (contract default). */
    public PageResult<RiskEvent> execute(EventType type, PriorityLevel level, EventStatus status, int page, int size) {
        EventFilter filter = new EventFilter(type, level, status == null ? EventStatus.ACTIVE : status);
        return repository.search(filter, Math.max(0, page), Math.clamp(size, 1, MAX_PAGE_SIZE));
    }
}
