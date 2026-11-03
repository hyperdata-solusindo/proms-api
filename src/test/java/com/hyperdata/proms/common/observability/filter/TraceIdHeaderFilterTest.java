package com.hyperdata.proms.common.observability.filter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import io.micrometer.tracing.Span;
import io.micrometer.tracing.TraceContext;
import io.micrometer.tracing.Tracer;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import java.io.IOException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class TraceIdHeaderFilterTest {

    @Test
    @DisplayName("Should inject X-Trace-Id header into HTTP response when span is active")
    void shouldInjectTraceIdHeaderWhenSpanActive() throws ServletException, IOException {
        Tracer tracer = mock(Tracer.class);
        Span span = mock(Span.class);
        TraceContext context = mock(TraceContext.class);

        when(tracer.currentSpan()).thenReturn(span);
        when(span.context()).thenReturn(context);
        when(context.traceId()).thenReturn("4bf92f3577b34da6a3ce929d0e0e4736");

        TraceIdHeaderFilter filter = new TraceIdHeaderFilter(tracer);
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain filterChain = mock(FilterChain.class);

        filter.doFilter(request, response, filterChain);

        assertThat(response.getHeader(TraceIdHeaderFilter.TRACE_ID_HEADER))
            .isEqualTo("4bf92f3577b34da6a3ce929d0e0e4736");
        verify(filterChain).doFilter(request, response);
    }
}
