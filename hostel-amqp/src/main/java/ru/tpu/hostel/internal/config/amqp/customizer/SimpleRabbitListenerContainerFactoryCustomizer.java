package ru.tpu.hostel.internal.config.amqp.customizer;

import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;

/**
 * Функциональный интерфейс для кастомизации {@link SimpleRabbitListenerContainerFactoryCustomizer}.
 */
@FunctionalInterface
public interface SimpleRabbitListenerContainerFactoryCustomizer
        extends Customizer<SimpleRabbitListenerContainerFactory> {
}
