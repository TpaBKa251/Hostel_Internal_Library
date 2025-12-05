package ru.tpu.hostel.internal.config.amqp.properties;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.convert.DurationUnit;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;
import java.time.temporal.ChronoUnit;

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
