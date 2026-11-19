package br.com.urbanrisk.infrastructure;

import br.com.urbanrisk.application.IngestEvent;
import br.com.urbanrisk.application.IngestEventCommand;
import br.com.urbanrisk.domain.EventSource;
import br.com.urbanrisk.infrastructure.simulator.SensorSimulator;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class SensorSimulatorTest {

    @Test
    void emitsValidSensorEventsAroundSaoPaulo() {
        IngestEvent ingest = mock(IngestEvent.class);
        SensorSimulator simulator = new SensorSimulator(ingest, new Random(42));

        for (int i = 0; i < 50; i++) simulator.tick();

        ArgumentCaptor<IngestEventCommand> captor = ArgumentCaptor.forClass(IngestEventCommand.class);
        verify(ingest, times(50)).execute(captor.capture());
        assertThat(captor.getAllValues()).allSatisfy(c -> {
            assertThat(c.source()).isEqualTo(EventSource.SENSOR);
            assertThat(c.latitude()).isBetween(-23.75, -23.35);
            assertThat(c.longitude()).isBetween(-46.85, -46.40);
            assertThat(c.severity()).isBetween(1, 5);
            assertThat(c.title()).isNotBlank().hasSizeLessThanOrEqualTo(120);
            assertThat(c.sourceRef()).startsWith("sim-sensor-");
        });
    }

    @Test
    void tickSwallowsFailuresSoSchedulerKeepsRunning() {
        IngestEvent ingest = mock(IngestEvent.class);
        org.mockito.Mockito.when(ingest.execute(org.mockito.ArgumentMatchers.any())).thenThrow(new RuntimeException("db down"));

        new SensorSimulator(ingest, new Random(1)).tick();
    }
}
