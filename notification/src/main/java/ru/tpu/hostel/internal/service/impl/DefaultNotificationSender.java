package ru.tpu.hostel.internal.service.impl;

import io.micrometer.common.util.StringUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import ru.tpu.hostel.internal.builder.NotificationRequestBuilder;
import ru.tpu.hostel.internal.external.amqp.AmqpMessageSender;
import ru.tpu.hostel.internal.external.amqp.NotificationMessageType;
import ru.tpu.hostel.internal.external.amqp.dto.NotificationRequestDto;
import ru.tpu.hostel.internal.external.amqp.dto.NotificationType;
import ru.tpu.hostel.internal.service.NotificationSender;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Дефолтная реализация {@link NotificationSender}. Безопасно отправляет сообщения с ДТО через RabbitMQ
 * (не кидает исключения)
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Primary
public class DefaultNotificationSender implements NotificationSender {

    private final AmqpMessageSender amqpMessageSender;

    private final NotificationRequestBuilder notificationRequestBuilder;

    @Override
    public void sendNotification(@NotNull UUID userId, @NotNull NotificationType type) {
        NotificationRequestDto notificationRequestDto = notificationRequestBuilder.build(
                userId,
                type
        );
        sendNotificationIfNotNull(notificationRequestDto);
    }

    @Override
    public void sendNotification(@NotNull UUID userId, @NotNull NotificationType type, @NotNull String message) {
        NotificationRequestDto notificationRequestDto = notificationRequestBuilder.build(
                userId,
                type,
                message
        );
        sendNotificationIfNotNull(notificationRequestDto);
    }

    @Override
    public void sendNotification(
            @NotNull UUID userId,
            @NotNull NotificationType type,
            @NotNull String title,
            @NotNull String message
    ) {
        NotificationRequestDto notificationRequestDto = notificationRequestBuilder.build(
                userId,
                type,
                title,
                message
        );
        sendNotificationIfNotNull(notificationRequestDto);
    }

    @Override
    public void sendNotification(@NotNull NotificationRequestDto notification) {
        if (notification.userId() == null
                || notification.type() == null
                || StringUtils.isBlank(notification.title())
                || StringUtils.isBlank(notification.message())) {
            log.error("Один из параметров уведомления пустой");
            return;
        }
        sendNotificationViaAmqp(notification);
    }

    @Override
    public void sendNotification(@NotNull List<NotificationRequestDto> listOfNotificationRequestDto) {
        if (listOfNotificationRequestDto.isEmpty()) {
            log.warn("Список уведомлений пустой");
            return;
        }

        listOfNotificationRequestDto.stream()
                .filter(Objects::nonNull)
                .forEach(this::sendNotification);
    }

    @Override
    public void sendNotification(@NotNull NotificationRequestDto[] arrayOfNotificationRequestDto) {
        sendNotification(Arrays.asList(arrayOfNotificationRequestDto));
    }

    private void sendNotificationIfNotNull(@Nullable NotificationRequestDto notification) {
        if (notification == null) {
            log.error("Уведомление пустое");
            return;
        }
        sendNotificationViaAmqp(notification);
    }

    private void sendNotificationViaAmqp(@NotNull NotificationRequestDto notification) {
        try {
            amqpMessageSender.send(
                    NotificationMessageType.SEND_NOTIFICATION,
                    notification.userId().toString(),
                    notification
            );
        } catch (Exception e) {
            log.error("Ошибка отправки уведомления", e);
        }
    }

}
