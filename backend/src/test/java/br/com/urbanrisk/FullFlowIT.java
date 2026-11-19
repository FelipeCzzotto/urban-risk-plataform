package br.com.urbanrisk;

import br.com.urbanrisk.infrastructure.PostgresContainerConfig;
import br.com.urbanrisk.infrastructure.persistence.RiskEventJpaRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;

import java.lang.reflect.Type;
import java.time.Instant;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Real HTTP + real STOMP + Testcontainers Postgres with Flyway migrations and ddl validate. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(PostgresContainerConfig.class)
@TestPropertySource(properties = {
        "app.ingest-api-key=it-key",
        "app.reporter-hash-pepper=it-pepper",
        "app.simulator.enabled=false",
        "spring.config.import="})
class FullFlowIT {

    @LocalServerPort int port;
    @Autowired RiskEventJpaRepository jpa;
    @Autowired ObjectMapper mapper;

    RestClient http;
    WebSocketStompClient stomp;
    StompSession session;
    final BlockingQueue<JsonNode> events = new LinkedBlockingQueue<>();
    final BlockingQueue<JsonNode> alerts = new LinkedBlockingQueue<>();

    @BeforeEach
    void setUp() throws Exception {
        jpa.deleteAll();
        http = RestClient.builder().baseUrl("http://localhost:" + port + "/api/v1").build();
        stomp = new WebSocketStompClient(new StandardWebSocketClient());
        stomp.setMessageConverter(new MappingJackson2MessageConverter());
        session = stomp.connectAsync("ws://localhost:" + port + "/ws", new StompSessionHandlerAdapter() {
        }).get(5, TimeUnit.SECONDS);
        session.subscribe("/topic/events", collector(events));
        session.subscribe("/topic/alerts", collector(alerts));
        Thread.sleep(200); // let SUBSCRIBE frames reach the broker
    }

    @AfterEach
    void tearDown() {
        session.disconnect();
        stomp.stop();
    }

    private StompFrameHandler collector(BlockingQueue<JsonNode> queue) {
        return new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return JsonNode.class;
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                queue.add((JsonNode) payload);
            }
        };
    }

    private String body(int severity) {
        return """
                {"type":"FLOOD","title":"Alagamento","description":"Rua alagada","latitude":-23.55,
                 "longitude":-46.63,"neighborhood":"Sé","severity":%d,"sourceRef":"sensor-01",
                 "occurredAt":"%s"}""".formatted(severity, Instant.now());
    }

    @Test
    void ingestPersistsPublishesListsAndResolves() throws Exception {
        JsonNode created = mapper.readTree(http.post().uri("/events/sensor").header("X-API-Key", "it-key")
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON).body(body(5))
                .retrieve().body(String.class));
        String id = created.get("id").asText();
        assertThat(created.get("priorityLevel").asText()).isEqualTo("HIGH");
        assertThat(created.has("reporterHash")).isFalse();
        assertThat(jpa.count()).isEqualTo(1);

        JsonNode pushed = events.poll(5, TimeUnit.SECONDS);
        assertThat(pushed).isNotNull();
        assertThat(pushed.get("id").asText()).isEqualTo(id);
        JsonNode alert = alerts.poll(5, TimeUnit.SECONDS);
        assertThat(alert).isNotNull();
        assertThat(alert.has("reporterHash")).isFalse();

        JsonNode alertList = mapper.readTree(http.get().uri("/alerts").retrieve().body(String.class));
        assertThat(alertList.isArray()).isTrue();
        assertThat(alertList.get(0).get("id").asText()).isEqualTo(id);

        JsonNode page = mapper.readTree(http.get().uri("/events").retrieve().body(String.class));
        assertThat(page.get("totalElements").asLong()).isEqualTo(1);

        JsonNode summary = mapper.readTree(http.get().uri("/dashboard/summary").retrieve().body(String.class));
        assertThat(summary.get("totalActive").asLong()).isEqualTo(1);
        assertThat(summary.get("byType").get("FLOOD").asLong()).isEqualTo(1);

        JsonNode resolved = mapper.readTree(http.patch().uri("/events/{id}/resolve", id).header("X-API-Key", "it-key")
                .retrieve().body(String.class));
        assertThat(resolved.get("status").asText()).isEqualTo("RESOLVED");
        JsonNode resolvedPush = events.poll(5, TimeUnit.SECONDS);
        assertThat(resolvedPush).isNotNull();
        assertThat(resolvedPush.get("status").asText()).isEqualTo("RESOLVED");
        assertThat(mapper.readTree(http.get().uri("/alerts").retrieve().body(String.class))).isEmpty();
    }

    @Test
    void communityContactIsHashedNeverStoredRaw() {
        http.post().uri("/events/community").contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .body("""
                        {"type":"HEAT_ISLAND","title":"Calor","latitude":-23.55,"longitude":-46.63,
                         "neighborhood":"Centro","severity":2,"contact":"Maria@Example.com"}""")
                .retrieve().toBodilessEntity();

        var stored = jpa.findAll().getFirst();
        assertThat(stored.getReporterHash()).hasSize(64).doesNotContainIgnoringCase("maria");
        assertThat(stored.getSourceRef()).isEqualTo("community");
        assertThat(stored.getDescription()).isEmpty();
    }

    @Test
    void lowPriorityEventIsNotPushedAsAlert() throws Exception {
        http.post().uri("/events/public").header("X-API-Key", "it-key")
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON).body(body(1))
                .retrieve().toBodilessEntity();

        assertThat(events.poll(5, TimeUnit.SECONDS)).isNotNull();
        assertThat(alerts.poll(500, TimeUnit.MILLISECONDS)).isNull();
    }

    @Test
    void missingKeyIs401AndHealthIsUp() {
        assertThatThrownBy(() -> http.post().uri("/events/sensor")
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON).body(body(3))
                .retrieve().toBodilessEntity())
                .isInstanceOfSatisfying(HttpClientErrorException.class,
                        e -> assertThat(e.getStatusCode().value()).isEqualTo(401));

        String health = RestClient.create("http://localhost:" + port).get().uri("/actuator/health")
                .retrieve().body(String.class);
        assertThat(health).contains("\"status\":\"UP\"");
    }
}
