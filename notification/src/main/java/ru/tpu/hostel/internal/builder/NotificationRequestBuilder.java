package ru.tpu.hostel.internal.builder;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ru.tpu.hostel.internal.builder.impl.DefaultNotificationRequestBuilder;
import ru.tpu.hostel.internal.external.amqp.dto.NotificationRequestDto;
import ru.tpu.hostel.internal.external.amqp.dto.NotificationType;

import java.util.UUID;

/**
 * Интерфейс билдера для ДТО отправки уведомления в Notification Service. Имеет стандартную реализацию для безопасного
 * создания ДТО {@link DefaultNotificationRequestBuilder}
 */
public interface NotificationRequestBuilder {

    /**
     * Метод создания ДТО с минимальными входными параметрами
     *
     * @param userId ID юзера, которому нужно отправить уведомление
     * @param type   тип уведомления
     * @return созданное ДТО для отправки
     */
    @Nullable
    NotificationRequestDto build(@NotNull UUID userId, @NotNull NotificationType type);

    /**
     * Метод для создания ДТО с телом уведомления (НЕ ЗАГОЛОВКОМ)
     *
     * @param userId  ID юзера, которому нужно отправить уведомление
     * @param type    тип уведомления
     * @param message тело уведомления
     * @return созданное ДТО для отправки
     */
    @Nullable
    NotificationRequestDto build(@NotNull UUID userId, @NotNull NotificationType type, @NotNull String message);

    /**
     * Метод для создания ДТО с полными набором параметров
     *
     * @param userId  ID юзера, которому нужно отправить уведомление
     * @param type    тип уведомления
     * @param title   заголовок уведомления
     * @param message тело уведомления
     * @return созданное ДТО для отправки
     */
    @Nullable
    NotificationRequestDto build(
            @NotNull UUID userId,
            @NotNull NotificationType type,
            @NotNull String title,
            @NotNull String message
    );
}
