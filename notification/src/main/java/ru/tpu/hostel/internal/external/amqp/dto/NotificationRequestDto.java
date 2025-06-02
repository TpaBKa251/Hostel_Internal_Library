package ru.tpu.hostel.internal.external.amqp.dto;

import java.util.UUID;

/**
 * ДТО для отправки сообщения о создании уведомления в Notification Service
 *
 * @param userId  ID юзера, которому нужно отправить уведомление
 * @param type    тип уведомления
 * @param title   заголовок уведомления
 * @param message сообщение уведомления
 */
public record NotificationRequestDto(
        UUID userId,
        NotificationType type,
        String title,
        String message
) {
}
