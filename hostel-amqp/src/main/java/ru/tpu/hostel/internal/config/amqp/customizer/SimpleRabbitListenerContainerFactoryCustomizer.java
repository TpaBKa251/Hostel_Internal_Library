package ru.tpu.hostel.internal.config.amqp.customizer;

import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;

@FunctionalInterface
public interface SimpleRabbitListenerContainerFactoryCustomizer
        extends Customizer<SimpleRabbitListenerContainerFactory> {
}
