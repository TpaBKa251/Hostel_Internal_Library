package ru.tpu.hostel.internal.config.amqp.customizer;

import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;

/**
 * Функциональный интерфейс для кастомизации {@link CachingConnectionFactory}.
 */
@FunctionalInterface
public interface TracedConnectionFactoryCustomizer extends Customizer<CachingConnectionFactory> {

}
