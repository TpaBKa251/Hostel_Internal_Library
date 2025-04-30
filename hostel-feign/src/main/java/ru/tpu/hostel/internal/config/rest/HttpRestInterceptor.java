package ru.tpu.hostel.internal.config.rest;

import feign.RequestInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.tpu.hostel.internal.utils.ExecutionContext;

/**
 * Интерцептор для добавления в заголовок REST запросов, которые отправляются через Feign клиент, информации о
 * трассировке
 *
 * @author Илья Лапшин
 * @version 1.0.0
 * @see ExecutionContext
 * @since 1.0.0
 */
@Configuration
public class HttpRestInterceptor {

    private static final String USER_ID_HEADER = "X-User-Id";

    private static final String ROLES_HEADER = "X-User-Roles";

    private static final String TRACEPARENT_HEADER = "traceparent";

    private static final String TRACEPARENT_PATTERN = "00-%s-%s-01";

    @Bean
    public RequestInterceptor tracingHttpRequestInterceptor() {
        return requestTemplate -> {
            ExecutionContext context = ExecutionContext.get();
            String traceparent = String.format(
                    TRACEPARENT_PATTERN,
                    context.getTraceId(),
                    context.getSpanId()
            );
            requestTemplate.header(TRACEPARENT_HEADER, traceparent);
            requestTemplate.header(USER_ID_HEADER, context.getUserID().toString());
            requestTemplate.header(ROLES_HEADER, context.getUserRoles().toString());
        };
    }

}
