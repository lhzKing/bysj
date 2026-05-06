package com.example.trace.config;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TraceDemoDataPropertiesTest {

    @Test
    void setMaxGenerateCount_shouldRejectValuesAboveHardCap() {
        TraceDemoDataProperties properties = new TraceDemoDataProperties();

        assertThatThrownBy(() -> properties.setMaxGenerateCount(TraceDemoDataProperties.HARD_MAX_GENERATE_COUNT + 1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("trace.demo-data.max-generate-count must be between 1 and 500");
    }
}
