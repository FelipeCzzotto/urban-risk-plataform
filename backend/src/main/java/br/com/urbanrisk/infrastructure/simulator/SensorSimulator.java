package br.com.urbanrisk.infrastructure.simulator;

import br.com.urbanrisk.application.IngestEvent;
import br.com.urbanrisk.application.IngestEventCommand;
import br.com.urbanrisk.domain.EventSource;
import br.com.urbanrisk.domain.EventType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Random;

/** Emits a synthetic sensor event every 10s around São Paulo. Enabled by app.simulator.enabled (dev profile). */
@Component
@ConditionalOnProperty(name = "app.simulator.enabled", havingValue = "true")
public class SensorSimulator {

    private static final Logger log = LoggerFactory.getLogger(SensorSimulator.class);

    private record Place(String neighborhood, double lat, double lng) {
    }

    private static final List<Place> PLACES = List.of(
            new Place("Sé", -23.5505, -46.6333),
            new Place("Pinheiros", -23.5670, -46.6920),
            new Place("Mooca", -23.5600, -46.5990),
            new Place("Santana", -23.5020, -46.6250),
            new Place("Itaquera", -23.5400, -46.4560),
            new Place("Santo Amaro", -23.6540, -46.7100),
            new Place("Lapa", -23.5270, -46.7040));

    private static final Map<EventType, String> TITLES = Map.of(
            EventType.FLOOD, "Nível de água elevado",
            EventType.HEAT_ISLAND, "Temperatura acima do normal",
            EventType.SERVICE_OUTAGE, "Falha no fornecimento de energia");

    private final IngestEvent ingestEvent;
    private final Random random;

    @Autowired
    public SensorSimulator(IngestEvent ingestEvent) {
        this(ingestEvent, new Random());
    }

    public SensorSimulator(IngestEvent ingestEvent, Random random) {
        this.ingestEvent = ingestEvent;
        this.random = random;
    }

    @Scheduled(fixedRate = 10_000, initialDelay = 5_000)
    public void tick() {
        Place p = PLACES.get(random.nextInt(PLACES.size()));
        EventType type = EventType.values()[random.nextInt(EventType.values().length)];
        // ~550m jitter keeps events clustered enough to exercise corroboration
        double lat = p.lat() + (random.nextDouble() - 0.5) * 0.01;
        double lng = p.lng() + (random.nextDouble() - 0.5) * 0.01;
        int severity = 1 + random.nextInt(5);
        String sensorId = "sim-sensor-" + (1 + random.nextInt(20));
        try {
            ingestEvent.execute(new IngestEventCommand(type, TITLES.get(type), "Leitura simulada de " + sensorId,
                    lat, lng, p.neighborhood(), severity, EventSource.SENSOR, sensorId, Instant.now(), null));
        } catch (RuntimeException e) {
            log.warn("Simulator tick failed: {}", e.getMessage());
        }
    }
}
