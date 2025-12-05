package ru.tpu.hostel.internal.config.amqp.customizer;

import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;

@FunctionalInterface
public interface TracedConnectionFactoryCustomizer extends Customizer<CachingConnectionFactory> {

}
