package br.com.urbanrisk.web;

import br.com.urbanrisk.application.DashboardSummary;
import br.com.urbanrisk.application.GetDashboardSummary;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "Dashboard")
public class DashboardController {

    private final GetDashboardSummary getDashboardSummary;

    public DashboardController(GetDashboardSummary getDashboardSummary) {
        this.getDashboardSummary = getDashboardSummary;
    }

    @GetMapping("/api/v1/dashboard/summary")
    @Operation(summary = "Active totals by type and level")
    public DashboardSummary summary() {
        return getDashboardSummary.execute();
    }
}
