package ru.tpu.hostel.internal.config.amqp;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "queueing.notification-service.send")
@Data
public class RabbitNotificationQueueingPropertiesSend {

    @NotEmpty
    private String exchangeName = "notification-exchange";

    @NotEmpty
    private String routingKey = "notification-send";

}
