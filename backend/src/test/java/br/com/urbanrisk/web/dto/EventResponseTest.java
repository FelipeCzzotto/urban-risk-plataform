package br.com.urbanrisk.web.dto;

import org.junit.jupiter.api.Test;

import java.lang.reflect.RecordComponent;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

class EventResponseTest {

    @Test
    void dtoNeverExposesPii() {
        assertThat(Arrays.stream(EventResponse.class.getRecordComponents()).map(RecordComponent::getName))
                .doesNotContain("reporterHash", "contact")
                .contains("id", "type", "title", "description", "latitude", "longitude", "neighborhood", "severity",
                        "source", "sourceRef", "confidence", "priorityScore", "priorityLevel", "status",
                        "occurredAt", "createdAt");
    }
}
