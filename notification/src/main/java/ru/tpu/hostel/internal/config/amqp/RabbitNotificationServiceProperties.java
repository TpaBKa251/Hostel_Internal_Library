package ru.tpu.hostel.internal.config.amqp;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.convert.DurationUnit;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;
import java.time.temporal.ChronoUnit;

@Validated
@ConfigurationProperties(prefix = "rabbitmq.notification-service")
public record RabbitNotificationServiceProperties(

        @NotEmpty
        String username,

        @NotEmpty
        String password,

        @NotEmpty
        String virtualHost,

        @NotEmpty
        String addresses,

        @NotNull
        @DurationUnit(ChronoUnit.MILLIS)
        Duration connectionTimeout

) {
}
