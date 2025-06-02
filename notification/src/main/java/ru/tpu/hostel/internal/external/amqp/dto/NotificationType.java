package ru.tpu.hostel.internal.external.amqp.dto;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public enum NotificationType {
    BALANCE("Баланс"),
    DOCUMENT("Справки"),
    KITCHEN_SCHEDULE("Дежурство на кухне"),
    ROLE("Должность"),
    DUTY("Дежурство"),
    BOOKING("Запись");

    private final String notificationName;

}
