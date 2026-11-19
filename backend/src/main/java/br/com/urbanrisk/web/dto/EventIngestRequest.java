package br.com.urbanrisk.web.dto;

import br.com.urbanrisk.application.IngestEventCommand;
import br.com.urbanrisk.domain.EventSource;
import br.com.urbanrisk.domain.EventType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;

@Schema(description = "Sensor / public-data ingestion payload")
public record EventIngestRequest(
        @NotNull EventType type,
        @NotBlank @Size(max = 120) String title,
        @Size(max = 1000) String description,
        @NotNull @DecimalMin("-90") @DecimalMax("90") Double latitude,
        @NotNull @DecimalMin("-180") @DecimalMax("180") Double longitude,
        @NotBlank @Size(max = 80) String neighborhood,
        @NotNull @Min(1) @Max(5) Integer severity,
        @NotBlank @Size(max = 120) @Schema(example = "sensor-sp-001") String sourceRef,
        @Schema(description = "Defaults to now when omitted") Instant occurredAt) {

    public IngestEventCommand toCommand(EventSource source) {
        return new IngestEventCommand(type, title, description, latitude, longitude, neighborhood, severity, source,
                sourceRef, occurredAt, null);
    }
}
