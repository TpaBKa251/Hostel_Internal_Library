package ru.tpu.hostel.internal.config.otlp;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.ContextKey;
import io.opentelemetry.context.propagation.ContextPropagators;
import io.opentelemetry.context.propagation.TextMapGetter;
import io.opentelemetry.context.propagation.TextMapPropagator;
import io.opentelemetry.context.propagation.TextMapSetter;
import io.opentelemetry.exporter.otlp.trace.OtlpGrpcSpanExporter;
import io.opentelemetry.instrumentation.jdbc.datasource.JdbcTelemetry;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.logs.SdkLoggerProvider;
import io.opentelemetry.sdk.metrics.SdkMeterProvider;
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
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.util.StringUtils;
import ru.tpu.hostel.internal.utils.Roles;

import javax.sql.DataSource;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static ru.tpu.hostel.internal.utils.ServiceHeaders.USER_ID;
import static ru.tpu.hostel.internal.utils.ServiceHeaders.USER_ID_HEADER;
import static ru.tpu.hostel.internal.utils.ServiceHeaders.USER_ROLES;
import static ru.tpu.hostel.internal.utils.ServiceHeaders.USER_ROLES_HEADER;

/**
 * Конфигурация для настройки трассировки через Open Telemetry и ее экспорта
 *
 * @author Илья Лапшин
 * @version 1.5.0
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

                boolean isRootSpan = !Span.fromContext(parentContext).getSpanContext().isValid();

                if (isRootSpan && shouldExcludeRootSpan(name, spanKind, attributes)) {
                    return SamplingResult.drop();
                }

                String httpTarget = attributes.get(AttributeKey.stringKey("http.target"));
                String httpRoute = attributes.get(AttributeKey.stringKey("http.route"));
                String urlPath = attributes.get(AttributeKey.stringKey("url.path"));
                String httpUrl = attributes.get(AttributeKey.stringKey("http.url"));

                if (shouldExclude(httpTarget)
                        || shouldExclude(httpRoute)
                        || shouldExclude(urlPath)
                        || shouldExclude(httpUrl)
                        || name.contains("actuator")
                        || name.contains("health")
                        || name.contains("metrics")) {
                    return SamplingResult.drop();
                }

                return Sampler.alwaysOn().shouldSample(parentContext, traceId, name, spanKind, attributes, parentLinks);
            }

            private boolean shouldExcludeRootSpan(String spanName, SpanKind spanKind, Attributes attributes) {
                String dbSystem = attributes.get(AttributeKey.stringKey("db.system"));
                String messagingSystem = attributes.get(AttributeKey.stringKey("messaging.system"));
                String dbOperation = attributes.get(AttributeKey.stringKey("db.operation"));
                String dbStatement = attributes.get(AttributeKey.stringKey("db.statement"));

                return
                        dbSystem != null ||
                                dbOperation != null ||
                                dbStatement != null ||
                                spanName.contains("jdbc") ||
                                spanName.contains("database") ||
                                spanName.contains("sql") ||
                                spanName.contains("query") ||

                                messagingSystem != null ||
                                spanName.contains("rabbitmq") ||
                                spanName.contains("amqp") ||
                                spanName.contains("messaging") ||

                                spanName.contains("migration") ||
                                spanName.contains("flyway") ||
                                spanName.contains("pool") ||
                                spanName.contains("connection") ||
                                spanName.contains("channel") ||
                                spanName.contains("init");
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

    @Bean
    public TextMapPropagator userContextPropagator() {
        return new TextMapPropagator() {

            @Override
            public Collection<String> fields() {
                return List.of(USER_ID_HEADER, USER_ROLES_HEADER);
            }

            @Override
            public <C> void inject(Context context, C carrier, TextMapSetter<C> setter) {
                UUID userId = context.get(ContextKey.named(USER_ID));
                Set<Roles> roles = context.get(ContextKey.named(USER_ROLES));

                if (userId != null) {
                    setter.set(carrier, USER_ID_HEADER, userId.toString());
                }
                if (roles != null && !roles.isEmpty()) {
                    String rolesStr = roles.stream().map(Enum::name).collect(Collectors.joining(","));
                    setter.set(carrier, USER_ROLES_HEADER, rolesStr);
                }
            }

            @Override
            public <C> Context extract(Context context, C carrier, TextMapGetter<C> getter) {
                String userIdStr = getter.get(carrier, USER_ID_HEADER);
                String rolesStr = getter.get(carrier, USER_ROLES_HEADER);

                Context newContext = context;
                if (StringUtils.hasText(userIdStr)) {
                    UUID userId = UUID.fromString(userIdStr);
                    newContext = newContext.with(ContextKey.named(USER_ID), userId);
                }
                if (StringUtils.hasText(rolesStr)) {
                    Set<Roles> roles = Arrays.stream(rolesStr.split(","))
                            .map(Roles::valueOf)
                            .collect(Collectors.toUnmodifiableSet());
                    newContext = newContext.with(ContextKey.named(USER_ROLES), roles);
                }
                return newContext;
            }
        };
    }

    @Bean
    @Primary
    public ContextPropagators customContextPropagators() {
        return ContextPropagators.create(
                TextMapPropagator.composite(
                        W3CTraceContextPropagator.getInstance(),
                        userContextPropagator()
                )
        );
    }

    @Bean
    @Primary
    public SdkMeterProvider customMeterProvider() {
        return SdkMeterProvider.builder().build();
    }

    @Bean
    @Primary
    public SdkLoggerProvider customLoggerProvider() {
        return SdkLoggerProvider.builder().build();
    }

    @Bean
    @Primary
    public OpenTelemetrySdk customOpenTelemetry(
            SdkTracerProvider tracerProvider,
            ContextPropagators propagators,
            SdkLoggerProvider loggerProvider,
            SdkMeterProvider meterProvider
    ) {
        return OpenTelemetrySdk.builder()
                .setTracerProvider(tracerProvider)
                .setPropagators(propagators)
                .setMeterProvider(meterProvider)
                .setLoggerProvider(loggerProvider)
                .build();
    }

    @Bean
    @Primary
    @ConditionalOnBean(DataSource.class)
    @Order(Ordered.HIGHEST_PRECEDENCE)
    public DataSource tracingDataSource(
            OpenTelemetry openTelemetry,
            DataSource dataSource
    ) {
        return JdbcTelemetry.builder(openTelemetry)
                .setCaptureQueryParameters(false)
                .setStatementSanitizationEnabled(true)
                .setDataSourceInstrumenterEnabled(true)
                .setStatementInstrumenterEnabled(true)
                .setTransactionInstrumenterEnabled(false)
                .build()
                .wrap(dataSource);
    }

}