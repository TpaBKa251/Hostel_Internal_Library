package ru.tpu.hostel.internal.service;

import org.jetbrains.annotations.NotNull;
import ru.tpu.hostel.internal.builder.NotificationRequestBuilder;
import ru.tpu.hostel.internal.external.amqp.dto.NotificationRequestDto;
import ru.tpu.hostel.internal.external.amqp.dto.NotificationType;
import ru.tpu.hostel.internal.service.impl.DefaultNotificationSender;

import java.util.List;
import java.util.UUID;

/**
 * Интерфейс для отправки уведомлений. Имеет дефолтную безопасную реализацию {@link DefaultNotificationSender}
 *
 * @see NotificationRequestBuilder
 */
public interface NotificationSender {

    /**
     * Отправляет уведомление с минимальным количеством параметров
     *
     * @param userId ID юзера, которому нужно отправить уведомление
     * @param type   тип уведомления
     * @see NotificationRequestBuilder#build(UUID, NotificationType)
     */
    void sendNotification(@NotNull UUID userId, @NotNull NotificationType type);

    /**
     * Отправляет уведомление с телом уведомления (НЕ ЗАГОЛОВКОМ)
     *
     * @param userId  ID юзера, которому нужно отправить уведомление
     * @param type    тип уведомления
     * @param message тело уведомления
     * @see NotificationRequestBuilder#build(UUID, NotificationType, String)
     */
    void sendNotification(@NotNull UUID userId, @NotNull NotificationType type, @NotNull String message);

    /**
     * Отправляет уведомление с полными набором параметров
     *
     * @param userId  ID юзера, которому нужно отправить уведомление
     * @param type    тип уведомления
     * @param title   заголовок уведомления
     * @param message тело уведомления
     * @see NotificationRequestBuilder#build(UUID, NotificationType, String, String)
     */
    void sendNotification(
            @NotNull UUID userId,
            @NotNull NotificationType type,
            @NotNull String title,
            @NotNull String message
    );

    /**
     * Отправляет заранее созданное уведомление
     *
     * @param notificationRequestDto ДТО уведомления
     */
    void sendNotification(@NotNull NotificationRequestDto notificationRequestDto);

    /**
     * Отправляет список заранее созданных уведомлений
     *
     * @param listOfNotificationRequestDto список из ДТО уведомлений
     */
    void sendNotification(@NotNull List<NotificationRequestDto> listOfNotificationRequestDto);

    /**
     * Отправляет массив заранее созданных уведомлений
     *
     * @param arrayOfNotificationRequestDto массив из ДТО уведомлений
     */
    void sendNotification(@NotNull NotificationRequestDto[] arrayOfNotificationRequestDto);

}
