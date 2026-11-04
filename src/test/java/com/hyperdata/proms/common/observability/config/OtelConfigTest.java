package com.hyperdata.proms.common.observability.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.exporter.otlp.http.trace.OtlpHttpSpanExporter;
import io.opentelemetry.sdk.resources.Resource;
import java.time.Duration;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.micrometer.tracing.opentelemetry.autoconfigure.otlp.OtlpHttpSpanExporterBuilderCustomizer;

class OtelConfigTest {

    private final OtelConfig otelConfig = new OtelConfig();

    @Test
    @DisplayName("Should create Resource bean with configured attributes")
    void shouldCreateResourceBeanWithAttributes() {
        Resource resource = otelConfig.otelResource("test-service", "1.2.3", "staging");

        assertThat(resource).isNotNull();
        assertThat(resource.getAttribute(AttributeKey.stringKey("service.name"))).isEqualTo("test-service");
        assertThat(resource.getAttribute(AttributeKey.stringKey("service.version"))).isEqualTo("1.2.3");
        assertThat(resource.getAttribute(AttributeKey.stringKey("deployment.environment"))).isEqualTo("staging");
    }

    @Test
    @DisplayName("Should create OtlpHttpSpanExporter with normalized endpoint")
    @SuppressWarnings("unchecked")
    void shouldCreateOtlpHttpSpanExporter() {
        ObjectProvider<OtlpHttpSpanExporterBuilderCustomizer> customizers = mock(ObjectProvider.class);
        when(customizers.orderedStream()).thenReturn(Stream.empty());

        OtlpHttpSpanExporter exporter = otelConfig.otlpHttpSpanExporter(
            "http://localhost:4318",
            Duration.ofSeconds(2),
            customizers);

        assertThat(exporter).isNotNull();
    }
}
