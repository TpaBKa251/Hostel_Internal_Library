package ru.tpu.hostel.internal.config.rest;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.propagation.TextMapSetter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.tpu.hostel.internal.utils.ExecutionContext;

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
@RequiredArgsConstructor
public class HttpFeignInterceptor {

    private static final TextMapSetter<RequestTemplate> REQUEST_TEMPLATE_TEXT_MAP_SETTER = (carrier, key, value) -> {
        if (carrier != null) {
            carrier.header(key, value);
        }
    };

    private final OpenTelemetry openTelemetry;

    @Bean
    public RequestInterceptor tracingHttpRequestInterceptor() {
        return requestTemplate -> {
            Span span = Span.current();
            if (span != null) {
                span.updateName(requestTemplate.method() + " " + requestTemplate.path());
                span.setAttribute("http.method", requestTemplate.method());
                span.setAttribute("http.route", requestTemplate.path());
                span.setAttribute("http.target", requestTemplate.path());
                span.setAttribute("http.scheme", "http");
                span.setAttribute("http.host", requestTemplate.feignTarget().name());
                span.setAttribute("http.url", requestTemplate.url());
            }

            openTelemetry.getPropagators()
                    .getTextMapPropagator()
                    .inject(Context.current(), requestTemplate, REQUEST_TEMPLATE_TEXT_MAP_SETTER);
        };
    }

}
