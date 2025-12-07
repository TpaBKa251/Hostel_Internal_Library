package ru.tpu.hostel.internal.config.amqp.properties;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.validation.annotation.Validated;
import ru.tpu.hostel.internal.config.amqp.customizer.RabbitTemplateCustomizer;


/**
 * Свойства отправителей.
 *
 * @param exchangeName                 имя обменника
 * @param queueName                    имя очереди
 * @param routingKey                   ключ маршрутизации
 * @param channelTransacted            задает транзакционность канала отправки сообщения.
 * @param rabbitTemplateCustomizerName опциональное свойство имени кастомного бина {@link RabbitTemplateCustomizer}
 * @param messagePropertiesBeanName    опциональное свойство имени кастомного бина {@link MessageProperties}
 */
@Validated
public record RabbitSenderProperties(

        @NotBlank
        String exchangeName,

        @NotBlank
        String queueName,

        @NotBlank
        String routingKey,

        @NotNull
        Boolean channelTransacted,

        String rabbitTemplateCustomizerName,

        String messagePropertiesBeanName

) {
}
