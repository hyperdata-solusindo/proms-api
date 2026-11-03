package com.hyperdata.proms.common.observability.aspect;

import io.micrometer.tracing.ScopedSpan;
import io.micrometer.tracing.Tracer;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;

@Aspect
@Component
@RequiredArgsConstructor
public class AutoTraceAspect {

    private final Tracer tracer;

    @Pointcut("execution(public * com.hyperdata.proms.module..service..*(..)) || " +
        "execution(public * com.hyperdata.proms.module..facade.internal..*(..))")
    public void businessMethods() {
    }

    @Around("businessMethods()")
    public Object traceBusinessMethod(ProceedingJoinPoint pjp) throws Throwable {
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
