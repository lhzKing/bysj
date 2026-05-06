package com.example.trace.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Guardrails for demo-data admin endpoints.
 */
@ConfigurationProperties(prefix = "trace.demo-data")
public class TraceDemoDataProperties {

    public static final int MIN_GENERATE_COUNT = 1;
    public static final int HARD_MAX_GENERATE_COUNT = 500;

    private boolean enabled = false;
    private int maxGenerateCount = HARD_MAX_GENERATE_COUNT;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int getMaxGenerateCount() {
        return maxGenerateCount;
    }

    public void setMaxGenerateCount(int maxGenerateCount) {
        if (maxGenerateCount < MIN_GENERATE_COUNT || maxGenerateCount > HARD_MAX_GENERATE_COUNT) {
            throw new IllegalArgumentException("trace.demo-data.max-generate-count must be between 1 and "
                    + HARD_MAX_GENERATE_COUNT);
        }
        this.maxGenerateCount = maxGenerateCount;
    }
}
