package ru.tpu.hostel.internal.config.amqp.properties;

import jakarta.validation.constraints.NotBlank;
import org.springframework.validation.annotation.Validated;

@Validated
public record RabbitListenerProperties(

        @NotBlank
        String queueName,

        String customizerName

) {
}
