package br.com.urbanrisk.web;

import br.com.urbanrisk.application.GetEvent;
import br.com.urbanrisk.application.IngestEvent;
import br.com.urbanrisk.application.ListEvents;
import br.com.urbanrisk.application.ResolveEvent;
import br.com.urbanrisk.domain.EventSource;
import br.com.urbanrisk.domain.EventStatus;
import br.com.urbanrisk.domain.EventType;
import br.com.urbanrisk.domain.PriorityLevel;
import br.com.urbanrisk.web.dto.CommunityEventRequest;
import br.com.urbanrisk.web.dto.EventIngestRequest;
import br.com.urbanrisk.web.dto.EventResponse;
import br.com.urbanrisk.web.dto.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/events")
@Tag(name = "Events")
public class EventController {

    private final IngestEvent ingestEvent;
    private final ListEvents listEvents;
    private final GetEvent getEvent;
    private final ResolveEvent resolveEvent;

    public EventController(IngestEvent ingestEvent, ListEvents listEvents, GetEvent getEvent, ResolveEvent resolveEvent) {
        this.ingestEvent = ingestEvent;
        this.listEvents = listEvents;
        this.getEvent = getEvent;
        this.resolveEvent = resolveEvent;
    }

    @PostMapping("/sensor")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Ingest sensor event", parameters = @Parameter(in = ParameterIn.HEADER, name = "X-API-Key", required = true))
    public EventResponse ingestSensor(@Valid @RequestBody EventIngestRequest body) {
        return EventResponse.from(ingestEvent.execute(body.toCommand(EventSource.SENSOR)));
    }

    @PostMapping("/public")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Ingest public-data event", parameters = @Parameter(in = ParameterIn.HEADER, name = "X-API-Key", required = true))
    public EventResponse ingestPublic(@Valid @RequestBody EventIngestRequest body) {
        return EventResponse.from(ingestEvent.execute(body.toCommand(EventSource.PUBLIC_DATA)));
    }

    @PostMapping("/community")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Community report (public, rate-limited; contact is hashed, never stored raw)")
    public EventResponse ingestCommunity(@Valid @RequestBody CommunityEventRequest body) {
        return EventResponse.from(ingestEvent.execute(body.toCommand()));
    }

    @GetMapping
    @Operation(summary = "List events sorted by priorityScore desc, occurredAt desc (status defaults to ACTIVE)")
    public PageResponse<EventResponse> list(@RequestParam(required = false) EventType type,
                                            @RequestParam(required = false) PriorityLevel level,
                                            @RequestParam(required = false) EventStatus status,
                                            @RequestParam(defaultValue = "0") int page,
                                            @RequestParam(defaultValue = "20") int size) {
        return PageResponse.from(listEvents.execute(type, level, status, page, size).map(EventResponse::from));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get event by id")
    public EventResponse get(@PathVariable UUID id) {
        return EventResponse.from(getEvent.execute(id));
    }

    @PatchMapping("/{id}/resolve")
    @Operation(summary = "Mark event resolved", parameters = @Parameter(in = ParameterIn.HEADER, name = "X-API-Key", required = true))
    public EventResponse resolve(@PathVariable UUID id) {
        return EventResponse.from(resolveEvent.execute(id));
    }
}
