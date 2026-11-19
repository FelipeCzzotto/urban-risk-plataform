package br.com.urbanrisk.domain;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

class ConfidenceCalculatorTest {

    @ParameterizedTest
    @CsvSource({"SENSOR,0.9", "PUBLIC_DATA,0.8", "COMMUNITY,0.5"})
    void baseConfidencePerSourceWithoutCorroboration(EventSource source, double expected) {
        assertThat(ConfidenceCalculator.calculate(source, 0)).isCloseTo(expected, within(1e-9));
    }

    @Test
    void addsTenthPerCorroboratingEvent() {
        assertThat(ConfidenceCalculator.calculate(EventSource.COMMUNITY, 3)).isCloseTo(0.8, within(1e-9));
    }

    @Test
    void capsAtOne() {
        assertThat(ConfidenceCalculator.calculate(EventSource.SENSOR, 1)).isEqualTo(1.0);
        assertThat(ConfidenceCalculator.calculate(EventSource.COMMUNITY, 50)).isEqualTo(1.0);
    }

    @Test
    void rejectsNegativeCorroboration() {
        assertThatThrownBy(() -> ConfidenceCalculator.calculate(EventSource.SENSOR, -1))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
