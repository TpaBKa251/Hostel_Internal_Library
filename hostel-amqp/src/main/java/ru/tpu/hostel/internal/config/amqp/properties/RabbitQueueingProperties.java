package ru.tpu.hostel.internal.config.amqp.properties;

import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;

import java.util.Map;

/**
 * Свойства очередей.
 *
 * @param listeners набор параметров для слушателя.
 * @param senders   набор параметров для отправителей.
 */
@Validated
public record RabbitQueueingProperties(

        @Valid
        Map<String, RabbitListenerProperties> listeners,

        @Valid
        Map<String, RabbitSenderProperties> senders

) {
}
