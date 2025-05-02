package ru.tpu.hostel.internal;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Import;
import ru.tpu.hostel.internal.config.amqp.tracing.RabbitListenerTracingBeanPostProcessor;
import ru.tpu.hostel.internal.external.amqp.impl.DefaultAmqpMessageSender;

@AutoConfiguration
@Import({
        //AmqpMessageSenderLoggingFilter.class,
        DefaultAmqpMessageSender.class,
        RabbitListenerTracingBeanPostProcessor.class
})
public class HostelAmqpAutoConfiguration {
}
