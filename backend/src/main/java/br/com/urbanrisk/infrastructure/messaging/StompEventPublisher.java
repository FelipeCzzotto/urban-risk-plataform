package br.com.urbanrisk.infrastructure.messaging;

import br.com.urbanrisk.application.port.EventPublisher;
import br.com.urbanrisk.domain.RiskEvent;
import br.com.urbanrisk.web.dto.EventResponse;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Component
public class StompEventPublisher implements EventPublisher {

    public static final String EVENTS_TOPIC = "/topic/events";
    public static final String ALERTS_TOPIC = "/topic/alerts";

    private final SimpMessagingTemplate template;

    public StompEventPublisher(SimpMessagingTemplate template) {
        this.template = template;
    }

    @Override
    public void eventChanged(RiskEvent event) {
        template.convertAndSend(EVENTS_TOPIC, EventResponse.from(event));
    }

    @Override
    public void alertRaised(RiskEvent event) {
        template.convertAndSend(ALERTS_TOPIC, EventResponse.from(event));
    }
}
