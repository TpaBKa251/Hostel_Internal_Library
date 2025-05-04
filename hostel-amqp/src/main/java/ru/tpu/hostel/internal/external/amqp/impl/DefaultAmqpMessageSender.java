package ru.tpu.hostel.internal.external.amqp.impl;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.core.MessagePropertiesBuilder;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import ru.tpu.hostel.internal.config.amqp.AmqpMessagingConfig;
import ru.tpu.hostel.internal.exception.ServiceException;
import ru.tpu.hostel.internal.external.amqp.AmqpMessageSender;
import ru.tpu.hostel.internal.external.amqp.Microservice;
import ru.tpu.hostel.internal.utils.ExecutionContext;
import ru.tpu.hostel.internal.utils.TimeUtil;

import java.io.IOException;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.Set;
import java.util.UUID;

import static ru.tpu.hostel.internal.utils.ServiceHeaders.TRACEPARENT_HEADER;
import static ru.tpu.hostel.internal.utils.ServiceHeaders.TRACEPARENT_PATTERN;
import static ru.tpu.hostel.internal.utils.ServiceHeaders.USER_ID_HEADER;
import static ru.tpu.hostel.internal.utils.ServiceHeaders.USER_ROLES_HEADER;

@Service
@Primary
@RequiredArgsConstructor
public class DefaultAmqpMessageSender implements AmqpMessageSender {

    private static final String SENDING_MESSAGE_ERROR = "Ошибка отправки сообщения RabbitMQ";

    private static final String SERIALIZATION_OR_DESERIALIZATION_ERROR
            = "Ошибка сериализации/десериализации сообщения RabbitMQ";

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .disable(SerializationFeature.WRITE_DATE_TIMESTAMPS_AS_NANOSECONDS)
            .disable(DeserializationFeature.READ_DATE_TIMESTAMPS_AS_NANOSECONDS)
            .setTimeZone(TimeUtil.getTimeZone())
            .setSerializationInclusion(JsonInclude.Include.NON_NULL);

    private final Set<AmqpMessagingConfig> amqpMessagingConfigs;

    @Override
    public void send(Enum<?> messageType, String messageId, Object messagePayload) {
        try {
            AmqpMessagingConfig amqpMessagingConfig = getAmqpMessagingConfig(messageType);
            MessageProperties messageProperties = getMessageProperties(
                    messageId,
                    amqpMessagingConfig.messageProperties()
            );
            Message message = new Message(MAPPER.writeValueAsBytes(messagePayload), messageProperties);
            amqpMessagingConfig.rabbitTemplate().send(message);
        } catch (AmqpException e) {
            throw new ServiceException.ServiceUnavailable(SENDING_MESSAGE_ERROR, e);
        } catch (IOException e) {
            throw new ServiceException.InternalServerError(SERIALIZATION_OR_DESERIALIZATION_ERROR, e);
        }
    }

    @Override
    public <R> R sendAndReceive(Enum<?> messageType, String messageId, Object messagePayload, Class<R> responseType) {
        try {
            AmqpMessagingConfig amqpMessagingConfig = getAmqpMessagingConfig(messageType);
            MessageProperties messageProperties = getMessageProperties(
                    messageId,
                    amqpMessagingConfig.messageProperties()
            );
            Message message = new Message(MAPPER.writeValueAsBytes(messagePayload), messageProperties);
            Message response = amqpMessagingConfig.rabbitTemplate().sendAndReceive(message);

            if (response == null || response.getBody() == null || response.getBody().length == 0) {
                throw new ServiceException.ServiceUnavailable("Ответ пустой");
            }

            return MAPPER.readValue(response.getBody(), responseType);
        } catch (AmqpException e) {
            throw new ServiceException.ServiceUnavailable(SENDING_MESSAGE_ERROR, e);
        } catch (IOException e) {
            throw new ServiceException.InternalServerError(SERIALIZATION_OR_DESERIALIZATION_ERROR, e);
        }
    }

    public void send(Microservice microservice, String routingKey, String messageId, Object messagePayload) {
        try {
            AmqpMessagingConfig amqpMessagingConfig = getAmqpMessagingConfig(microservice);
            MessageProperties messageProperties = getMessageProperties(
                    messageId,
                    amqpMessagingConfig.messageProperties()
            );
            Message message = new Message(MAPPER.writeValueAsBytes(messagePayload), messageProperties);
            amqpMessagingConfig.rabbitTemplate().send(routingKey, message);
        } catch (AmqpException e) {
            throw new ServiceException.ServiceUnavailable(SENDING_MESSAGE_ERROR, e);
        } catch (IOException e) {
            throw new ServiceException.InternalServerError(SERIALIZATION_OR_DESERIALIZATION_ERROR, e);
        }
    }

    public void send(
            Microservice microservice,
            String exchange,
            String routingKey,
            String messageId,
            Object messagePayload
    ) {
        try {
            AmqpMessagingConfig amqpMessagingConfig = getAmqpMessagingConfig(microservice);
            MessageProperties messageProperties = getMessageProperties(
                    messageId,
                    amqpMessagingConfig.messageProperties()
            );
            Message message = new Message(MAPPER.writeValueAsBytes(messagePayload), messageProperties);
            amqpMessagingConfig.rabbitTemplate().send(exchange, routingKey, message);
        } catch (AmqpException e) {
            throw new ServiceException.ServiceUnavailable(SENDING_MESSAGE_ERROR, e);
        } catch (IOException e) {
            throw new ServiceException.InternalServerError(SERIALIZATION_OR_DESERIALIZATION_ERROR, e);
        }
    }

    public <R> R sendAndReceive(
            Microservice microservice,
            String routingKey,
            String messageId,
            Object messagePayload,
            Class<R> responseType
    ) {
        try {
            AmqpMessagingConfig amqpMessagingConfig = getAmqpMessagingConfig(microservice);
            MessageProperties messageProperties = getMessageProperties(
                    messageId,
                    amqpMessagingConfig.messageProperties()
            );
            Message message = new Message(MAPPER.writeValueAsBytes(messagePayload), messageProperties);
            Message response = amqpMessagingConfig.rabbitTemplate().sendAndReceive(routingKey, message);

            if (response == null || response.getBody() == null || response.getBody().length == 0) {
                throw new ServiceException.ServiceUnavailable("Ответ пустой");
            }

            return MAPPER.readValue(response.getBody(), responseType);
        } catch (AmqpException e) {
            throw new ServiceException.ServiceUnavailable(SENDING_MESSAGE_ERROR, e);
        } catch (IOException e) {
            throw new ServiceException.InternalServerError(SERIALIZATION_OR_DESERIALIZATION_ERROR, e);
        }
    }

    public <R> R sendAndReceive(
            Microservice microservice,
            String exchange,
            String routingKey,
            String messageId,
            Object messagePayload,
            Class<R> responseType
    ) {
        try {
            AmqpMessagingConfig amqpMessagingConfig = getAmqpMessagingConfig(microservice);
            MessageProperties messageProperties = getMessageProperties(
                    messageId,
                    amqpMessagingConfig.messageProperties()
            );
            Message message = new Message(MAPPER.writeValueAsBytes(messagePayload), messageProperties);
            Message response = amqpMessagingConfig.rabbitTemplate().sendAndReceive(exchange, routingKey, message);

            if (response == null || response.getBody() == null || response.getBody().length == 0) {
                throw new ServiceException.ServiceUnavailable("Ответ пустой");
            }

            return MAPPER.readValue(response.getBody(), responseType);
        } catch (AmqpException e) {
            throw new ServiceException.ServiceUnavailable(SENDING_MESSAGE_ERROR, e);
        } catch (IOException e) {
            throw new ServiceException.InternalServerError(SERIALIZATION_OR_DESERIALIZATION_ERROR, e);
        }
    }

    private AmqpMessagingConfig getAmqpMessagingConfig(Enum<?> amqpMessageType) {
        return amqpMessagingConfigs.stream()
                .filter(config -> config.isApplicable(amqpMessageType))
                .findFirst()
                .orElseThrow(() ->
                        new ServiceException.NotImplemented("Не найден AMQP конфиг для типа: " + amqpMessageType)
                );
    }

    private AmqpMessagingConfig getAmqpMessagingConfig(Microservice microservice) {
        return amqpMessagingConfigs.stream()
                .filter(config -> config.isApplicable(microservice))
                .findFirst()
                .orElseThrow(() ->
                        new ServiceException.NotImplemented("Не найден AMQP конфиг для сервиса: " + microservice)
                );
    }

    private MessageProperties getMessageProperties(String messageId, MessageProperties messageProperties) {
        ZonedDateTime now = TimeUtil.getZonedDateTime();
        long nowMillis = now.toInstant().toEpochMilli();
        ExecutionContext context = ExecutionContext.get();
        String traceparent = String.format(TRACEPARENT_PATTERN, context.getTraceId(), context.getSpanId());

        return MessagePropertiesBuilder.fromProperties(messageProperties)
                .setMessageId(messageId)
                .setCorrelationId(UUID.randomUUID().toString())
                .setTimestamp(new Date(nowMillis))
                .setHeader(USER_ID_HEADER, context.getUserID())
                .setHeader(USER_ROLES_HEADER, context.getUserRoles().toString().replace("[", "").replace("]", "").replaceAll(" ", ""))
                .setHeader(TRACEPARENT_HEADER, traceparent)
                .build();
    }

}
