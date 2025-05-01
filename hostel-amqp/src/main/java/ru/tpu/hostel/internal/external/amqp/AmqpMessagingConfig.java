package ru.tpu.hostel.internal.external.amqp;

import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

public interface AmqpMessagingConfig {

    RabbitTemplate rabbitTemplate();

    MessageProperties messageProperties();

    boolean isApplicable(AmqpMessageType amqpMessageType);

}
