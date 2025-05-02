package ru.tpu.hostel.internal.external.amqp;

import ru.tpu.hostel.internal.exception.ServiceException;

public interface AmqpMessageSender {

    void send(Enum<?> messageType, String messageId, Object messagePayload);

    <R> R sendAndReceive(Enum<?> messageType, String messageId, Object messagePayload, Class<R> responseType);

    default void send(Microservice microservice, String routingKey, String messageId, Object messagePayload) {
        throw new ServiceException.NotImplemented();
    }

    default void send(
            Microservice microservice,
            String exchange,
            String routingKey,
            String messageId,
            Object messagePayload
    ) {
        throw new ServiceException.NotImplemented();
    }

    default <R> R sendAndReceive(
            Microservice microservice,
            String routingKey,
            String messageId,
            Object messagePayload,
            Class<R> responseType
    ) {
        throw new ServiceException.NotImplemented();
    }

    default <R> R sendAndReceive(
            Microservice microservice,
            String exchange,
            String routingKey,
            String messageId,
            Object messagePayload,
            Class<R> responseType
    ) {
        throw new ServiceException.NotImplemented();
    }

}
