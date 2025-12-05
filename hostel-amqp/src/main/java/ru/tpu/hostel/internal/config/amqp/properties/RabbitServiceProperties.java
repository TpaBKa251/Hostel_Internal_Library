package ru.tpu.hostel.internal.config.amqp.properties;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import org.springframework.validation.annotation.Validated;

import java.util.List;

@Validated
public record RabbitServiceProperties(

        @NotNull
        @Valid
        RabbitConnectionProperties connectionProperties,

        @NotNull
        @Valid
        RabbitQueueingProperties queueingProperties,

        String messageConverterName

) {
}
