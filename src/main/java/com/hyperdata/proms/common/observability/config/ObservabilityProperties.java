package com.hyperdata.proms.common.observability.config;

import java.util.Collections;
import java.util.Set;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "proms.observability.masking")
public record ObservabilityProperties(
    boolean enabled,
    Set<String> additionalKeys) {

    public ObservabilityProperties {
        if (additionalKeys == null) {
            additionalKeys = Collections.emptySet();
        }
    }
}
