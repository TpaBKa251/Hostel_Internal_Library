package ru.tpu.hostel.internal.config.amqp;

import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import ru.tpu.hostel.internal.external.amqp.Microservice;

import java.util.Set;

/**
 * Конфиг, который используется для отправки сообщений через RabbitMQ. Необходимо сконфигурировать:
 * <ul>
 * <li>{@link RabbitTemplate} для самой отправки - подключение и маршрутизация;</li>
 * <li>{@link MessageProperties} для настройки сообщения;</li>
 * <li>{@code Set} из {@link Microservice} для определения в какие микросервисы происходит отправка;</li>
 * <li>Метод {@link #isApplicable(Enum)} - фильтрация по типу сообщения для поиска конфига;</li>
 * <li><b>ОПЦИОНАЛЬНО</b> метод {@link #isApplicable(Microservice)} - второй метод для фильтрации. По умолчанию работает
 * на основе метода {@link #receivingMicroservices()}.</li>
 * </ul>
 *
 * <p>Настройки для {@code MessageProperties} (то, что вы пишите в {@link #messageProperties()} <b><i>ДОПОЛНЯЕТ</i></b>
 * эти настройки):
 * <pre><code>
 * ZonedDateTime now = TimeUtil.getZonedDateTime();
 * long nowMillis = now.toInstant().toEpochMilli();
 * ExecutionContext context = ExecutionContext.get();
 * String traceparent = String.format(TRACEPARENT_PATTERN, context.getTraceId(), context.getSpanId());
 *
 *     return MessagePropertiesBuilder.fromProperties(messageProperties)
 *        .setMessageId(messageId)
 *        .setCorrelationId(UUID.randomUUID().toString())
 *        .setTimestamp(new Date(nowMillis))
 *        .setHeader(USER_ID_HEADER, context.getUserID())
 *        .setHeader(USER_ROLES_HEADER, context.getUserRoles().toString().replace("[", "").replace("]", "").replaceAll(" ", ""))
 *        .setHeader(TRACEPARENT_HEADER, traceparent)
 *        .build();
 * </code></pre>
 * <p>
 * Пример конфига:
 * <pre>{@code
 * @Bean
 * public AmqpMessagingConfig schedulesServiceAmqpMessagingConfigBook(
 *         @Qualifier(SCHEDULES_SERVICE_CONNECTION_FACTORY) ConnectionFactory connectionFactory,
 *         @Qualifier(SCHEDULES_SERVICE_MESSAGE_CONVERTER) MessageConverter messageConverter,
 *         RabbitScheduleServiceBookQueueingProperties properties
 * ) {
 *     return new AmqpMessagingConfig() {
 *         @Override
 *         public RabbitTemplate rabbitTemplate() {
 *             RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
 *             rabbitTemplate.setMessageConverter(messageConverter);
 *             rabbitTemplate.setExchange(properties.exchangeName());
 *             rabbitTemplate.setRoutingKey(properties.routingKey());
 *             rabbitTemplate.setObservationEnabled(true);
 *             return rabbitTemplate;
 *         }
 *
 *         @Override
 *         public MessageProperties messageProperties() {
 *             return MessagePropertiesBuilder.newInstance()
 *                     .setPriority(10)
 *                     .setDeliveryMode(MessageDeliveryMode.PERSISTENT)
 *                     .setContentType(MessageProperties.CONTENT_TYPE_JSON)
 *                     .build();
 *         }
 *
 *         @Override
 *         public Set<Microservice> receivingMicroservices() {
 *             return Set.of(Microservice.SCHEDULE);
 *         }
 *
 *         @Override
 *         public boolean isApplicable(Enum<?> amqpMessageType) {
 *             return amqpMessageType == ScheduleMessageType.BOOK;
 *         }
 *     };
 * }
 * }</pre>
 *
 * @author Илья Лапшин
 * @version 1.0.4
 * @since 1.0.4
 */
public interface AmqpMessagingConfig {

    /**
     * Метод, где необходимо написать конфигурацию {@link RabbitTemplate} для отправки сообщения.
     * <p>
     * Пример:
     * <pre>{@code
     * @Override
     * public RabbitTemplate rabbitTemplate() {
     *     RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
     *     rabbitTemplate.setMessageConverter(messageConverter);
     *     rabbitTemplate.setExchange(properties.exchangeName());
     *     rabbitTemplate.setRoutingKey(properties.routingKey());
     *     rabbitTemplate.setObservationEnabled(true);
     *     return rabbitTemplate;
     * }
     * }</pre>
     *
     * @return экземпляр RabbitTemplate для отправки
     */
    RabbitTemplate rabbitTemplate();

    /**
     * Дополнительные настройки отправляемого сообщения. Дополняет настройки:
     * <pre><code>
     * ZonedDateTime now = TimeUtil.getZonedDateTime();
     * long nowMillis = now.toInstant().toEpochMilli();
     * ExecutionContext context = ExecutionContext.get();
     * String traceparent = String.format(TRACEPARENT_PATTERN, context.getTraceId(), context.getSpanId());
     *
     *     return MessagePropertiesBuilder.fromProperties(messageProperties)
     *        .setMessageId(messageId)
     *        .setCorrelationId(UUID.randomUUID().toString())
     *        .setTimestamp(new Date(nowMillis))
     *        .setHeader(USER_ID_HEADER, context.getUserID())
     *        .setHeader(USER_ROLES_HEADER, context.getUserRoles().toString().replace("[", "").replace("]", "").replaceAll(" ", ""))
     *        .setHeader(TRACEPARENT_HEADER, traceparent)
     *        .build();
     * </code></pre>
     * <p>
     * Пример:
     * <pre>{@code
     * @Override
     * public MessageProperties messageProperties() {
     *     return MessagePropertiesBuilder.newInstance()
     *             .setPriority(10)
     *             .setDeliveryMode(MessageDeliveryMode.PERSISTENT)
     *             .setContentType(MessageProperties.CONTENT_TYPE_JSON)
     *             .build();
     * }
     * }</pre>
     *
     * @return экземпляр MessageProperties
     */
    MessageProperties messageProperties();

    /**
     * Сет из микросервисов, которые являются получателями сообщения.
     * <p>
     * Пример:
     * <pre>{@code
     * @Override
     * public Set<Microservice> receivingMicroservices() {
     *     return Set.of(Microservice.SCHEDULE);
     * }
     * }</pre>
     *
     * @return сет из Microservice
     */
    Set<Microservice> receivingMicroservices();

    /**
     * Метод для фильтрации конфига на основе типа сообщения. Нужен, чтобы выбрать верный конфиг для отправки сообщения.
     * <p>
     * Пример:
     * <pre>{@code
     * @Override
     * public boolean isApplicable(Enum<?> amqpMessageType) {
     *     return amqpMessageType == ScheduleMessageType.BOOK; // Можно использовать любой Enum
     * }
     * }</pre>
     *
     * @param amqpMessageType енам для фильтрации (любой)
     * @return подходит конфиг или нет
     */
    boolean isApplicable(Enum<?> amqpMessageType);

    /**
     * Опциональный для переопределения метод. Нужен для фильтрации конфига на основе микросервиса-получателя.
     *
     * @param microservice енам микросервиса-получателя
     * @return подходит конфиг или нет
     */
    default boolean isApplicable(Microservice microservice) {
        return receivingMicroservices().contains(microservice);
    }

}
