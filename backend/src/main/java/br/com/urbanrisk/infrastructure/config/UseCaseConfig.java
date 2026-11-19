package br.com.urbanrisk.infrastructure.config;

import br.com.urbanrisk.application.GetDashboardSummary;
import br.com.urbanrisk.application.GetEvent;
import br.com.urbanrisk.application.IngestEvent;
import br.com.urbanrisk.application.ListAlerts;
import br.com.urbanrisk.application.ListEvents;
import br.com.urbanrisk.application.ResolveEvent;
import br.com.urbanrisk.application.port.EventPublisher;
import br.com.urbanrisk.application.port.ReporterHasher;
import br.com.urbanrisk.application.port.RiskEventRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/** Wires framework-free use cases. */
@Configuration(proxyBeanMethods = false)
public class UseCaseConfig {

    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    IngestEvent ingestEvent(RiskEventRepository r, EventPublisher p, ReporterHasher h, Clock c) {
        return new IngestEvent(r, p, h, c);
    }

    @Bean
    ListEvents listEvents(RiskEventRepository r) {
        return new ListEvents(r);
    }

    @Bean
    GetEvent getEvent(RiskEventRepository r) {
        return new GetEvent(r);
    }

    @Bean
    ResolveEvent resolveEvent(RiskEventRepository r, EventPublisher p) {
        return new ResolveEvent(r, p);
    }

    @Bean
    ListAlerts listAlerts(RiskEventRepository r) {
        return new ListAlerts(r);
    }

    @Bean
    GetDashboardSummary getDashboardSummary(RiskEventRepository r, Clock c) {
        return new GetDashboardSummary(r, c);
    }
}
