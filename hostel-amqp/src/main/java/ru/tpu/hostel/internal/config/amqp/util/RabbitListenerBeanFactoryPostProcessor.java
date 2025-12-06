package ru.tpu.hostel.internal.config.amqp.util;

import io.opentelemetry.api.OpenTelemetry;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.amqp.core.AcknowledgeMode;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.Ordered;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import ru.tpu.hostel.internal.config.amqp.customizer.SimpleRabbitListenerContainerFactoryCustomizer;
import ru.tpu.hostel.internal.config.amqp.properties.RabbitProperties;
import ru.tpu.hostel.internal.config.amqp.tracing.TracedConnectionFactory;
import ru.tpu.hostel.internal.config.amqp.tracing.interceptor.AmqpMessageReceiveInterceptor;
import ru.tpu.hostel.internal.external.amqp.Microservice;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Component
public class RabbitListenerBeanFactoryPostProcessor implements
        BeanDefinitionRegistryPostProcessor, Ordered, EnvironmentAware, ApplicationContextAware {

    private static final String LISTENER_POSTFIX = "RabbitListener";

    private static final String LISTENERS_BEAN_NAMES_BEAN_NAME = "rabbitListenersBeanNames";

    private Environment environment;

    private ApplicationContext applicationContext;

    private ConfigurableListableBeanFactory beanFactory;

    @Override
    public void setEnvironment(@NotNull Environment environment) {
        this.environment = environment;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
        this.beanFactory = (ConfigurableListableBeanFactory)
                applicationContext.getAutowireCapableBeanFactory();
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE; // Выполняем ПОСЛЕ всех остальных BeanDefinitionRegistryPostProcessor
    }

    @Override
    public void postProcessBeanDefinitionRegistry(@NotNull BeanDefinitionRegistry registry) {
        // 1. Читаем свойства НЕ через бин, а через Environment + Binder
        RabbitProperties rabbitProperties = Binder.get(environment)
                .bind("rabbitmq", RabbitProperties.class)
                .orElseThrow(() -> new IllegalStateException("Не удалось прочитать RabbitProperties"));

        // 2. Регистрируем мапу имен слушателей (пока без создания фабрик)
        Map<Microservice, Map<String, Map<String, String>>> listenersBeanNames = new EnumMap<>(Microservice.class);

        rabbitProperties.properties().forEach((microservice, servicePropertiesMap) -> {
            Map<String, Map<String, String>> serviceMap = new HashMap<>();
            servicePropertiesMap.forEach((serviceName, serviceProperties) -> {
                Map<String, String> listenerToBeanNameMap = new HashMap<>();

                Set<String> listenerNames = serviceProperties.queueingProperties()
                        .listeners()
                        .keySet()
                        .stream()
                        .map(s -> Arrays.stream(s.split(" "))
                                .map(s1 -> StringUtils.capitalize(s1.trim()))
                                .collect(Collectors.joining())
                        )
                        .collect(Collectors.toSet());

                listenerNames.forEach(listenerName -> {
                    String beanName = microservice.name().toLowerCase()
                            + StringUtils.capitalize(serviceName)
                            + listenerName
                            + LISTENER_POSTFIX;

                    listenerToBeanNameMap.put(listenerName, beanName);
                });

                serviceMap.put(serviceName, listenerToBeanNameMap);
            });
            listenersBeanNames.put(microservice, serviceMap);
        });

        // Регистрируем мапу имен как singleton (пока пустая, фабрики создадим позже)
        beanFactory.registerSingleton(LISTENERS_BEAN_NAMES_BEAN_NAME, listenersBeanNames);

        log.info("Зарегистрирована мапа имен слушателей RabbitMQ");
    }

    @Override
    public void postProcessBeanFactory(@NotNull ConfigurableListableBeanFactory beanFactory) {
        // 3. Теперь, когда все BeanDefinition созданы, можем безопасно получать бины
        try {
            RabbitProperties rabbitProperties = applicationContext.getBean(RabbitProperties.class);
            OpenTelemetry openTelemetry = applicationContext.getBean(OpenTelemetry.class);

            @SuppressWarnings("unchecked")
            Map<Microservice, Map<String, TracedConnectionFactory>> connectionFactories =
                    (Map<Microservice, Map<String, TracedConnectionFactory>>)
                            applicationContext.getBean("customConnectionFactories");

            // 4. Создаем и регистрируем фабрики слушателей
            connectionFactories.forEach((microservice, innerMap) -> {
                innerMap.forEach((name, connectionFactory) -> {
                    rabbitProperties.properties()
                            .get(microservice)
                            .get(name)
                            .queueingProperties()
                            .listeners()
                            .forEach((listenerKey, listenerProperties) -> {
                                String listenerName = Arrays.stream(listenerKey.split(" "))
                                        .map(s -> StringUtils.capitalize(s.trim()))
                                        .collect(Collectors.joining());

                                String beanName = microservice.name().toLowerCase()
                                        + StringUtils.capitalize(name)
                                        + listenerName
                                        + LISTENER_POSTFIX;

                                SimpleRabbitListenerContainerFactory listenerFactory = createListenerContainerFactory(
                                        connectionFactory,
                                        getCustomizer(listenerProperties.customizerName()),
                                        openTelemetry
                                );

                                log.debug("Создание фабрики слушателя: {}", beanName);
                                beanFactory.registerSingleton(beanName, listenerFactory);
                            });
                });
            });

            log.info("Фабрики RabbitListener успешно созданы и зарегистрированы");

        } catch (Exception e) {
            log.error("Ошибка при создании фабрик RabbitListener", e);
            throw new IllegalStateException("Не удалось создать фабрики RabbitListener", e);
        }
    }

    private SimpleRabbitListenerContainerFactory createListenerContainerFactory(
            TracedConnectionFactory connectionFactory,
            SimpleRabbitListenerContainerFactoryCustomizer customizer,
            OpenTelemetry openTelemetry
    ) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        if (customizer != null) {
            customizer.customize(factory);
        }
        factory.setAcknowledgeMode(AcknowledgeMode.AUTO);
        factory.setDefaultRequeueRejected(false);
        factory.setConnectionFactory(connectionFactory);
        factory.setAdviceChain(new AmqpMessageReceiveInterceptor(openTelemetry));

        return factory;
    }

    private SimpleRabbitListenerContainerFactoryCustomizer getCustomizer(String beanName) {
        if (!StringUtils.hasText(beanName)) {
            return null;
        }

        try {
            return applicationContext.getBean(beanName, SimpleRabbitListenerContainerFactoryCustomizer.class);
        } catch (NoSuchBeanDefinitionException e) {
            throw new IllegalStateException(
                    String.format("""
                                    Не найден бин кастомайзера '%s'.
                                    
                                    Создайте бин с этим именем:
                                    
                                    @Bean("%s")
                                    public SimpleRabbitListenerContainerFactoryCustomizer %s() {
                                        return factory -> {
                                            // настройка объекта
                                        };
                                    }
                                    """,
                            beanName,
                            beanName,
                            beanName
                    ),
                    e
            );
        }
    }
}