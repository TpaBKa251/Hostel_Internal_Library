package ru.tpu.hostel.internal.config.amqp.tracing;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Tracer;
import lombok.RequiredArgsConstructor;
import org.aopalliance.aop.Advice;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Role;

import java.util.Arrays;
import java.util.Objects;

@Configuration
@RequiredArgsConstructor
@Role(BeanDefinition.ROLE_INFRASTRUCTURE)
public class RabbitListenerTracingBeanPostProcessor implements BeanPostProcessor {

    private final Tracer tracer;

    private final OpenTelemetry openTelemetry;

    @SuppressWarnings("NullableProblems")
    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) {
        if (bean instanceof SimpleRabbitListenerContainerFactory factory) {
            addInterceptorIfMissing(factory);
        }
        return bean;
    }

    private void addInterceptorIfMissing(SimpleRabbitListenerContainerFactory factory) {
        Advice[] existingAdvices = Objects.requireNonNullElse(factory.getAdviceChain(), new Advice[0]);

        boolean alreadyExists = Arrays.stream(existingAdvices)
                .anyMatch(advice -> advice instanceof AmqpMessageReceiveInterceptor);

        if (!alreadyExists) {
            Advice[] newAdvices = Arrays.copyOf(existingAdvices, existingAdvices.length + 1);
            newAdvices[existingAdvices.length] = new AmqpMessageReceiveInterceptor(tracer, openTelemetry);
            factory.setAdviceChain(newAdvices);
        }
    }
}