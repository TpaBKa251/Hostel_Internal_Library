package ru.tpu.hostel.internal.config.otlp;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.context.Context;
import io.opentelemetry.exporter.otlp.trace.OtlpGrpcSpanExporter;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.data.LinkData;
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import io.opentelemetry.sdk.trace.samplers.Sampler;
import io.opentelemetry.sdk.trace.samplers.SamplingResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

/**
 * Конфигурация для настройки трассировки через Open Telemetry и ее экспорта
 *
 * @author Илья Лапшин
 * @version 1.0.3
 * @see OpenTelemetryProperties
 * @since 1.0.0
 */
@Slf4j
@Configuration
@EnableConfigurationProperties({OpenTelemetryProperties.class})
@RequiredArgsConstructor
public class OpenTelemetryConfig {

    private static final String OTLP_SPAN_EXPORTER = "customOtlpSpanExporter";

    private static final String SDK_TRACER_PROVIDER = "customSdkTracerProvider";

    private static final String SDK_SAMPLER = "customSdkSampler";

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

    @Bean(SDK_SAMPLER)
    @Primary
    public Sampler sdkSampler() {
        return new Sampler() {

            private final List<Pattern> excludePatterns = List.of(
                    Pattern.compile("^/actuator.*"),
                    Pattern.compile("^/health.*"),
                    Pattern.compile("^/metrics.*")
            );

            @Override
            public SamplingResult shouldSample(
                    Context parentContext,
                    String traceId,
                    String name,
                    SpanKind spanKind,
                    Attributes attributes,
                    List<LinkData> parentLinks) {
                log.info("Sampler кастомный");

                // Проверяем различные возможные атрибуты с путями
                String httpTarget = attributes.get(AttributeKey.stringKey("http.target"));
                String httpRoute = attributes.get(AttributeKey.stringKey("http.route"));
                String urlPath = attributes.get(AttributeKey.stringKey("url.path"));
                String httpUrl = attributes.get(AttributeKey.stringKey("http.url"));

                log.info("{}, {}, {}, {}", httpTarget, httpRoute, urlPath, httpUrl);

                if (shouldExclude(httpTarget) || shouldExclude(httpRoute) || shouldExclude(urlPath) || shouldExclude(httpUrl)) {
                    log.info("не трассируем в сэмплере");
                    return SamplingResult.drop();
                }

                return Sampler.alwaysOn().shouldSample(parentContext, traceId, name, spanKind, attributes, parentLinks);
            }

            private boolean shouldExclude(String path) {
                if (path == null) return false;

                return excludePatterns.stream()
                        .anyMatch(pattern -> pattern.matcher(path).matches());
            }

            @Override
            public String getDescription() {
                return "ActuatorExcludingSampler";
            }
        };
    }

    @Bean(SDK_TRACER_PROVIDER)
    @Primary
    public SdkTracerProvider sdkTracerProvider(
            @Qualifier(OTLP_SPAN_EXPORTER) SpanExporter otlpSpanExporter,
            @Qualifier(SDK_SAMPLER) Sampler sampler
    ) {
        return SdkTracerProvider.builder()
                .setSampler(sampler)
                .addSpanProcessor(BatchSpanProcessor.builder(otlpSpanExporter).build())
                .setResource(Resource.getDefault().toBuilder()
                        .put("service.name", properties.serviceName())
                        .build())
                .build();
    }

}