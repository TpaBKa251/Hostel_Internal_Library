package ru.tpu.hostel.internal.config.amqp.properties;

import jakarta.validation.constraints.NotBlank;
import org.springframework.validation.annotation.Validated;
import ru.tpu.hostel.internal.config.amqp.customizer.SimpleRabbitListenerContainerFactoryCustomizer;

/**
 * Свойства слушателя.
 *
 * @param exchangeName   имя обменника
 * @param queueName      имя очереди
 * @param routingKey     ключ маршрутизации
 * @param customizerName опциональное свойство для имени кастомного бина
 *                       {@link SimpleRabbitListenerContainerFactoryCustomizer}.
 */
@Validated
public record RabbitListenerProperties(

        @NotBlank
        String exchangeName,

        @NotBlank
        String queueName,

        @NotBlank
        String routingKey,

        String customizerName

) {
}
