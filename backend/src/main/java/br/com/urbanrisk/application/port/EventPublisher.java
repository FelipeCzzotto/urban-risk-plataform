package br.com.urbanrisk.application.port;

import br.com.urbanrisk.domain.RiskEvent;

public interface EventPublisher {

    /** Created or updated (incl. resolved). */
    void eventChanged(RiskEvent event);

    /** Event reached HIGH|CRITICAL. */
    void alertRaised(RiskEvent event);
}
