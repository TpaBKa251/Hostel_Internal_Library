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
 * @version 1.0.4
 * @see ExecutionContext
 * @since 1.0.0
 */
@Slf4j
@Configuration
public class HttpFeignInterceptor {

    @Bean
    public RequestInterceptor tracingHttpRequestInterceptor() {
        log.info("Starting tracing request interceptor for Feign");
        return requestTemplate -> {
            ExecutionContext context = ExecutionContext.get();
            log.info("{}", context);
            String traceparent = String.format(
                    TRACEPARENT_PATTERN,
                    context.getTraceId(),
                    context.getSpanId()
            );
            requestTemplate.header(TRACEPARENT_HEADER, traceparent);
            requestTemplate.header(USER_ID_HEADER, context.getUserID().toString());
            requestTemplate.header(USER_ROLES_HEADER, context.getUserRoles().toString());
        };
    }

}
