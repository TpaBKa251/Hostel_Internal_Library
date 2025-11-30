package ru.tpu.hostel.internal.config.rest;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.internal.InstrumentationUtil;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import io.opentelemetry.context.propagation.TextMapGetter;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.Filter;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import ru.tpu.hostel.internal.exception.ServiceException;
import ru.tpu.hostel.internal.utils.ExecutionContext;
import ru.tpu.hostel.internal.utils.Roles;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static ru.tpu.hostel.internal.utils.ServiceHeaders.USER_ID_HEADER;
import static ru.tpu.hostel.internal.utils.ServiceHeaders.USER_ROLES_HEADER;

/**
 * Интерцептор для создания {@link ExecutionContext} на старте выполнения запроса и очистке контекста по завершении
 * обработки запроса
 *
 * @author Илья Лапшин
 * @version 1.2.0
 * @since 1.0.0
 */
@Configuration
@Slf4j
@RequiredArgsConstructor
public class HttpRestInterceptor {

    private static final String START_CONTROLLER_METHOD_EXECUTION = "[REQUEST] {} {}";

    private static final String FINISH_CONTROLLER_METHOD_EXECUTION = "[RESPONSE] Статус: {}. Время выполнения: {} мс";

    private static final String ACTUATOR = "/actuator";

    private static final String HEALTH = "/health";

    private static final String METRICS = "/metrics";

    private static final List<String> NOT_LOGGED_PATHS = List.of(ACTUATOR, HEALTH, METRICS);

    public static final TextMapGetter<HttpServletRequest> HTTP_REQUEST_MAP_GETTER = new TextMapGetter<>() {
        @Override
        public Iterable<String> keys(HttpServletRequest carrier) {
            return Collections.list(carrier.getHeaderNames());
        }

        @Override
        public String get(HttpServletRequest carrier, String key) {
            return carrier != null
                    ? carrier.getHeader(key)
                    : null;
        }
    };

    private final OpenTelemetry openTelemetry;

    @Order(Ordered.HIGHEST_PRECEDENCE)
    public Filter httpFilter() {
        return (request, response, chain) -> {
            HttpServletRequest req = (HttpServletRequest) request;
            HttpServletResponse res = (HttpServletResponse) response;

            if (NOT_LOGGED_PATHS.stream().anyMatch(p -> req.getRequestURI().startsWith(p))) {
                InstrumentationUtil.suppressInstrumentation(() -> {
                    try {
                        chain.doFilter(request, response);
                    } catch (IOException | ServletException e) {
                        throw new ServiceException("Неизвестная ошибка", HttpStatus.INTERNAL_SERVER_ERROR, e);
                    }
                });

                return;
            }

            String path = req.getRequestURI();
            String method = req.getMethod();

            Context parentContext = openTelemetry.getPropagators()
                    .getTextMapPropagator()
                    .extract(Context.current(), req, HTTP_REQUEST_MAP_GETTER);

            Span span = openTelemetry.getTracer("ru.tpu.hostel.internal.core")
                    .spanBuilder(method + " " + path)
                    .setParent(parentContext)
                    .setSpanKind(SpanKind.SERVER)
                    .setAttribute("http.method", method)
                    .setAttribute("http.route", path)
                    .setAttribute("http.target", req.getRequestURI())
                    .setAttribute("http.scheme", req.getScheme())
                    .setAttribute("http.host", req.getServerName())
                    .setAttribute("http.port", req.getServerPort())
                    .setAttribute("http.url", req.getRequestURL().toString())
                    .startSpan();

            try (Scope ignore = span.makeCurrent()) {
                SpanContext sc = span.getSpanContext();
                String traceId = sc.isValid() ? sc.getTraceId() : null;
                MDC.put("traceId", traceId);
                String spanId = sc.isValid() ? sc.getSpanId() : null;
                MDC.put("spanId", spanId);
                UUID userId = getUserId(req);
                Set<Roles> roles = getRoles(req);
                if (userId != null) {
                    MDC.put("userId", userId.toString());
                }
                if (roles != null && !roles.isEmpty()) {
                    MDC.put("roles", roles.stream().map(Roles::name).collect(Collectors.joining(",")));
                }

                ExecutionContext.create(userId, roles, traceId, spanId);
                boolean needToLog = needToLog(req.getRequestURI());
                logRequest(req, needToLog);
                long startTime = System.currentTimeMillis();
                chain.doFilter(request, response);
                long endTime = System.currentTimeMillis() - startTime;
                logResponse(res, endTime, needToLog);

                span.setAttribute("http.status_code", res.getStatus());

                if (res.getStatus() >= 100 && res.getStatus() < 400) {
                    span.setStatus(StatusCode.OK);
                } else {
                    span.setStatus(StatusCode.ERROR, "Status " + res.getStatus());
                }
            } catch (Exception ex) {
                span.recordException(ex);
                span.setStatus(StatusCode.ERROR, ex.getMessage());
                throw ex;
            } finally {
                ExecutionContext.clear();
                MDC.clear();
                span.end();
            }
        };
    }

    @Bean
    public FilterRegistrationBean<Filter> tracingSuppressionFilterRegistration() {
        FilterRegistrationBean<Filter> fr = new FilterRegistrationBean<>(httpFilter());
        fr.setOrder(Ordered.HIGHEST_PRECEDENCE);
        fr.addUrlPatterns("/*");
        fr.setDispatcherTypes(DispatcherType.REQUEST);
        fr.setName("tracingSuppression");
        fr.setAsyncSupported(true);
        return fr;
    }

    private UUID getUserId(HttpServletRequest request) {
        String userIdString = request.getHeader(USER_ID_HEADER);
        return userIdString == null || userIdString.isEmpty()
                ? null
                : UUID.fromString(userIdString);
    }

    private Set<Roles> getRoles(HttpServletRequest request) {
        String rolesString = request.getHeader(USER_ROLES_HEADER);
        return rolesString == null || rolesString.isEmpty()
                ? Collections.emptySet()
                : Arrays.stream(rolesString.split(","))
                .map(Roles::valueOf)
                .collect(Collectors.toUnmodifiableSet());
    }

    private void logRequest(HttpServletRequest request, boolean needToLog) {
        if (!needToLog) {
            return;
        }
        String method = request.getMethod();
        String requestURI = request.getRequestURI();

        log.info(START_CONTROLLER_METHOD_EXECUTION, method, requestURI);
    }

    private void logResponse(HttpServletResponse response, long duration, boolean needToLog) {
        if (!needToLog) {
            return;
        }
        int status = response.getStatus();
        log.info(FINISH_CONTROLLER_METHOD_EXECUTION, status, duration);
    }

    private boolean needToLog(String requestURI) {
        return requestURI != null && NOT_LOGGED_PATHS.stream().noneMatch(requestURI::startsWith);
    }

}
