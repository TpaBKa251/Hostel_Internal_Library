package ru.tpu.hostel.internal.external.amqp;

import org.jetbrains.annotations.NotNull;
import org.springframework.amqp.core.MessageProperties;
import ru.tpu.hostel.internal.exception.ServiceException;
import ru.tpu.hostel.internal.external.amqp.impl.DefaultAmqpMessageSender;

/**
 * Интерфейс для отправки сообщений через RabbitMQ. Имеет дефолтную универсальную реализацию
 * {@link DefaultAmqpMessageSender}.
 * <p>
 * Примеры использования:
 * </p>
 * <ul>
 * <li>RPC-отправка с получением ответа {@link #sendAndReceive(Enum, String, Object, Class)}:
 * <pre>{@code
 * ScheduleResponse scheduleResponse = amqpMessageSender.sendAndReceive(
 *         ScheduleMessageType.BOOK,
 *         bookingTimeSlotRequest.slotId().toString(),
 *         bookingTimeSlotRequest.slotId(),
 *         ScheduleResponse.class
 * );
 * }</pre>
 * </li>
 * <li>Асинхронная отправка сообщения {@link #send(Enum, String, Object)}:
 * <pre>{@code
 * amqpMessageSender.send(
 *         ScheduleMessageType.CANCEL,
 *         bookingToCancel.getId().toString(),
 *         bookingToCancel.getTimeSlot()
 * );
 * }</pre>
 * </li>
 * <li>Асинхронная отправка ответа {@link #sendReply(Enum, MessageProperties, Object)}:
 * <pre>{@code
 * @RabbitListener(queues = "${queueing.timeslots.queueName}", containerFactory = TIMESLOT_LISTENER)
 * public void receiveTimeslotMessage(Message message) {
 *     MessageProperties messageProperties = message.getMessageProperties();
 *     ScheduleResponse timeSlotResponse = ScheduleResponseMapper.mapTimeslotResponse(timeSlot);
 *     amqpMessageSender.sendReply(TimeslotMessageType.BOOK_REPLY, messageProperties, timeSlotResponse);
 * }
 * }</pre>
 * </li>
 * </ul>
 *
 * @author Илья Лапшин
 * @version 1.0.10
 * @since 1.0.7
 */
public interface AmqpMessageSender {

    /**
     * Стандартная асинхронная отправка
     *
     * @param messageType    тип отправляемого сообщения
     * @param messageId      ID сообщения
     * @param messagePayload содержимое сообщения
     */
    void send(@NotNull Enum<?> messageType, @NotNull String messageId, @NotNull Object messagePayload);

    /**
     * Синхронная RPC отправка. Сразу возвращает ответ
     *
     * @param messageType    тип отправляемого сообщения
     * @param messageId      ID сообщения
     * @param messagePayload содержимое сообщения
     * @param responseType   класс ответа
     * @return ответ на сообщение
     */
    @NotNull
    <R> R sendAndReceive(
            @NotNull Enum<?> messageType,
            @NotNull String messageId,
            @NotNull Object messagePayload,
            @NotNull Class<R> responseType
    );

    /**
     * Асинхронная отправка ответа. Используется на стороне получателя при RPC сообщениях. Особенность заключается в
     * необходимости передачи свойств полученного сообщения
     *
     * @param messageType    тип отправляемого сообщения
     * @param properties     свойства полученного сообщения
     * @param messagePayload содержимое ответа
     */
    void sendReply(@NotNull Enum<?> messageType, @NotNull MessageProperties properties, @NotNull Object messagePayload);

    /**
     * Асинхронная отправка в микросервис по ключу маршрутизации.
     *
     * @param microservice   микросервис-получатель
     * @param routingKey     ключ маршрутизации
     * @param messageId      ID сообщения
     * @param messagePayload содержимое сообщения
     */
    default void send(
            @NotNull Microservice microservice,
            @NotNull String routingKey,
            @NotNull String messageId,
            @NotNull Object messagePayload
    ) {
        throw new ServiceException.NotImplemented();
    }

    /**
     * Асинхронная отправка в микросервис по ключу маршрутизации и обменнику.
     *
     * @param microservice   микросервис-получатель
     * @param exchange       обменник
     * @param routingKey     ключ маршрутизации
     * @param messageId      ID сообщения
     * @param messagePayload содержимое сообщения
     */
    default void send(
            @NotNull Microservice microservice,
            @NotNull String exchange,
            @NotNull String routingKey,
            @NotNull String messageId,
            @NotNull Object messagePayload
    ) {
        throw new ServiceException.NotImplemented();
    }

    /**
     * Синхронная RPC отправка в микросервис по ключу маршрутизации.
     *
     * @param microservice   микросервис-получатель
     * @param routingKey     ключ маршрутизации
     * @param messageId      ID сообщения
     * @param messagePayload содержимое сообщения
     * @param responseType   класс ответа
     * @return ответ на сообщение
     */
    @NotNull
    default <R> R sendAndReceive(
            @NotNull Microservice microservice,
            @NotNull String routingKey,
            @NotNull String messageId,
            @NotNull Object messagePayload,
            @NotNull Class<R> responseType
    ) {
        throw new ServiceException.NotImplemented();
    }

    /**
     * Синхронная RPC отправка в микросервис по ключу маршрутизации и обменнику.
     *
     * @param microservice   микросервис-получатель
     * @param exchange       обменник
     * @param routingKey     ключ маршрутизации
     * @param messageId      ID сообщения
     * @param messagePayload содержимое сообщения
     * @param responseType   класс ответа
     * @return ответ на сообщение
     */
    @NotNull
    default <R> R sendAndReceive(
            @NotNull Microservice microservice,
            @NotNull String exchange,
            @NotNull String routingKey,
            @NotNull String messageId,
            @NotNull Object messagePayload,
            @NotNull Class<R> responseType
    ) {
        throw new ServiceException.NotImplemented();
    }

}
