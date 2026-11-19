package br.com.urbanrisk.application;

import br.com.urbanrisk.application.port.RiskEventRepository;
import br.com.urbanrisk.domain.RiskEvent;

import java.util.UUID;

public class GetEvent {

    private final RiskEventRepository repository;

    public GetEvent(RiskEventRepository repository) {
        this.repository = repository;
    }

    public RiskEvent execute(UUID id) {
        return repository.findById(id).orElseThrow(() -> new EventNotFoundException(id));
    }
}
