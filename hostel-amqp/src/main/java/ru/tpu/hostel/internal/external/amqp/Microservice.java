package ru.tpu.hostel.internal.external.amqp;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public enum Microservice {

    USER("user-service"),
    BOOKING("booking-service"),
    SCHEDULE("schedule-service"),
    ADMINISTRATION("administration-service"),
    NOTIFICATION("notification-service");

    private final String serviceName;

    public static Microservice fromServiceName(String serviceName) {
        for (Microservice microservice : values()) {
            if (microservice.serviceName.equals(serviceName) || microservice.name().equalsIgnoreCase(serviceName)) {
                return microservice;
            }
        }

        return null;
    }
}
