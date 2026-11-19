package br.com.urbanrisk.application;

import br.com.urbanrisk.application.port.EventPublisher;
import br.com.urbanrisk.application.port.RiskEventRepository;
import br.com.urbanrisk.domain.RiskEvent;

import java.util.UUID;

public class ResolveEvent {

    private final RiskEventRepository repository;
    private final EventPublisher publisher;

    public ResolveEvent(RiskEventRepository repository, EventPublisher publisher) {
        this.repository = repository;
        this.publisher = publisher;
    }

    /** Idempotent: resolving a resolved event re-publishes the same state. */
    public RiskEvent execute(UUID id) {
        RiskEvent event = repository.findById(id).orElseThrow(() -> new EventNotFoundException(id));
        event.resolve();
        repository.save(event);
        publisher.eventChanged(event);
        return event;
    }
}
