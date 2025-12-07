package ru.tpu.hostel.internal.config.amqp.properties;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.convert.DurationUnit;
import org.springframework.validation.annotation.Validated;
import ru.tpu.hostel.internal.config.amqp.customizer.TracedConnectionFactoryCustomizer;

import java.time.Duration;
import java.time.temporal.ChronoUnit;

/**
 * Свойства для подключения.
 *
 * @param username          имя юзера.
 * @param password          пароль.
 * @param virtualHost       виртуальный хост.
 * @param addresses         адрес.
 * @param connectionTimeout таймаут для коннекта.
 * @param customizerName    опциональное свойство для имени кастомного бина {@link TracedConnectionFactoryCustomizer}.
 */
@Validated
public record RabbitConnectionProperties(

        @NotBlank
        String username,

        @NotBlank
        String password,

        @NotBlank
        String virtualHost,

        @NotBlank
        String addresses,

        @NotNull
        @DurationUnit(ChronoUnit.MILLIS)
        Duration connectionTimeout,

        String customizerName

) {
}
