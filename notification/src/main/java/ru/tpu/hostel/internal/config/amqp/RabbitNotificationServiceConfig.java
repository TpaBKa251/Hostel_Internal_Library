package ru.tpu.hostel.internal.config.amqp;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.core.MessagePropertiesBuilder;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import ru.tpu.hostel.internal.external.amqp.Microservice;
import ru.tpu.hostel.internal.external.amqp.NotificationMessageType;

import java.util.Set;

@Configuration
@Slf4j
@EnableConfigurationProperties({
        RabbitNotificationServiceProperties.class,
        RabbitNotificationQueueingPropertiesSend.class
})
public class RabbitNotificationServiceConfig {

    private static final String NOTIFICATION_SERVICE_CONNECTION_FACTORY = "notificationServiceConnectionFactory";

    private static final String NOTIFICATION_SERVICE_AMQP_ADMIN = "notificationServiceAmqpAdmin";

    private static final String NOTIFICATION_SERVICE_RABBIT_TEMPLATE = "notificationServiceRabbitTemplate";

    private static final String NOTIFICATION_SERVICE_MESSAGE_CONVERTER = "notificationServiceMessageConverter";

    @Primary
    @Bean(NOTIFICATION_SERVICE_MESSAGE_CONVERTER)
    public MessageConverter notificationServiceMessageConverter() {
        ObjectMapper objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

        return new Jackson2JsonMessageConverter(objectMapper);
    }

    @Primary
    @Bean(NOTIFICATION_SERVICE_CONNECTION_FACTORY)
    public ConnectionFactory notificationServiceConnectionFactory(RabbitNotificationServiceProperties properties) {
        CachingConnectionFactory connectionFactory = new CachingConnectionFactory();
        connectionFactory.setUsername(properties.username());
        connectionFactory.setPassword(properties.password());
        connectionFactory.setVirtualHost(properties.virtualHost());
        connectionFactory.setAddresses(properties.addresses());
        connectionFactory.setConnectionTimeout((int) properties.connectionTimeout().toMillis());
        return connectionFactory;
    }

    @Primary
    @Bean(NOTIFICATION_SERVICE_RABBIT_TEMPLATE)
    public RabbitTemplate notificationServiceRabbitTemplate(
            @Qualifier(NOTIFICATION_SERVICE_CONNECTION_FACTORY) ConnectionFactory connectionFactory,
            @Qualifier(NOTIFICATION_SERVICE_MESSAGE_CONVERTER) MessageConverter messageConverter
    ) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(messageConverter);
        rabbitTemplate.setObservationEnabled(true);
        return rabbitTemplate;
    }

    @Primary
    @Bean(NOTIFICATION_SERVICE_AMQP_ADMIN)
    public AmqpAdmin notificationServiceAmqpAdmin(
            @Qualifier(NOTIFICATION_SERVICE_RABBIT_TEMPLATE) RabbitTemplate rabbitTemplate
    ) {
        return new RabbitAdmin(rabbitTemplate);
    }

    @Bean
    public AmqpMessagingConfig notificationServiceAmqpMessagingConfigSendNotification(
            @Qualifier(NOTIFICATION_SERVICE_CONNECTION_FACTORY) ConnectionFactory connectionFactory,
            @Qualifier(NOTIFICATION_SERVICE_MESSAGE_CONVERTER) MessageConverter messageConverter,
            RabbitNotificationQueueingPropertiesSend properties
    ) {
        return new AmqpMessagingConfig() {
            @Override
            public @NotNull RabbitTemplate rabbitTemplate() {
                RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
                rabbitTemplate.setMessageConverter(messageConverter);
                rabbitTemplate.setExchange(properties.exchangeName());
                rabbitTemplate.setRoutingKey(properties.routingKey());
                rabbitTemplate.setObservationEnabled(true);
                return rabbitTemplate;
            }

            @Override
            public @NotNull MessageProperties messageProperties() {
                return MessagePropertiesBuilder.newInstance()
                        .setPriority(0)
                        .setDeliveryMode(MessageDeliveryMode.PERSISTENT)
                        .setContentType(MessageProperties.CONTENT_TYPE_JSON)
                        .build();
            }

            @Override
            public @NotNull Set<Microservice> receivingMicroservices() {
                return Set.of(Microservice.NOTIFICATION);
            }

            @Override
            public boolean isApplicable(Enum<?> amqpMessageType) {
                return amqpMessageType == NotificationMessageType.SEND_NOTIFICATION;
            }
        };
    }

}
