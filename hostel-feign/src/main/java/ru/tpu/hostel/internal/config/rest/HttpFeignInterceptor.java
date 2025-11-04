package ru.tpu.hostel.internal.config.rest;

import feign.RequestInterceptor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.tpu.hostel.internal.utils.ExecutionContext;

import static ru.tpu.hostel.internal.utils.ServiceHeaders.TRACEPARENT_HEADER;
import static ru.tpu.hostel.internal.utils.ServiceHeaders.TRACEPARENT_PATTERN;
import static ru.tpu.hostel.internal.utils.ServiceHeaders.USER_ID_HEADER;
import static ru.tpu.hostel.internal.utils.ServiceHeaders.USER_ROLES_HEADER;

/**
 * Интерцептор для добавления в заголовок REST запросов, которые отправляются через Feign клиент, информации о
 * трассировке
 *
 * @author Илья Лапшин
 * @version 1.1.2
 * @see ExecutionContext
 * @since 1.0.0
 */
@Slf4j
@Configuration
public class HttpFeignInterceptor {

    @Bean
    public RequestInterceptor tracingHttpRequestInterceptor() {
        return requestTemplate -> {
            ExecutionContext context = ExecutionContext.get();
            if (context == null) {
                return;
            }
            String traceparent = String.format(
                    TRACEPARENT_PATTERN,
                    context.getTraceId(),
                    context.getSpanId()
            );
            requestTemplate.header(TRACEPARENT_HEADER, traceparent);
            if (context.getUserID() != null) {
                requestTemplate.header(USER_ID_HEADER, context.getUserID().toString());
            }
            if (context.getUserRoles() != null && !context.getUserRoles().isEmpty()) {
                requestTemplate.header(USER_ROLES_HEADER, context.getUserRoles().toString().replace("[", "").replace("]", "").replaceAll(" ", ""));
            }
        };
    }

}
