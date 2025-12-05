package ru.tpu.hostel.internal.config.amqp;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.opentelemetry.api.OpenTelemetry;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;
import ru.tpu.hostel.internal.config.amqp.customizer.Customizer;
import ru.tpu.hostel.internal.config.amqp.customizer.RabbitTemplateCustomizer;
import ru.tpu.hostel.internal.config.amqp.customizer.TracedConnectionFactoryCustomizer;
import ru.tpu.hostel.internal.config.amqp.properties.RabbitConnectionProperties;
import ru.tpu.hostel.internal.config.amqp.properties.RabbitProperties;
import ru.tpu.hostel.internal.config.amqp.properties.RabbitSenderProperties;
import ru.tpu.hostel.internal.config.amqp.tracing.TracedConnectionFactory;
import ru.tpu.hostel.internal.external.amqp.Microservice;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Configuration
@EnableConfigurationProperties(RabbitProperties.class)
@RequiredArgsConstructor
public class RabbitConfiguration {

    @Bean
    Map<Microservice, Map<String, MessageConverter>> messageConverters(
            RabbitProperties rabbitProperties,
            ApplicationContext applicationContext
    ) {
        Map<Microservice, Map<String, MessageConverter>> converters = new EnumMap<>(Microservice.class);
        ObjectMapper defaultObjectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

        rabbitProperties.properties().forEach((microservice, servicePropertiesMap) -> {
            Map<String, MessageConverter> converterMap = new HashMap<>();
            servicePropertiesMap.forEach((propertiesName, serviceProperties) -> {
                MessageConverter converter = getBean(
                        serviceProperties.messageConverterName(),
                        applicationContext,
                        MessageConverter.class
                );
                if (converter == null) {
                    converter = new Jackson2JsonMessageConverter(defaultObjectMapper);
                }
                converterMap.put(propertiesName, converter);
            });
            converters.put(microservice, converterMap);
        });

        return converters;
    }

    @Bean
    Map<Microservice, Map<String, TracedConnectionFactory>> connectionFactories(
            OpenTelemetry openTelemetry,
            RabbitProperties rabbitProperties,
            ApplicationContext applicationContext
    ) {
        Map<Microservice, Map<String, TracedConnectionFactory>> connectionFactories = new EnumMap<>(Microservice.class);
        Map<String, TracedConnectionFactory> connectionFactoriesMap = new HashMap<>();

        rabbitProperties.properties().forEach(((microservice, stringRabbitServicePropertiesMap) -> {
            stringRabbitServicePropertiesMap.forEach((servicePropertiesName, serviceProperties) -> {
                String customizerName = serviceProperties.connectionProperties().customizerName();
                TracedConnectionFactoryCustomizer customizer = getBean(
                        customizerName,
                        applicationContext,
                        TracedConnectionFactoryCustomizer.class
                );

                TracedConnectionFactory factory = getTracedConnectionFactory(
                        serviceProperties.connectionProperties(),
                        customizer,
                        openTelemetry
                );

                connectionFactoriesMap.put(servicePropertiesName, factory);
            });
            connectionFactories.put(microservice, connectionFactoriesMap);
        }));

        return connectionFactories;
    }

    private TracedConnectionFactory getTracedConnectionFactory(
            RabbitConnectionProperties connectionProperties,
            TracedConnectionFactoryCustomizer customizer,
            OpenTelemetry openTelemetry
    ) {
        CachingConnectionFactory connectionFactory = new CachingConnectionFactory();
        if (customizer != null) {
            customizer.customize(connectionFactory);
        }
        connectionFactory.setUsername(connectionProperties.username());
        connectionFactory.setPassword(connectionProperties.password());
        connectionFactory.setVirtualHost(connectionProperties.virtualHost());
        connectionFactory.setAddresses(connectionProperties.addresses());
        connectionFactory.setConnectionTimeout((int) connectionProperties.connectionTimeout().toMillis());
        return new TracedConnectionFactory(connectionFactory, openTelemetry);
    }

    @Bean
    Map<Microservice, Map<String, RabbitTemplate>> rabbitTemplates(
            Map<Microservice, Map<String, TracedConnectionFactory>> connectionFactories,
            Map<Microservice, Map<String, MessageConverter>> messageConverters,
            RabbitProperties rabbitProperties
    ) {
        Map<Microservice, Map<String, RabbitTemplate>> rabbitTemplates = new EnumMap<>(Microservice.class);

        rabbitProperties.properties().forEach((microservice, servicePropertiesMap) -> {
            Map<String, RabbitTemplate> rabbitTemplateMap = new HashMap<>();

            servicePropertiesMap.forEach((propertiesName, _) -> {
                TracedConnectionFactory connectionFactory = connectionFactories.get(microservice).get(propertiesName);
                MessageConverter messageConverter = messageConverters.get(microservice).get(propertiesName);

                RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
                rabbitTemplate.setMessageConverter(messageConverter);
                rabbitTemplate.setObservationEnabled(true);

                rabbitTemplateMap.put(propertiesName, rabbitTemplate);
            });
            rabbitTemplates.put(microservice, rabbitTemplateMap);
        });

        return rabbitTemplates;
    }

    @Bean
    Map<Microservice, Map<String, RabbitAdmin>> amqpAdmins(
            Map<Microservice, Map<String, RabbitTemplate>> rabbitTemplates,
            RabbitProperties rabbitProperties
    ) {
        Map<Microservice, Map<String, RabbitAdmin>> amqpAdmins = new EnumMap<>(Microservice.class);
        rabbitProperties.properties().forEach((microservice, servicePropertiesMap) -> {
            Map<String, RabbitAdmin> rabbitAdminMap = new HashMap<>();

            servicePropertiesMap.forEach((propertiesName, serviceProperties) -> {
                RabbitAdmin rabbitAdmin = new RabbitAdmin(rabbitTemplates.get(microservice).get(propertiesName));
                rabbitAdminMap.put(propertiesName, rabbitAdmin);

                serviceProperties.queueingProperties().senders().forEach((_, sender) ->
                        initQueue(rabbitAdmin, sender)
                );

                serviceProperties.queueingProperties().listeners().forEach((_, listener) ->
                        initQueue(rabbitAdmin, listener.queueName())
                );
            });
            amqpAdmins.put(microservice, rabbitAdminMap);
        });

        return amqpAdmins;
    }

    private void initQueue(RabbitAdmin rabbitAdmin, RabbitSenderProperties rabbitSenderProperties) {
        DirectExchange exchange = new DirectExchange(rabbitSenderProperties.exchangeName());

        Queue queue = QueueBuilder.durable(rabbitSenderProperties.queueName())
                .quorum()
                .build();

        rabbitAdmin.declareQueue(queue);
        rabbitAdmin.declareExchange(exchange);
        declareAndBindQueue(rabbitAdmin, rabbitSenderProperties.routingKey(), exchange, queue);
    }

    private void initQueue(RabbitAdmin rabbitAdmin, String queueName) {
        Queue queue = QueueBuilder.durable(queueName)
                .quorum()
                .build();

        rabbitAdmin.declareQueue(queue);
    }

    private void declareAndBindQueue(
            RabbitAdmin rabbitAdmin,
            String replyRoutingKey,
            DirectExchange exchange,
            Queue queue
    ) {
        Binding binding = BindingBuilder.bind(queue).to(exchange).with(replyRoutingKey);

        rabbitAdmin.declareQueue(queue);
        rabbitAdmin.declareBinding(binding);
    }

    @Bean
    Set<AmqpMessagingConfig> amqpMessagingConfigs(
            Map<Microservice, Map<String, TracedConnectionFactory>> connectionFactories,
            Map<Microservice, Map<String, MessageConverter>> messageConverters,
            RabbitProperties rabbitProperties,
            ApplicationContext applicationContext
    ) {
        Set<AmqpMessagingConfig> amqpMessagingConfigs = new HashSet<>();

        connectionFactories.forEach((microservice, connectionFactoriesMap) ->
                connectionFactoriesMap.forEach((name, connectionFactory) -> {
                    Map<String, RabbitSenderProperties> senders = rabbitProperties.properties()
                            .get(microservice)
                            .get(name)
                            .queueingProperties()
                            .senders();

                    senders.forEach((type, senderProperties) -> {
                        RabbitTemplateCustomizer rabbitTemplateCustomizer = getBean(
                                senderProperties.rabbitTemplateCustomizerName(),
                                applicationContext,
                                RabbitTemplateCustomizer.class
                        );

                        AmqpMessagingConfig config = new AmqpMessagingConfig() {
                            @Override
                            public @NotNull RabbitTemplate rabbitTemplate() {
                                return getRabbitTemplate(
                                        connectionFactory,
                                        messageConverters.get(microservice).get(name),
                                        senderProperties,
                                        rabbitTemplateCustomizer
                                );
                            }

                            @Override
                            public @NotNull MessageProperties defaultMessageProperties() {
                                MessageProperties messageProperties = getBean(
                                        senderProperties.messagePropertiesBeanName(),
                                        applicationContext,
                                        MessageProperties.class
                                );
                                return messageProperties == null
                                        ? new MessageProperties()
                                        : messageProperties;
                            }

                            @Override
                            public @NotNull Set<Microservice> receivingMicroservices() {
                                return Set.of(microservice);
                            }

                            @Override
                            public boolean isApplicable(@NotNull Enum<?> amqpMessageType) {
                                return amqpMessageType.name().equalsIgnoreCase(type);
                            }
                        };

                        amqpMessagingConfigs.add(config);
                    });
                }));

        return amqpMessagingConfigs;
    }

    private RabbitTemplate getRabbitTemplate(
            TracedConnectionFactory connectionFactory,
            MessageConverter messageConverter,
            RabbitSenderProperties senderProperties,
            RabbitTemplateCustomizer rabbitTemplateCustomizer
    ) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate();
        if (rabbitTemplateCustomizer != null) {
            rabbitTemplateCustomizer.customize(rabbitTemplate);
        }
        rabbitTemplate.setConnectionFactory(connectionFactory);
        rabbitTemplate.setMessageConverter(messageConverter);
        rabbitTemplate.setExchange(senderProperties.exchangeName());
        rabbitTemplate.setRoutingKey(senderProperties.routingKey());
        rabbitTemplate.setObservationEnabled(true);
        rabbitTemplate.setChannelTransacted(senderProperties.channelTransacted());
        return rabbitTemplate;
    }

    private <T> T getBean(
            String beanName,
            ApplicationContext applicationContext,
            Class<T> beanClass
    ) {
        if (!StringUtils.hasText(beanName)) {
            return null;
        }

        try {
            return applicationContext.getBean(beanName, beanClass);
        } catch (NoSuchBeanDefinitionException e) {
            if (Customizer.class.isAssignableFrom(beanClass)) {
                throw new IllegalStateException(
                        String.format("""
                                        Не найден бин кастомайзера '%s'.
                                        
                                        Создайте бин с этим именем:
                                        
                                        @Bean("%s")
                                        public %s %s() {
                                            return o -> {
                                                // настройка объекта
                                            };
                                        }
                                        """,
                                beanName,
                                beanName,
                                beanClass.getSimpleName(),
                                beanName
                        ),
                        e
                );
            } else if (MessageProperties.class.equals(beanClass)) {
                throw new IllegalStateException(
                        String.format("""
                                        Не найден бин MessageProperties '%s'
                                        
                                        Создайте бин с этим именем:
                                        
                                        @Bean("%s")
                                        public MessageProperties %s() {
                                            // Настройка
                                            return MessagePropertiesBuilder.newInstance()
                                                    .<установка параметров>
                                                    .build();
                                        }
                                        """,
                                beanName,
                                beanName,
                                beanName
                        ),
                        e
                );
            } else if (MessageConverter.class.equals(beanClass)) {
                throw new IllegalStateException(
                        String.format("""
                                        Не найден бин MessageConverter '%s'
                                        
                                        Создайте бин с этим именем:
                                        
                                        @Bean("%s")
                                        public MessageConverter %s() {
                                            ObjectMapper objectMapper = new ObjectMapper()
                                                    .<настройка>
                                            return new Jackson2JsonMessageConverter(objectMapper);
                                        }
                                        """,
                                beanName,
                                beanName,
                                beanName
                        ),
                        e
                );
            }
            throw e;
        }
    }

}
