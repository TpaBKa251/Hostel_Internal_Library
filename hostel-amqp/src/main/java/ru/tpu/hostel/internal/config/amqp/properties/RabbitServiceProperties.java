package ru.tpu.hostel.internal.config.amqp.properties;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.validation.annotation.Validated;

/**
 * Набор параметров для интеграции с микросервисом.
 *
 * @param connectionProperties свойства для подключения.
 * @param queueingProperties   свойства для очередей.
 * @param messageConverterName опциональное свойство для имени кастомного бина {@link MessageConverter}.
 */
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
