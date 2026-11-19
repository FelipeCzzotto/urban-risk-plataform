package br.com.urbanrisk.web;

import br.com.urbanrisk.application.ListAlerts;
import br.com.urbanrisk.web.dto.EventResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@Tag(name = "Alerts")
public class AlertController {

    private final ListAlerts listAlerts;

    public AlertController(ListAlerts listAlerts) {
        this.listAlerts = listAlerts;
    }

    @GetMapping("/api/v1/alerts")
    @Operation(summary = "ACTIVE HIGH|CRITICAL events, priorityScore desc (max 50)")
    public List<EventResponse> alerts() {
        return listAlerts.execute().stream().map(EventResponse::from).toList();
    }
}
