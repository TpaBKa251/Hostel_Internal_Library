package ru.tpu.hostel.internal;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Tracer;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Role;
import ru.tpu.hostel.internal.config.amqp.tracing.RabbitListenerTracingBeanPostProcessor;
import ru.tpu.hostel.internal.external.amqp.impl.DefaultAmqpMessageSender;

@AutoConfiguration
@Import({
        //AmqpMessageSenderLoggingFilter.class,
        DefaultAmqpMessageSender.class
})
public class HostelAmqpAutoConfiguration {

    @Bean
    @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
    public static BeanPostProcessor rabbitListenerTracingBeanPostProcessor(Tracer tracer, OpenTelemetry openTelemetry) {
        return new RabbitListenerTracingBeanPostProcessor(tracer, openTelemetry);
    }
}
