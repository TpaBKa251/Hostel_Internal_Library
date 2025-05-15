package ru.tpu.hostel.internal.external.amqp;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Енам микросервисов
 *
 * @author Илья Лапшин
 * @version 1.0.4
 * @since 1.0.4
 */
@RequiredArgsConstructor
@Getter
public enum Microservice {

    USER("user-service"),
    BOOKING("booking-service"),
    SCHEDULE("schedule-service"),
    ADMINISTRATION("administration-service"),
    NOTIFICATION("notification-service");

    private final String serviceName;

    /**
     * Метод для получения енама по имени микросервиса в формате name-service (например, user-service)
     *
     * @param serviceName имя микросервиса
     * @return енам, соответствующий имени
     */
    public static Microservice fromServiceName(String serviceName) {
        for (Microservice microservice : values()) {
            if (microservice.serviceName.equals(serviceName) || microservice.name().equalsIgnoreCase(serviceName)) {
                return microservice;
            }
        }

        return null;
    }
}
