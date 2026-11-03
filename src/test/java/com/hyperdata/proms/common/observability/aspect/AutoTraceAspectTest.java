package com.hyperdata.proms.common.observability.aspect;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import io.micrometer.tracing.ScopedSpan;
import io.micrometer.tracing.Tracer;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class AutoTraceAspectTest {

    @Test
    @DisplayName("Should create scoped span with method name and proceed joinpoint")
    void shouldCreateSpanAndProceed() throws Throwable {
        Tracer tracer = mock(Tracer.class);
        ScopedSpan span = mock(ScopedSpan.class);
        when(tracer.startScopedSpan(anyString())).thenReturn(span);

        ProceedingJoinPoint pjp = mock(ProceedingJoinPoint.class);
        Signature signature = mock(Signature.class);
        when(signature.getDeclaringType()).thenReturn((Class) String.class);
        when(signature.getName()).thenReturn("testMethod");
        when(pjp.getSignature()).thenReturn(signature);
        when(pjp.proceed()).thenReturn("result");

        AutoTraceAspect aspect = new AutoTraceAspect(tracer);
        Object result = aspect.traceBusinessMethod(pjp);

        assertThat(result).isEqualTo("result");
        verify(tracer).startScopedSpan("String#testMethod");
        verify(span).tag("code.namespace", "java.lang.String");
        verify(span).tag("code.function", "testMethod");
        verify(span).end();
    }
}
