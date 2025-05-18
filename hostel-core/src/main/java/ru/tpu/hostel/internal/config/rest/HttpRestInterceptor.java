package ru.tpu.hostel.internal.config.rest;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.filter.OncePerRequestFilter;
import ru.tpu.hostel.internal.utils.ExecutionContext;
import ru.tpu.hostel.internal.utils.Roles;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
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
 * @version 1.1.2
 * @since 1.0.0
 */
@SuppressWarnings("NullableProblems")
@Configuration
public class HttpRestInterceptor {

    @Bean
    public OncePerRequestFilter executionContextFilter() {
        return new OncePerRequestFilter() {
            @Override
            protected void doFilterInternal(
                    HttpServletRequest request,
                    HttpServletResponse response,
                    FilterChain filterChain
            ) throws ServletException, IOException {
                Span span = Span.current();
                SpanContext sc = span.getSpanContext();
                String traceId = sc.isValid() ? sc.getTraceId() : null;
                String spanId = sc.isValid() ? sc.getSpanId() : null;

                UUID userId = getUserId(request);
                Set<Roles> roles = getRoles(request);
                if (userId != null) {
                    MDC.put("userId", userId.toString());
                }
                if (roles != null && !roles.isEmpty()) {
                    MDC.put("roles", roles.stream().map(Roles::name).collect(Collectors.joining(", ")));
                }

                ExecutionContext.create(userId, roles, traceId, spanId);
                try {
                    filterChain.doFilter(request, response);
                } finally {
                    ExecutionContext.clear();
                    MDC.clear();
                }
            }
        };
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
}
