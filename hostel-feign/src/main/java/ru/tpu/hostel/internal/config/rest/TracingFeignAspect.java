package ru.tpu.hostel.internal.config.rest;

import feign.FeignException;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

@Aspect
@Component
@RequiredArgsConstructor
public class TracingFeignAspect {

    private final OpenTelemetry openTelemetry;

    @Around("within(@org.springframework.cloud.openfeign.FeignClient *)")
    public Object wrapInSpanFeignRequest(ProceedingJoinPoint joinPoint) throws Throwable {
        Span span = openTelemetry.getTracer("ru.tpu.hostel.internal.feign")
                .spanBuilder("")
                .setParent(Context.current())
                .setSpanKind(SpanKind.CLIENT)
                .startSpan();

        try (Scope ignore = span.makeCurrent()) {
            Object response = joinPoint.proceed();
            span.setAttribute("http.status_code", 200);
            span.setStatus(StatusCode.OK);
            return response;
        } catch (Throwable throwable) {
            if (throwable instanceof FeignException fe) {
                span.setAttribute("http.status_code", fe.status());
            }
            span.recordException(throwable);
            span.setStatus(StatusCode.ERROR, throwable.getMessage());
            throw throwable;
        } finally {
            span.end();
        }
    }

}
