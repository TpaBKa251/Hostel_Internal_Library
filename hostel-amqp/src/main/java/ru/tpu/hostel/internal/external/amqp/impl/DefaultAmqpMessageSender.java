package ru.tpu.hostel.internal.external.amqp.impl;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.propagation.TextMapSetter;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.core.MessagePropertiesBuilder;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import ru.tpu.hostel.internal.config.amqp.AmqpMessagingConfig;
import ru.tpu.hostel.internal.exception.ServiceException;
import ru.tpu.hostel.internal.external.amqp.AmqpMessageSender;
import ru.tpu.hostel.internal.external.amqp.Microservice;
import ru.tpu.hostel.internal.utils.TimeUtil;

import java.io.IOException;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Date;
import java.util.Set;
import java.util.UUID;

/**
 * Дефолтная реализация интерфейса {@link AmqpMessageSender}. Можно использовать везде и всюду, необходимо лишь написать
 * бины конфигов для отправки {@link AmqpMessagingConfig}
 *
 * @author Илья Лапшин
 * @version 1.3.5
 * @since 1.0.7
 */
@Service
@Primary
@RequiredArgsConstructor
public class DefaultAmqpMessageSender implements AmqpMessageSender {

    private static final String SENDING_MESSAGE_ERROR = "Ошибка отправки сообщения RabbitMQ";

    private static final String SERIALIZATION_OR_DESERIALIZATION_ERROR
            = "Ошибка сериализации/десериализации сообщения RabbitMQ";

    private static final String EMPTY_RESPONSE_ERROR = "Ответ пустой";

    private static final String EMPTY_STRING_ERROR = "Пустая строка";

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .disable(SerializationFeature.WRITE_DATE_TIMESTAMPS_AS_NANOSECONDS)
            .disable(DeserializationFeature.READ_DATE_TIMESTAMPS_AS_NANOSECONDS)
            .setTimeZone(TimeUtil.getTimeZone())
            .setSerializationInclusion(JsonInclude.Include.NON_NULL);

    private static final TextMapSetter<MessageProperties> MESSAGE_PROPERTIES_TEXT_MAP_SETTER = (carrier, key, value) -> {
        if (carrier != null) {
            carrier.setHeader(key, value);
        }
    };

    private final Set<AmqpMessagingConfig> amqpMessagingConfigs;

    private final OpenTelemetry openTelemetry;

    @Override
    public void send(@NotNull Enum<?> messageType, @NotNull String messageId, @NotNull Object messagePayload) {
        checkString(messageId);
        try {
            AmqpMessagingConfig amqpMessagingConfig = getAmqpMessagingConfig(messageType);
            MessageProperties messageProperties = getMessageProperties(
                    messageId,
                    amqpMessagingConfig.defaultMessageProperties()
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
    public void send(
            @NotNull Enum<?> messageType,
            @NotNull String messageId,
            @NotNull Object messagePayload,
            MessageProperties messageProperties
    ) {
        checkString(messageId);
        try {
            AmqpMessagingConfig amqpMessagingConfig = getAmqpMessagingConfig(messageType);
            MessageProperties customMessageProperties = MessagePropertiesBuilder
                    .fromProperties(amqpMessagingConfig.defaultMessageProperties())
                    .copyProperties(messageProperties)
                    .build();

            messageProperties = getMessageProperties(
                    messageId,
                    customMessageProperties
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
    @NotNull
    public <R> R sendAndReceive(
            @NotNull Enum<?> messageType,
            @NotNull String messageId,
            @NotNull Object messagePayload,
            @NotNull Class<R> responseType
    ) {
        checkString(messageId);
        try {
            AmqpMessagingConfig amqpMessagingConfig = getAmqpMessagingConfig(messageType);
            MessageProperties messageProperties = getMessageProperties(
                    messageId,
                    amqpMessagingConfig.defaultMessageProperties()
            );
            Message message = new Message(MAPPER.writeValueAsBytes(messagePayload), messageProperties);
            Message response = amqpMessagingConfig.rabbitTemplate().sendAndReceive(message);

            if (response == null || response.getBody() == null || response.getBody().length == 0) {
                throw new ServiceException.ServiceUnavailable(EMPTY_RESPONSE_ERROR);
            }

            return MAPPER.readValue(response.getBody(), responseType);
        } catch (AmqpException e) {
            throw new ServiceException.ServiceUnavailable(SENDING_MESSAGE_ERROR, e);
        } catch (IOException e) {
            throw new ServiceException.InternalServerError(SERIALIZATION_OR_DESERIALIZATION_ERROR, e);
        }
    }

    @Override
    public <R> @NotNull R sendAndReceive(
            @NotNull Enum<?> messageType,
            @NotNull String messageId,
            @NotNull Object messagePayload,
            @NotNull Class<R> responseType,
            MessageProperties messageProperties
    ) {
        throw new ServiceException.NotImplemented();
    }

    @Override
    public void sendReply(
            @NotNull Enum<?> messageType,
            @NotNull MessageProperties properties,
            @NotNull Object messagePayload
    ) {
        try {
            AmqpMessagingConfig amqpMessagingConfig = getAmqpMessagingConfig(messageType);
            MessageProperties messageProperties = getReplyMessageProperties(properties);
            Message message = new Message(MAPPER.writeValueAsBytes(messagePayload), messageProperties);
            amqpMessagingConfig.rabbitTemplate().send("", messageProperties.getReplyTo(), message);
        } catch (AmqpException e) {
            throw new ServiceException.ServiceUnavailable(SENDING_MESSAGE_ERROR, e);
        } catch (IOException e) {
            throw new ServiceException.InternalServerError(SERIALIZATION_OR_DESERIALIZATION_ERROR, e);
        }
    }

    @Override
    public void send(
            @NotNull Microservice microservice,
            @NotNull String routingKey,
            @NotNull String messageId,
            @NotNull Object messagePayload
    ) {
        checkString(routingKey, messageId);
        try {
            AmqpMessagingConfig amqpMessagingConfig = getAmqpMessagingConfig(microservice);
            MessageProperties messageProperties = getMessageProperties(
                    messageId,
                    amqpMessagingConfig.defaultMessageProperties()
            );
            Message message = new Message(MAPPER.writeValueAsBytes(messagePayload), messageProperties);
            amqpMessagingConfig.rabbitTemplate().send(routingKey, message);
        } catch (AmqpException e) {
            throw new ServiceException.ServiceUnavailable(SENDING_MESSAGE_ERROR, e);
        } catch (IOException e) {
            throw new ServiceException.InternalServerError(SERIALIZATION_OR_DESERIALIZATION_ERROR, e);
        }
    }

    @Override
    public void send(
            @NotNull Microservice microservice,
            @NotNull String exchange,
            @NotNull String routingKey,
            @NotNull String messageId,
            @NotNull Object messagePayload
    ) {
        checkString(exchange, routingKey, messageId);
        try {
            AmqpMessagingConfig amqpMessagingConfig = getAmqpMessagingConfig(microservice);
            MessageProperties messageProperties = getMessageProperties(
                    messageId,
                    amqpMessagingConfig.defaultMessageProperties()
            );
            Message message = new Message(MAPPER.writeValueAsBytes(messagePayload), messageProperties);
            amqpMessagingConfig.rabbitTemplate().send(exchange, routingKey, message);
        } catch (AmqpException e) {
            throw new ServiceException.ServiceUnavailable(SENDING_MESSAGE_ERROR, e);
        } catch (IOException e) {
            throw new ServiceException.InternalServerError(SERIALIZATION_OR_DESERIALIZATION_ERROR, e);
        }
    }

    @Override
    @NotNull
    public <R> R sendAndReceive(
            @NotNull Microservice microservice,
            @NotNull String routingKey,
            @NotNull String messageId,
            @NotNull Object messagePayload,
            @NotNull Class<R> responseType
    ) {
        checkString(routingKey, messageId);
        try {
            AmqpMessagingConfig amqpMessagingConfig = getAmqpMessagingConfig(microservice);
            MessageProperties messageProperties = getMessageProperties(
                    messageId,
                    amqpMessagingConfig.defaultMessageProperties()
            );
            Message message = new Message(MAPPER.writeValueAsBytes(messagePayload), messageProperties);
            Message response = amqpMessagingConfig.rabbitTemplate().sendAndReceive(routingKey, message);

            if (response == null || response.getBody() == null || response.getBody().length == 0) {
                throw new ServiceException.ServiceUnavailable(EMPTY_RESPONSE_ERROR);
            }

            return MAPPER.readValue(response.getBody(), responseType);
        } catch (AmqpException e) {
            throw new ServiceException.ServiceUnavailable(SENDING_MESSAGE_ERROR, e);
        } catch (IOException e) {
            throw new ServiceException.InternalServerError(SERIALIZATION_OR_DESERIALIZATION_ERROR, e);
        }
    }

    @Override
    @NotNull
    public <R> R sendAndReceive(
            @NotNull Microservice microservice,
            @NotNull String exchange,
            @NotNull String routingKey,
            @NotNull String messageId,
            @NotNull Object messagePayload,
            @NotNull Class<R> responseType
    ) {
        checkString(exchange, routingKey, messageId);
        try {
            AmqpMessagingConfig amqpMessagingConfig = getAmqpMessagingConfig(microservice);
            MessageProperties messageProperties = getMessageProperties(
                    messageId,
                    amqpMessagingConfig.defaultMessageProperties()
            );
            Message message = new Message(MAPPER.writeValueAsBytes(messagePayload), messageProperties);
            Message response = amqpMessagingConfig.rabbitTemplate().sendAndReceive(exchange, routingKey, message);

            if (response == null || response.getBody() == null || response.getBody().length == 0) {
                throw new ServiceException.ServiceUnavailable(EMPTY_RESPONSE_ERROR);
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
        MessageProperties properties = MessagePropertiesBuilder.fromProperties(messageProperties)
                .setMessageId(messageId)
                .setCorrelationId(UUID.randomUUID().toString())
                .setTimestamp(new Date(nowMillis))
                .build();

        openTelemetry.getPropagators()
                .getTextMapPropagator()
                .inject(Context.current(), properties, MESSAGE_PROPERTIES_TEXT_MAP_SETTER);

        return properties;
    }

    private MessageProperties getReplyMessageProperties(MessageProperties messageProperties) {
        ZonedDateTime now = TimeUtil.getZonedDateTime();
        long nowMillis = now.toInstant().toEpochMilli();

        return MessagePropertiesBuilder.fromProperties(messageProperties)
                .setTimestamp(new Date(nowMillis))
                .build();
    }

    private void checkString(String... strings) {
        if (strings == null || Arrays.stream(strings).anyMatch(s -> !StringUtils.hasText(s))) {
            throw new ServiceException.InternalServerError(EMPTY_STRING_ERROR);
        }
    }

}
