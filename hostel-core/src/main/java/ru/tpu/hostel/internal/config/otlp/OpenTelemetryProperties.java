package ru.tpu.hostel.internal.config.otlp;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.convert.DurationUnit;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;
import java.time.temporal.ChronoUnit;

/**
 * Свойства для Open Telemetry трассировки. Пишутся в <b><i>application.yaml</i></b>.
 * <p>Пример:
 * <pre><code>
 *     otlp:
 *       tracing:
 *         export-enabled: true
 *         endpoint: http://tempo:4317
 *         timeout: 5000
 *         service-name: booking-service
 * </code></pre>
 *
 * @param exportEnabled включает или отключает экспорт трассировки
 * @param endpoint      эндроинт, куда экспортировать трассировку
 * @param timeout       таймаут для экспорта
 * @param serviceName   имя сервиса
 * @author Илья Лапшин
 * @version 1.0.0
 * @since 1.0.0
 */
@Validated
@ConfigurationProperties(prefix = "otlp.tracing")
public record OpenTelemetryProperties(

        @NotNull
        Boolean exportEnabled,

        @NotEmpty
        String endpoint,

        @DurationUnit(ChronoUnit.MILLIS)
        @NotNull
        Duration timeout,

        @NotEmpty
        String serviceName

) {
}
