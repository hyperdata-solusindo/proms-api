package com.hyperdata.proms.common.observability.config;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.exporter.otlp.http.trace.OtlpHttpSpanExporter;
import io.opentelemetry.exporter.otlp.http.trace.OtlpHttpSpanExporterBuilder;
import io.opentelemetry.sdk.resources.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.micrometer.tracing.opentelemetry.autoconfigure.otlp.OtlpHttpSpanExporterBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
@ConditionalOnClass(OtlpHttpSpanExporter.class)
public class OtelConfig {

    private static final Logger log = LoggerFactory.getLogger(OtelConfig.class);

    @Bean
    @ConditionalOnMissingBean(Resource.class)
    public Resource otelResource(
        @Value("${management.opentelemetry.resource-attributes.service.name:${spring.application.name:api}}")
        String serviceName,
        @Value("${management.opentelemetry.resource-attributes.service.version:${APP_VERSION:0.0.1}}")
        String serviceVersion,
        @Value("${management.opentelemetry.resource-attributes.deployment.environment:${SPRING_PROFILES_ACTIVE:local}}")
        String env) {

        return Resource.getDefault()
            .merge(
                Resource.create(
                    Attributes.of(
                        AttributeKey.stringKey("service.name"), serviceName,
                        AttributeKey.stringKey("service.version"), serviceVersion,
                        AttributeKey.stringKey("deployment.environment"), env)));
    }

    @Bean
    @ConditionalOnMissingBean(OtlpHttpSpanExporter.class)
    @ConditionalOnProperty(name = "management.tracing.enabled", matchIfMissing = true)
    public OtlpHttpSpanExporter otlpHttpSpanExporter(
        @Value("${management.opentelemetry.tracing.export.otlp.endpoint:${management.otlp.tracing.endpoint:${OTEL_EXPORTER_OTLP_ENDPOINT:http://localhost:4318}}}")
        String endpoint,
        @Value("${management.opentelemetry.tracing.export.otlp.timeout:${management.otlp.tracing.timeout:${OTEL_TIMEOUT_SECONDS:2s}}}")
        Duration timeout,
        ObjectProvider<OtlpHttpSpanExporterBuilderCustomizer> customizers) {

        String normalized = (endpoint != null && !endpoint.isBlank()) ? endpoint.trim() : "http://localhost:4318";
        if (!normalized.endsWith("/v1/traces")) {
            normalized = normalized.replaceAll("/+$", "") + "/v1/traces";
        }

        log.info("[OpenTelemetry] Initializing OtlpHttpSpanExporter with endpoint: {}", normalized);

        OtlpHttpSpanExporterBuilder builder = OtlpHttpSpanExporter.builder()
            .setEndpoint(normalized)
            .setTimeout(timeout);

        customizers.orderedStream().forEach(customizer -> customizer.customize(builder));
        return builder.build();
    }
}
