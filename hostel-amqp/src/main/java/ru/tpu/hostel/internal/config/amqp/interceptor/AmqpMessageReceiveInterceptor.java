package ru.tpu.hostel.internal.config.amqp.interceptor;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.rabbitmq.client.Channel;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import io.opentelemetry.context.propagation.TextMapGetter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aopalliance.aop.Advice;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.slf4j.MDC;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.listener.AbstractMessageListenerContainer;
import ru.tpu.hostel.internal.exception.ServiceException;
import ru.tpu.hostel.internal.utils.ExecutionContext;
import ru.tpu.hostel.internal.utils.Roles;

import java.util.Arrays;
import java.util.Collections;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static ru.tpu.hostel.internal.utils.ServiceHeaders.USER_ID_HEADER;
import static ru.tpu.hostel.internal.utils.ServiceHeaders.USER_ROLES_HEADER;
import static ru.tpu.hostel.internal.utils.TimeUtil.getLocalDateTimeStingFromMillis;

/**
 * Интерцептор для метода получения сообщений по RabbitMQ
 *
 * <p>💡Использовать в {@link SimpleRabbitListenerContainerFactory#setAdviceChain(Advice...)}:
 * <pre><code>
 *     factory.setAdviceChain(new AmqpMessageReceiveInterceptor(tracer, openTelemetry))
 * </code></pre>
 *
 * @author Илья Лапшин
 * @version 1.1.2
 * @since 1.0.3
 */
@RequiredArgsConstructor
@Slf4j
public class AmqpMessageReceiveInterceptor implements MethodInterceptor {

    private static final String START_RABBIT_LISTENER_METHOD_EXECUTION
            = "[RABBIT] Получено сообщение: messageId={}, payload={}";

    private static final String FINISH_RABBIT_LISTENER_METHOD_EXECUTION
            = "[RABBIT] Сообщение обработано: messageId={}. Время выполнения {} мс";

    private static final String RABBIT_LISTENER_EXCEPTION = "[RABBIT] Ошибка обработки сообщения: messageId={}. "
            + "Ошибка: {}, время старта: {}, время выполнения: {} мс";

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

    private static final ObjectWriter WRITER = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .disable(SerializationFeature.WRITE_DATE_TIMESTAMPS_AS_NANOSECONDS)
            .disable(DeserializationFeature.READ_DATE_TIMESTAMPS_AS_NANOSECONDS)
            .enable(SerializationFeature.INDENT_OUTPUT)
            .writer();

    private final OpenTelemetry openTelemetry;

    /**
     * Перехватывает метод получения/обработки сообщения, добавляет трассировку, создает {@link ExecutionContext},
     * логирует выполнение метода слушателя
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
        if (userId != null) {
            MDC.put("userId", userId.toString());
        }
        if (roles != null && !roles.isEmpty()) {
            MDC.put("roles", roles.stream().map(Roles::name).collect(Collectors.joining(",")));
        }

        String queue = messageProperties.getConsumerQueue();
        Span span = openTelemetry.getTracer("ru.tpu.hostel.internal.amqp").spanBuilder("rabbit.receive")
                .setParent(context)
                .setAttribute("messaging.system", "rabbitmq")
                .setAttribute("messaging.destination", queue)
                .setAttribute("messaging.operation", "receive")
                .setAttribute(
                        "messaging.rabbitmq.routing_key",
                        messageProperties.getReceivedRoutingKey()
                )
                .startSpan();

        long startTime = System.currentTimeMillis();
        try (Scope ignored = span.makeCurrent()) {
            MDC.put("traceId", span.getSpanContext().getTraceId());
            MDC.put("spanId", span.getSpanContext().getSpanId());
            ExecutionContext.create(
                    userId,
                    roles,
                    span.getSpanContext().getTraceId(),
                    span.getSpanContext().getSpanId()
            );
            log.info(
                    START_RABBIT_LISTENER_METHOD_EXECUTION,
                    messageProperties.getMessageId(),
                    safeMapToJson(message.getBody())
            );
            startTime = System.currentTimeMillis();
            Object result = invocation.proceed();
            long endTime = System.currentTimeMillis() - startTime;
            log.info(
                    FINISH_RABBIT_LISTENER_METHOD_EXECUTION,
                    messageProperties.getMessageId(),
                    endTime
            );
            span.setStatus(StatusCode.OK);
            return result;
        } catch (Exception e) {
            logException(e, messageProperties.getMessageId(), startTime);
            span.recordException(e);
            span.setStatus(StatusCode.ERROR, e.getMessage());
            throw e;
        } finally {
            ExecutionContext.clear();
            span.end();
            MDC.clear();
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

    private void logException(Exception e, String messageId, long startTime) {
        long endTime = System.currentTimeMillis() - startTime;
        if (e instanceof ServiceException serviceException) {
            log.error(
                    RABBIT_LISTENER_EXCEPTION,
                    messageId,
                    serviceException.getMessage(),
                    getLocalDateTimeStingFromMillis(startTime),
                    endTime
            );
        } else {
            log.error(
                    RABBIT_LISTENER_EXCEPTION,
                    messageId,
                    messageId,
                    getLocalDateTimeStingFromMillis(startTime),
                    endTime,
                    e
            );
        }
    }

    private String safeMapToJson(Object obj) {
        try {
            return WRITER.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            return "<ошибка сериализации>";
        }
    }
}
