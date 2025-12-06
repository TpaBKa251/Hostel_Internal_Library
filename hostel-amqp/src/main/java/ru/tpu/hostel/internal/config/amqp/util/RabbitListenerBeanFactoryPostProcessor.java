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
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.Ordered;
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
        BeanDefinitionRegistryPostProcessor, Ordered, ApplicationContextAware {

    private static final String LISTENER_POSTFIX = "RabbitListener";

    private static final String LISTENERS_BEAN_NAMES_BEAN_NAME = "rabbitListenersBeanNames";

    private ConfigurableListableBeanFactory beanFactory;

    private ApplicationContext applicationContext;

    @Override
    public void postProcessBeanDefinitionRegistry(@NotNull BeanDefinitionRegistry registry) {
        RabbitProperties rabbitProperties = applicationContext.getBean(RabbitProperties.class);
        OpenTelemetry openTelemetry = applicationContext.getBean(OpenTelemetry.class);

        @SuppressWarnings("unchecked")
        Map<Microservice, Map<String, TracedConnectionFactory>> connectionFactories =
                (Map<Microservice, Map<String, TracedConnectionFactory>>)
                        applicationContext.getBean("customConnectionFactories");

        Map<Microservice, Map<String, Map<String, String>>> listenersBeanNames = new EnumMap<>(Microservice.class);
        connectionFactories.forEach((microservice, innerMap) -> {
            Map<String, Map<String, String>> serviceMap = new HashMap<>();
            innerMap.forEach((name, connectionFactory) -> {
                Map<String, String> listenerToBeanNameMap = new HashMap<>();

                Set<String> listenerNames = rabbitProperties.properties()
                        .get(microservice)
                        .get(name)
                        .queueingProperties()
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
                            + StringUtils.capitalize(name)
                            + listenerName
                            + LISTENER_POSTFIX;

                    listenerToBeanNameMap.put(listenerName, beanName);

                    SimpleRabbitListenerContainerFactory listenerFactory = createListenerContainerFactory(
                            connectionFactory,
                            getCustomizer(
                                    rabbitProperties.properties()
                                            .get(microservice)
                                            .get(name)
                                            .queueingProperties()
                                            .listeners()
                                            .get(listenerName)
                                            .customizerName()
                            ),
                            openTelemetry
                    );

                    log.debug("Create listener bean: {}", beanName);
                    beanFactory.registerSingleton(beanName, listenerFactory);
                });
                serviceMap.put(name, listenerToBeanNameMap);
            });
            listenersBeanNames.put(microservice, serviceMap);
        });

        beanFactory.registerSingleton(LISTENERS_BEAN_NAMES_BEAN_NAME, listenersBeanNames);
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
        this.beanFactory = (ConfigurableListableBeanFactory)
                applicationContext.getAutowireCapableBeanFactory();
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 10;
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

    private SimpleRabbitListenerContainerFactoryCustomizer getCustomizer(
            String beanName
    ) {
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
