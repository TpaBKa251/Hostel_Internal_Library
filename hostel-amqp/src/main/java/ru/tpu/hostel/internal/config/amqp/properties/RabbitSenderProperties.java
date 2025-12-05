package ru.tpu.hostel.internal.config.amqp.properties;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.validation.annotation.Validated;

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
