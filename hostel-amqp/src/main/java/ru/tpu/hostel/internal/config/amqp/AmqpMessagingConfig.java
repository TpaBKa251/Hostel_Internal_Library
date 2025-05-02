package ru.tpu.hostel.internal.config.amqp;

import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import ru.tpu.hostel.internal.external.amqp.Microservice;

import java.util.Set;

public interface AmqpMessagingConfig {

    RabbitTemplate rabbitTemplate();

    MessageProperties messageProperties();

    Set<Microservice> receivingMicroservices();

    boolean isApplicable(Enum<?> amqpMessageType);

    default boolean isApplicable(Microservice microservice) {
        return receivingMicroservices().contains(microservice);
    }

}
