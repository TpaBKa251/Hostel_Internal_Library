package ru.tpu.hostel.internal.external.amqp;

import org.springframework.amqp.core.MessageProperties;
import ru.tpu.hostel.internal.exception.ServiceException;
import ru.tpu.hostel.internal.external.amqp.impl.DefaultAmqpMessageSender;

/**
 * Интерфейс для отправки сообщений через RabbitMQ. Имеет дефолтную универсальную реализацию
 * {@link DefaultAmqpMessageSender}
 * <p>
 * Примеры использования:
 * <ul>
 * <li>RPC отправка {@link #sendAndReceive(Enum, String, Object, Class)}</li>
 * <pre>{@code
 * ScheduleResponse scheduleResponse = amqpMessageSender.sendAndReceive(
 *         ScheduleMessageType.BOOK,
 *         bookingTimeSlotRequest.slotId().toString(),
 *         bookingTimeSlotRequest.slotId(),
 *         ScheduleResponse.class
 * );
 * }</pre>
 * <li>Асинхронная отправка {@link #send(Enum, String, Object)}</li>
 * <pre>{@code
 * amqpMessageSender.send(
 *         ScheduleMessageType.CANCEL,
 *         bookingToCancel.getId().toString(),
 *         bookingToCancel.getTimeSlot()
 * );
 * }</pre>
 * <li>Асинхронная отправка ответа {@link #sendReply(Enum, MessageProperties, Object)}</li>
 * <pre>{@code
 * @RabbitListener(queues = "${queueing.timeslots.queueName}", containerFactory = TIMESLOT_LISTENER)
 * public void receiveTimeslotMessage(Message message) {
 *     MessageProperties messageProperties = message.getMessageProperties(); // Обязательно нужны свойства полученного сообщения
 *     ScheduleResponse timeSlotResponse = ScheduleResponseMapper.mapTimeslotResponse(timeSlot);
 *     amqpMessageSender.sendReply(TimeslotMessageType.BOOK_REPLY, messageProperties, timeSlotResponse);
 * }
 * }</pre>
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
    void send(Enum<?> messageType, String messageId, Object messagePayload);

    /**
     * Синхронная RPC отправка. Сразу возвращает ответ
     *
     * @param messageType    тип отправляемого сообщения
     * @param messageId      ID сообщения
     * @param messagePayload содержимое сообщения
     * @param responseType   класс ответа
     * @return ответ на сообщение
     */
    <R> R sendAndReceive(Enum<?> messageType, String messageId, Object messagePayload, Class<R> responseType);

    /**
     * Асинхронная отправка ответа. Используется на стороне получателя при RPC сообщениях. Особенность заключается в
     * необходимости передачи свойств полученного сообщения
     *
     * @param messageType    тип отправляемого сообщения
     * @param properties     свойства полученного сообщения
     * @param messagePayload содержимое ответа
     */
    void sendReply(Enum<?> messageType, MessageProperties properties, Object messagePayload);

    /**
     * Асинхронная отправка в микросервис по ключу маршрутизации.
     *
     * @param microservice   микросервис-получатель
     * @param routingKey     ключ маршрутизации
     * @param messageId      ID сообщения
     * @param messagePayload содержимое сообщения
     */
    default void send(Microservice microservice, String routingKey, String messageId, Object messagePayload) {
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
            Microservice microservice,
            String exchange,
            String routingKey,
            String messageId,
            Object messagePayload
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
    default <R> R sendAndReceive(
            Microservice microservice,
            String routingKey,
            String messageId,
            Object messagePayload,
            Class<R> responseType
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
