package ru.tpu.hostel.internal.config.amqp;

import jakarta.validation.constraints.NotEmpty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "queueing.notification-service.send")
public record RabbitNotificationQueueingPropertiesSend(

        @NotEmpty
        String exchangeName,

        @NotEmpty
        String routingKey

) {
}
