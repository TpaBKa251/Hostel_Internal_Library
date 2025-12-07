package ru.tpu.hostel.internal.config.amqp.customizer;

import org.springframework.amqp.rabbit.core.RabbitTemplate;

/**
 * Функциональный интерфейс для кастомизации {@link RabbitTemplate}.
 */
@FunctionalInterface
public interface RabbitTemplateCustomizer extends Customizer<RabbitTemplate> {

}
