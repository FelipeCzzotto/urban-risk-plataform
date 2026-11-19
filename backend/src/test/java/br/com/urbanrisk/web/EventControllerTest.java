package br.com.urbanrisk.web;

import br.com.urbanrisk.application.DashboardSummary;
import br.com.urbanrisk.application.EventNotFoundException;
import br.com.urbanrisk.application.GetDashboardSummary;
import br.com.urbanrisk.application.GetEvent;
import br.com.urbanrisk.application.IngestEvent;
import br.com.urbanrisk.application.IngestEventCommand;
import br.com.urbanrisk.application.ListAlerts;
import br.com.urbanrisk.application.ListEvents;
import br.com.urbanrisk.application.PageResult;
import br.com.urbanrisk.application.ResolveEvent;
import br.com.urbanrisk.domain.EventSource;
import br.com.urbanrisk.domain.EventStatus;
import br.com.urbanrisk.domain.EventType;
import br.com.urbanrisk.domain.PriorityLevel;
import br.com.urbanrisk.domain.RiskEvent;
import br.com.urbanrisk.infrastructure.security.ApiKeyFilter;
import br.com.urbanrisk.infrastructure.security.CommunityRateLimitFilter;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {EventController.class, AlertController.class, DashboardController.class})
@Import({ApiKeyFilter.class, CommunityRateLimitFilter.class, GlobalExceptionHandler.class})
@TestPropertySource(properties = {"app.ingest-api-key=test-key", "app.rate-limit.community-per-minute=3"})
class EventControllerTest {

    private static final String BODY = """
            {"type":"FLOOD","title":"Alagamento","description":"Rua alagada","latitude":-23.55,
             "longitude":-46.63,"neighborhood":"Sé","severity":4,"sourceRef":"sensor-01",
             "occurredAt":"2026-11-18T12:00:00Z"}""";

    private static final String COMMUNITY_BODY = """
            {"type":"FLOOD","title":"Alagamento","description":"Rua alagada","latitude":-23.55,
             "longitude":-46.63,"neighborhood":"Sé","severity":4,"contact":"maria@example.com"}""";

    @Autowired MockMvc mvc;

    @MockitoBean IngestEvent ingestEvent;
    @MockitoBean ListEvents listEvents;
    @MockitoBean GetEvent getEvent;
    @MockitoBean ResolveEvent resolveEvent;
    @MockitoBean ListAlerts listAlerts;
    @MockitoBean GetDashboardSummary dashboardSummary;

    static RiskEvent sample(double score, PriorityLevel level) {
        RiskEvent e = new RiskEvent();
        e.setId(UUID.randomUUID());
        e.setType(EventType.FLOOD);
        e.setTitle("Alagamento");
        e.setDescription("Rua alagada");
        e.setLatitude(-23.55);
        e.setLongitude(-46.63);
        e.setNeighborhood("Sé");
        e.setSeverity(4);
        e.setSource(EventSource.COMMUNITY);
        e.setSourceRef("community");
        e.setConfidence(0.5);
        e.setPriorityScore(score);
        e.setPriorityLevel(level);
        e.setReporterHash("deadbeef-secret-hash");
        e.setOccurredAt(Instant.parse("2026-11-18T12:00:00Z"));
        e.setCreatedAt(Instant.parse("2026-11-18T12:00:01Z"));
        return e;
    }

    @Test
    void sensorIngestWithKeyReturns201AndDto() throws Exception {
        RiskEvent saved = sample(77.5, PriorityLevel.HIGH);
        when(ingestEvent.execute(any())).thenReturn(saved);

        mvc.perform(post("/api/v1/events/sensor").header("X-API-Key", "test-key")
                        .contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(saved.getId().toString()))
                .andExpect(jsonPath("$.priorityLevel").value("HIGH"))
                .andExpect(jsonPath("$.reporterHash").doesNotExist())
                .andExpect(jsonPath("$.contact").doesNotExist());

        ArgumentCaptor<IngestEventCommand> captor = ArgumentCaptor.forClass(IngestEventCommand.class);
        verify(ingestEvent).execute(captor.capture());
        assertThat(captor.getValue().source()).isEqualTo(EventSource.SENSOR);
        assertThat(captor.getValue().sourceRef()).isEqualTo("sensor-01");
    }

    @Test
    void publicIngestMapsPublicDataSource() throws Exception {
        when(ingestEvent.execute(any())).thenReturn(sample(50, PriorityLevel.MEDIUM));

        mvc.perform(post("/api/v1/events/public").header("X-API-Key", "test-key")
                        .contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isCreated());

        ArgumentCaptor<IngestEventCommand> captor = ArgumentCaptor.forClass(IngestEventCommand.class);
        verify(ingestEvent).execute(captor.capture());
        assertThat(captor.getValue().source()).isEqualTo(EventSource.PUBLIC_DATA);
    }

    @Test
    void sensorIngestWithoutKeyReturns401Problem() throws Exception {
        mvc.perform(post("/api/v1/events/sensor").contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(401));
        verifyNoInteractions(ingestEvent);
    }

    @Test
    void wrongKeyReturns401() throws Exception {
        mvc.perform(post("/api/v1/events/public").header("X-API-Key", "nope")
                        .contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void pathParameterOrEncodingCannotBypassApiKey() throws Exception {
        for (String uri : List.of("/api/v1/events/sensor;x=1", "/api/v1/events/%73ensor", "/api/v1/events//public")) {
            mvc.perform(post(java.net.URI.create(uri)).contentType(MediaType.APPLICATION_JSON).content(BODY))
                    .andExpect(status().isUnauthorized());
        }
        mvc.perform(patch(java.net.URI.create("/api/v1/events/" + UUID.randomUUID() + "/resolve;x=1")))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(ingestEvent, resolveEvent);
    }

    @Test
    void pathParameterCannotBypassCommunityRateLimit() throws Exception {
        when(ingestEvent.execute(any())).thenReturn(sample(40, PriorityLevel.MEDIUM));
        for (int i = 0; i < 4; i++) {
            var result = mvc.perform(post(java.net.URI.create("/api/v1/events/community;n=" + i))
                    .with(r -> { r.setRemoteAddr("10.0.0.77"); return r; })
                    .contentType(MediaType.APPLICATION_JSON).content(COMMUNITY_BODY));
            result.andExpect(status().is(i < 3 ? 201 : 429));
        }
    }

    @Test
    void resolveWithoutKeyReturns401() throws Exception {
        mvc.perform(patch("/api/v1/events/{id}/resolve", UUID.randomUUID()))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(resolveEvent);
    }

    @Test
    void invalidBodyReturns400ProblemWithFieldErrors() throws Exception {
        String bad = """
                {"type":"FLOOD","title":"","latitude":-123,"longitude":-46.63,"neighborhood":"Sé",
                 "severity":9,"sourceRef":"s"}""";
        mvc.perform(post("/api/v1/events/sensor").header("X-API-Key", "test-key")
                        .contentType(MediaType.APPLICATION_JSON).content(bad))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.errors.title").exists())
                .andExpect(jsonPath("$.errors.latitude").exists())
                .andExpect(jsonPath("$.errors.severity").exists());
        verifyNoInteractions(ingestEvent);
    }

    @Test
    void unknownEnumReturns400() throws Exception {
        mvc.perform(post("/api/v1/events/sensor").header("X-API-Key", "test-key")
                        .contentType(MediaType.APPLICATION_JSON).content(BODY.replace("FLOOD", "METEOR")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void communityIngestIsPublicAndPassesContact() throws Exception {
        when(ingestEvent.execute(any())).thenReturn(sample(40, PriorityLevel.MEDIUM));

        mvc.perform(post("/api/v1/events/community").contentType(MediaType.APPLICATION_JSON).content(COMMUNITY_BODY))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.reporterHash").doesNotExist())
                .andExpect(content().string(org.hamcrest.Matchers.not(containsString("maria@example.com"))));

        ArgumentCaptor<IngestEventCommand> captor = ArgumentCaptor.forClass(IngestEventCommand.class);
        verify(ingestEvent).execute(captor.capture());
        assertThat(captor.getValue().source()).isEqualTo(EventSource.COMMUNITY);
        assertThat(captor.getValue().contact()).isEqualTo("maria@example.com");
    }

    @Test
    void communityIngestIsRateLimitedPerClient() throws Exception {
        when(ingestEvent.execute(any())).thenReturn(sample(40, PriorityLevel.MEDIUM));
        for (int i = 0; i < 3; i++) {
            mvc.perform(post("/api/v1/events/community").with(r -> { r.setRemoteAddr("10.0.0.9"); return r; })
                            .contentType(MediaType.APPLICATION_JSON).content(COMMUNITY_BODY))
                    .andExpect(status().isCreated());
        }
        mvc.perform(post("/api/v1/events/community").with(r -> { r.setRemoteAddr("10.0.0.9"); return r; })
                        .contentType(MediaType.APPLICATION_JSON).content(COMMUNITY_BODY))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.status").value(429));
    }

    @Test
    void listEventsReturnsPageResponseShape() throws Exception {
        RiskEvent e = sample(70, PriorityLevel.HIGH);
        when(listEvents.execute(EventType.FLOOD, PriorityLevel.HIGH, EventStatus.ACTIVE, 1, 5))
                .thenReturn(new PageResult<>(List.of(e), 1, 5, 6, 2));

        mvc.perform(get("/api/v1/events?type=FLOOD&level=HIGH&status=ACTIVE&page=1&size=5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].reporterHash").doesNotExist())
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.size").value(5))
                .andExpect(jsonPath("$.totalElements").value(6))
                .andExpect(jsonPath("$.totalPages").value(2));
    }

    @Test
    void listEventsDefaultsPageAndSize() throws Exception {
        when(listEvents.execute(null, null, null, 0, 20)).thenReturn(new PageResult<>(List.of(), 0, 20, 0, 0));

        mvc.perform(get("/api/v1/events")).andExpect(status().isOk()).andExpect(jsonPath("$.size").value(20));
    }

    @Test
    void invalidFilterEnumReturns400() throws Exception {
        mvc.perform(get("/api/v1/events?level=EXTREME")).andExpect(status().isBadRequest());
    }

    @Test
    void getEventNotFoundReturns404Problem() throws Exception {
        UUID id = UUID.randomUUID();
        when(getEvent.execute(id)).thenThrow(new EventNotFoundException(id));

        mvc.perform(get("/api/v1/events/{id}", id))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void getEventMalformedIdReturns400() throws Exception {
        mvc.perform(get("/api/v1/events/not-a-uuid")).andExpect(status().isBadRequest());
    }

    @Test
    void resolveWithKeyReturnsResolvedDto() throws Exception {
        RiskEvent e = sample(85, PriorityLevel.CRITICAL);
        e.resolve();
        when(resolveEvent.execute(e.getId())).thenReturn(e);

        mvc.perform(patch("/api/v1/events/{id}/resolve", e.getId()).header("X-API-Key", "test-key"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RESOLVED"));
    }

    @Test
    void alertsReturnsPlainListPreservingPriorityOrder() throws Exception {
        when(listAlerts.execute()).thenReturn(List.of(sample(95, PriorityLevel.CRITICAL), sample(65, PriorityLevel.HIGH)));

        mvc.perform(get("/api/v1/alerts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].priorityScore").value(95.0))
                .andExpect(jsonPath("$[1].priorityScore").value(65.0))
                .andExpect(jsonPath("$[0].reporterHash").doesNotExist());
    }

    @Test
    void dashboardSummaryShape() throws Exception {
        Map<EventType, Long> byType = new EnumMap<>(EventType.class);
        byType.put(EventType.FLOOD, 2L);
        byType.put(EventType.HEAT_ISLAND, 0L);
        byType.put(EventType.SERVICE_OUTAGE, 1L);
        Map<PriorityLevel, Long> byLevel = new EnumMap<>(PriorityLevel.class);
        for (PriorityLevel l : PriorityLevel.values()) byLevel.put(l, 0L);
        byLevel.put(PriorityLevel.HIGH, 3L);
        when(dashboardSummary.execute())
                .thenReturn(new DashboardSummary(3, byType, byLevel, Instant.parse("2026-11-18T12:00:00Z")));

        mvc.perform(get("/api/v1/dashboard/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalActive").value(3))
                .andExpect(jsonPath("$.byType.FLOOD").value(2))
                .andExpect(jsonPath("$.byLevel.HIGH").value(3))
                .andExpect(jsonPath("$.lastUpdatedAt").value("2026-11-18T12:00:00Z"));
    }
}
