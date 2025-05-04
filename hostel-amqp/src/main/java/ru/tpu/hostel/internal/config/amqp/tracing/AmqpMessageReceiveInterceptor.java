package ru.tpu.hostel.internal.config.amqp.tracing;

import com.rabbitmq.client.Channel;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import io.opentelemetry.context.propagation.TextMapGetter;
import lombok.RequiredArgsConstructor;
import org.aopalliance.aop.Advice;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.listener.AbstractMessageListenerContainer;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import ru.tpu.hostel.internal.utils.ExecutionContext;
import ru.tpu.hostel.internal.utils.Roles;

import java.util.Arrays;
import java.util.Collections;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static ru.tpu.hostel.internal.utils.ServiceHeaders.USER_ID_HEADER;
import static ru.tpu.hostel.internal.utils.ServiceHeaders.USER_ROLES_HEADER;

/**
 * Интерцептор для метода получения сообщений по RabbitMQ
 *
 * <p>💡Использовать в {@link SimpleRabbitListenerContainerFactory#setAdviceChain(Advice...)}:
 * <pre><code>
 *     factory.setAdviceChain(new AmqpMessageReceiveInterceptor(tracer, openTelemetry))
 * </code></pre>
 *
 * @author Илья Лапшин
 * @version 1.0.4
 * @since 1.0.3
 */
@RequiredArgsConstructor
public class AmqpMessageReceiveInterceptor implements MethodInterceptor {

    private final Tracer tracer;

    private final OpenTelemetry openTelemetry;

    private static final TextMapGetter<Message> GETTER = new TextMapGetter<>() {
        @Override
        public Iterable<String> keys(Message carrier) {
            return carrier != null
                    ? carrier.getMessageProperties().getHeaders().keySet()
                    : Collections.emptySet();
        }

        @Override
        public String get(Message carrier, String key) {
            if (carrier == null) return null;
            Object value = carrier.getMessageProperties().getHeaders().get(key);
            return value != null ? value.toString() : null;
        }
    };

    /**
     * Перехватывает метод получения/обработки сообщения, добавляет трассировку, создает {@link ExecutionContext}
     *
     * <p>Интерцептор прикрепляется к методу
     * {@link AbstractMessageListenerContainer#executeListener(Channel, Object)}
     *
     * @author Илья Лапшин
     * @since 1.0.3
     */
    @Override
    public Object invoke(MethodInvocation invocation) throws Throwable {
        Message message = Arrays.stream(invocation.getArguments())
                .filter(Message.class::isInstance)
                .map(Message.class::cast)
                .findFirst()
                .orElse(null);

        if (message == null) {
            return invocation.proceed();
        }

        Context context = openTelemetry.getPropagators()
                .getTextMapPropagator()
                .extract(Context.current(), message, GETTER);

        MessageProperties messageProperties = message.getMessageProperties();

        UUID userId = getUserId(messageProperties);
        Set<Roles> roles = getRoles(messageProperties);

        String queue = messageProperties.getConsumerQueue();
        Span span = tracer.spanBuilder("rabbit.receive")
                .setParent(context)
                .setAttribute("messaging.system", "rabbitmq")
                .setAttribute("messaging.destination", queue)
                .setAttribute("messaging.operation", "receive")
                .setAttribute(
                        "messaging.rabbitmq.routing_key",
                        messageProperties.getReceivedRoutingKey()
                )
                .startSpan();

        ExecutionContext.create(userId, roles, span.getSpanContext().getTraceId(), span.getSpanContext().getSpanId());

        try (Scope ignored = span.makeCurrent()) {
            Object result = invocation.proceed();
            span.setStatus(StatusCode.OK);
            return result;
        } catch (Exception e) {
            span.recordException(e);
            span.setStatus(StatusCode.ERROR, e.getMessage());
            throw e;
        } finally {
            ExecutionContext.clear();
            span.end();
        }
    }

    private UUID getUserId(MessageProperties properties) {
        String userIdString = properties.getHeader(USER_ID_HEADER);
        return userIdString == null || userIdString.isEmpty()
                ? null
                : UUID.fromString(userIdString);
    }

    private Set<Roles> getRoles(MessageProperties properties) {
        String rolesString = properties.getHeader(USER_ROLES_HEADER);
        return rolesString == null || rolesString.isEmpty()
                ? Collections.emptySet()
                : Arrays.stream(rolesString.split(","))
                .map(Roles::valueOf)
                .collect(Collectors.toUnmodifiableSet());
    }
}
