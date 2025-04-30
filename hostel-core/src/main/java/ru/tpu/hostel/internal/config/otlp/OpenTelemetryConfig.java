package ru.tpu.hostel.internal.config.otlp;

import io.opentelemetry.exporter.otlp.trace.OtlpGrpcSpanExporter;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.util.concurrent.TimeUnit;

/**
 * Конфигурация для настройки трассировки через Open Telemetry и ее экспорта
 *
 * @author Илья Лапшин
 * @version 1.0.3
 * @see OpenTelemetryProperties
 * @since 1.0.0
 */
@Configuration
@EnableConfigurationProperties({OpenTelemetryProperties.class})
@RequiredArgsConstructor
public class OpenTelemetryConfig {

    private static final String OTLP_SPAN_EXPORTER = "customOtlpSpanExporter";

    private static final String SDK_TRACER_PROVIDER = "customSdkTracerProvider";

    private final OpenTelemetryProperties properties;

    @Bean(OTLP_SPAN_EXPORTER)
    @Primary
    public SpanExporter otlpSpanExporter() {
        if (Boolean.TRUE.equals(properties.exportEnabled())) {
            return OtlpGrpcSpanExporter.builder()
                    .setEndpoint(properties.endpoint())
                    .setTimeout(properties.timeout().toMillis(), TimeUnit.MILLISECONDS)
                    .build();
        }
        return SpanExporter.composite();
    }

    @Bean(SDK_TRACER_PROVIDER)
    @Primary
    public SdkTracerProvider sdkTracerProvider(@Qualifier(OTLP_SPAN_EXPORTER) SpanExporter otlpSpanExporter) {
        return SdkTracerProvider.builder()
                .addSpanProcessor(BatchSpanProcessor.builder(otlpSpanExporter).build())
                .setResource(Resource.getDefault().toBuilder()
                        .put("service.name", properties.serviceName())
                        .build())
                .build();
    }

}