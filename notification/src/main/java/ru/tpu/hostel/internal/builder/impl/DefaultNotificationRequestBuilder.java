package ru.tpu.hostel.internal.builder.impl;

import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import ru.tpu.hostel.internal.builder.NotificationRequestBuilder;
import ru.tpu.hostel.internal.external.amqp.dto.NotificationRequestDto;
import ru.tpu.hostel.internal.external.amqp.dto.NotificationType;

import java.util.UUID;

/**
 * Дефолтная реализация {@link NotificationRequestBuilder}. Безопасно билдит ДТО (не кидает исключения)
 */
@Slf4j
@Service
@Primary
public class DefaultNotificationRequestBuilder implements NotificationRequestBuilder {

    private static final String CREATE_NOTIFICATION_ERROR = "Не удалось создать уведомление";

    private static final String EMPTY_STRING_ERROR = "Текст или заголовок уведомления пустые";

    private static final String DEFAULT_NOTIFICATION_TITLE = "Уведомление о %s";

    private static final String DEFAULT_NOTIFICATION_MESSAGE = "Вам пришло уведомление о %s. Подробности можно узнать "
            + "в приложении в соответствующем разделе или у ответственного.";

    @Override
    @Nullable
    public NotificationRequestDto build(@NotNull UUID userId, @NotNull NotificationType type) {
        String title = DEFAULT_NOTIFICATION_TITLE.formatted(type.getNotificationName());
        String message = DEFAULT_NOTIFICATION_MESSAGE.formatted(type.getNotificationName());

        return new NotificationRequestDto(userId, type, title, message);
    }

    @Override
    @Nullable
    public NotificationRequestDto build(@NotNull UUID userId, @NotNull NotificationType type, @NotNull String message) {
        if (message.isBlank()) {
            return returnNull();
        }

        String title = DEFAULT_NOTIFICATION_TITLE.formatted(type.getNotificationName());
        return new NotificationRequestDto(userId, type, title, message);
    }

    @Override
    @Nullable
    public NotificationRequestDto build(
            @NotNull UUID userId,
            @NotNull NotificationType type,
            @NotNull String title,
            @NotNull String message
    ) {
        if (message.isBlank() || title.isBlank()) {
            return returnNull();
        }

        return new NotificationRequestDto(userId, type, title, message);
    }

    private NotificationRequestDto returnNull() {
        log.error(CREATE_NOTIFICATION_ERROR + ". " + EMPTY_STRING_ERROR);
        return null;
    }

}
