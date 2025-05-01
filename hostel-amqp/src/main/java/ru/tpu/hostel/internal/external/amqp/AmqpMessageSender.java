package ru.tpu.hostel.internal.external.amqp;

import java.io.IOException;

public interface AmqpMessageSender {

    <T extends Enum<?> & AmqpMessageType> void send(T messageType, String messageId, Object messagePayload)
            throws IOException;

    <T extends Enum<?> & AmqpMessageType, R> R sendAndReceive(
            T messageType,
            String messageId,
            Object messagePayload,
            Class<R> responseType
    ) throws IOException;

}
