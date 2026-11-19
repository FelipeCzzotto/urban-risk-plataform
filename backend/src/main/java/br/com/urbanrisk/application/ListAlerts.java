package br.com.urbanrisk.application;

import br.com.urbanrisk.application.port.RiskEventRepository;
import br.com.urbanrisk.domain.RiskEvent;

import java.util.List;

public class ListAlerts {

    public static final int MAX_ALERTS = 50;

    private final RiskEventRepository repository;

    public ListAlerts(RiskEventRepository repository) {
        this.repository = repository;
    }

    public List<RiskEvent> execute() {
        return repository.findActiveAlerts(MAX_ALERTS);
    }
}
