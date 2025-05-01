package ru.tpu.hostel.internal.external.amqp.impl;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.core.MessagePropertiesBuilder;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import ru.tpu.hostel.internal.exception.ServiceException;
import ru.tpu.hostel.internal.external.amqp.AmqpMessageSender;
import ru.tpu.hostel.internal.external.amqp.AmqpMessageType;
import ru.tpu.hostel.internal.external.amqp.AmqpMessagingConfig;
import ru.tpu.hostel.internal.utils.ExecutionContext;
import ru.tpu.hostel.internal.utils.TimeUtil;

import java.io.IOException;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.Set;
import java.util.UUID;

@Service
@Primary
@RequiredArgsConstructor
public class DefaultAmqpMessageSender implements AmqpMessageSender {

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .disable(SerializationFeature.WRITE_DATE_TIMESTAMPS_AS_NANOSECONDS)
            .disable(DeserializationFeature.READ_DATE_TIMESTAMPS_AS_NANOSECONDS)
            .setTimeZone(TimeUtil.getTimeZone())
            .setSerializationInclusion(JsonInclude.Include.NON_NULL);

    private final Set<AmqpMessagingConfig> amqpMessagingConfigs;

    @Override
    public <T extends Enum<?> & AmqpMessageType> void send(T messageType, String messageId, Object messagePayload)
            throws IOException {
        AmqpMessagingConfig amqpMessagingConfig = getAmqpMessagingConfig(messageType);
        RabbitTemplate rabbitTemplate = amqpMessagingConfig.rabbitTemplate();
        MessageProperties messageProperties = getMessageProperties(messageId, amqpMessagingConfig.messageProperties());

        Message message = new Message(MAPPER.writeValueAsBytes(messagePayload), messageProperties);
        rabbitTemplate.send(message);
    }

    @Override
    public <T extends Enum<?> & AmqpMessageType, R> R sendAndReceive(
            T messageType,
            String messageId,
            Object messagePayload,
            Class<R> responseType
    ) throws IOException {
        AmqpMessagingConfig amqpMessagingConfig = getAmqpMessagingConfig(messageType);
        RabbitTemplate rabbitTemplate = amqpMessagingConfig.rabbitTemplate();
        MessageProperties messageProperties = getMessageProperties(messageId, amqpMessagingConfig.messageProperties());

        Message message = new Message(MAPPER.writeValueAsBytes(messagePayload), messageProperties);
        Message response = rabbitTemplate.sendAndReceive(message);

        if (response == null || response.getBody() == null || response.getBody().length == 0) {
            throw new IOException("Ответ пустой");
        }

        return MAPPER.readValue(response.getBody(), responseType);
    }

    private AmqpMessagingConfig getAmqpMessagingConfig(AmqpMessageType amqpMessageType) {
        return amqpMessagingConfigs.stream()
                .filter(config -> config.isApplicable(amqpMessageType))
                .findFirst()
                .orElseThrow(() ->
                        new ServiceException.NotImplemented("Не найден AMQP конфиг для типа: " + amqpMessageType)
                );
    }

    private MessageProperties getMessageProperties(String messageId, MessageProperties messageProperties) {
        ZonedDateTime now = TimeUtil.getZonedDateTime();
        long nowMillis = now.toInstant().toEpochMilli();
        ExecutionContext context = ExecutionContext.get();

        return MessagePropertiesBuilder.newInstance()
                .copyProperties(messageProperties)
                .setMessageId(messageId)
                .setCorrelationId(UUID.randomUUID().toString())
                .setTimestamp(new Date(nowMillis))
                .setHeader("X-User-Id", context.getUserID())
                .setHeader("X-User-Roles", context.getUserRoles())
                .build();
    }

}
