package br.com.urbanrisk.infrastructure;

import br.com.urbanrisk.domain.RiskEvent;
import br.com.urbanrisk.infrastructure.messaging.StompEventPublisher;
import br.com.urbanrisk.web.dto.EventResponse;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class StompEventPublisherTest {

    @Test
    void publishesDtoToContractTopics() {
        SimpMessagingTemplate template = mock(SimpMessagingTemplate.class);
        StompEventPublisher publisher = new StompEventPublisher(template);
        RiskEvent e = new RiskEvent();
        e.setId(UUID.randomUUID());
        e.setReporterHash("secret");

        publisher.eventChanged(e);
        publisher.alertRaised(e);

        ArgumentCaptor<Object> payload = ArgumentCaptor.forClass(Object.class);
        verify(template).convertAndSend(eq("/topic/events"), payload.capture());
        verify(template).convertAndSend(eq("/topic/alerts"), payload.capture());
        assertThat(payload.getAllValues()).allSatisfy(p -> {
            assertThat(p).isInstanceOf(EventResponse.class);
            assertThat(((EventResponse) p).id()).isEqualTo(e.getId());
        });
    }
}
