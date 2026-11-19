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

@Schema(description = "Community report. sourceRef is forced to \"community\".")
public record CommunityEventRequest(
        @NotNull EventType type,
        @NotBlank @Size(max = 120) String title,
        @Size(max = 1000) String description,
        @NotNull @DecimalMin("-90") @DecimalMax("90") Double latitude,
        @NotNull @DecimalMin("-180") @DecimalMax("180") Double longitude,
        @NotBlank @Size(max = 80) String neighborhood,
        @NotNull @Min(1) @Max(5) Integer severity,
        @Schema(description = "Defaults to now when omitted") Instant occurredAt,
        @Size(max = 254) @Schema(description = "Optional e-mail/phone; only a peppered SHA-256 is stored",
                accessMode = Schema.AccessMode.WRITE_ONLY) String contact) {

    public IngestEventCommand toCommand() {
        return new IngestEventCommand(type, title, description, latitude, longitude, neighborhood, severity,
                EventSource.COMMUNITY, null, occurredAt, contact);
    }

    @Override
    public String toString() {
        return "CommunityEventRequest[type=" + type + ", title=" + title + "]";
    }
}
