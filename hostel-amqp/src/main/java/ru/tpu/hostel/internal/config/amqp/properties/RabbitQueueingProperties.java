package ru.tpu.hostel.internal.config.amqp.properties;

import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;

import java.util.Map;

@Validated
public record RabbitQueueingProperties(

        @Valid
        Map<String, RabbitListenerProperties> listeners,

        @Valid
        Map<String, RabbitSenderProperties> senders

) {
}
