package com.hyperdata.proms.common.observability.aspect;

import io.micrometer.tracing.ScopedSpan;
import io.micrometer.tracing.Tracer;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class AutoTraceAspect {

    private final Tracer tracer;

    public AutoTraceAspect(@Nullable
    Tracer tracer) {
        this.tracer = tracer;
    }

    @Pointcut("execution(public * com.hyperdata.proms.module..service..*(..)) || " +
        "execution(public * com.hyperdata.proms.module..client.impl..*(..))")
    public void businessMethods() {
    }

    @Around("businessMethods()")
    public Object traceBusinessMethod(ProceedingJoinPoint pjp) throws Throwable {
        if (tracer == null) {
            return pjp.proceed();
        }
        Class<?> declaringType = pjp.getSignature().getDeclaringType();
        String methodName = pjp.getSignature().getName();
        String spanName = declaringType.getSimpleName() + "#" + methodName;

        ScopedSpan span = tracer.startScopedSpan(spanName);
        try {
            span.tag("code.namespace", declaringType.getName());
            span.tag("code.function", methodName);
            return pjp.proceed();
        } catch (Throwable ex) {
            span.error(ex);
            throw ex;
        } finally {
            span.end();
        }
    }
}
