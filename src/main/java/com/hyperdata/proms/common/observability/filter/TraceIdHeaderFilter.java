package com.hyperdata.proms.common.observability.filter;

import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class TraceIdHeaderFilter extends OncePerRequestFilter {

    public static final String TRACE_ID_HEADER = "X-Trace-Id";

    private final Tracer tracer;

    public TraceIdHeaderFilter(@Nullable
    Tracer tracer) {
        this.tracer = tracer;
    }

    @Override
    protected void doFilterInternal(
        HttpServletRequest request,
        HttpServletResponse response,
        FilterChain filterChain) throws ServletException, IOException {
        String traceId = null;
        if (tracer != null) {
            Span currentSpan = tracer.currentSpan();
            if (currentSpan != null && currentSpan.context() != null) {
                traceId = currentSpan.context().traceId();
                if (traceId != null && !traceId.isBlank()) {
                    response.setHeader(TRACE_ID_HEADER, traceId);
                }
            }
        }

        if (traceId != null && !traceId.isBlank()) {
            final String finalTraceId = traceId;
            io.sentry.Sentry.configureScope(scope -> {
                scope.setTag("trace_id", finalTraceId);
                scope.setTag("traceId", finalTraceId);
                scope.setTag("otel.trace_id", finalTraceId);
                try {
                    scope.getPropagationContext().setTraceId(new io.sentry.protocol.SentryId(finalTraceId));
                } catch (Exception ignored) {
                    // Fallback gracefully if traceId format differs
                }
            });
        }

        filterChain.doFilter(request, response);
    }
}
