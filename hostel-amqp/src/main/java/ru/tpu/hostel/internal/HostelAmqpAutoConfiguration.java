package ru.tpu.hostel.internal;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Import;
import ru.tpu.hostel.internal.common.logging.AmqpMessageSenderLoggingFilter;
import ru.tpu.hostel.internal.config.amqp.RabbitConfiguration;
import ru.tpu.hostel.internal.config.amqp.util.RabbitListenerContainerFactoryNameResolver;
import ru.tpu.hostel.internal.external.amqp.impl.DefaultAmqpMessageSender;

@AutoConfiguration
@Import({
        DefaultAmqpMessageSender.class,
        AmqpMessageSenderLoggingFilter.class,
        RabbitConfiguration.class,
        RabbitListenerContainerFactoryNameResolver.class
})
public class HostelAmqpAutoConfiguration {
}
