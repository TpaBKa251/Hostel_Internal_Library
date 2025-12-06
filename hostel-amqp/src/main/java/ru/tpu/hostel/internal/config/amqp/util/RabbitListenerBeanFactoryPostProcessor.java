//package ru.tpu.hostel.internal.config.amqp.util;
//
//import io.opentelemetry.api.OpenTelemetry;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.amqp.core.AcknowledgeMode;
//import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
//import org.springframework.beans.factory.NoSuchBeanDefinitionException;
//import org.springframework.beans.factory.SmartInitializingSingleton;
//import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
//import org.springframework.context.ApplicationContext;
//import org.springframework.stereotype.Component;
//import org.springframework.util.StringUtils;
//import ru.tpu.hostel.internal.config.amqp.customizer.SimpleRabbitListenerContainerFactoryCustomizer;
//import ru.tpu.hostel.internal.config.amqp.properties.RabbitProperties;
//import ru.tpu.hostel.internal.config.amqp.tracing.TracedConnectionFactory;
//import ru.tpu.hostel.internal.config.amqp.tracing.interceptor.AmqpMessageReceiveInterceptor;
//import ru.tpu.hostel.internal.external.amqp.Microservice;
//
//import java.util.Arrays;
//import java.util.EnumMap;
//import java.util.HashMap;
//import java.util.Map;
//import java.util.Set;
//import java.util.stream.Collectors;
//
//@Slf4j
//@Component
//@RequiredArgsConstructor
//public class RabbitListenerBeanFactoryPostProcessor implements SmartInitializingSingleton {
//
//    private static final String LISTENER_POSTFIX = "RabbitListener";
//
//    private static final String LISTENERS_BEAN_NAMES_BEAN_NAME = "rabbitListenersBeanNames";
//
//    private final ConfigurableListableBeanFactory beanFactory;
//
//    private final Map<Microservice, Map<String, TracedConnectionFactory>> connectionFactories;
//
//    private final RabbitProperties rabbitProperties;
//
//    private final OpenTelemetry openTelemetry;
//
//    private final ApplicationContext applicationContext;
//
//    @Override
//    public void afterSingletonsInstantiated() {
//        Map<Microservice, Map<String, Map<String, String>>> listenersBeanNames = new EnumMap<>(Microservice.class);
//        connectionFactories.forEach((microservice, innerMap) -> {
//            Map<String, Map<String, String>> serviceMap = new HashMap<>();
//            innerMap.forEach((name, connectionFactory) -> {
//                Map<String, String> listenerToBeanNameMap = new HashMap<>();
//
//                Set<String> listenerNames = rabbitProperties.properties()
//                        .get(microservice)
//                        .get(name)
//                        .queueingProperties()
//                        .listeners()
//                        .keySet()
//                        .stream()
//                        .map(s -> Arrays.stream(s.split(" "))
//                                .map(s1 -> StringUtils.capitalize(s1.trim()))
//                                .collect(Collectors.joining())
//                        )
//                        .collect(Collectors.toSet());
//                listenerNames.forEach(listenerName -> {
//                    String beanName = microservice.name().toLowerCase()
//                            + StringUtils.capitalize(name)
//                            + listenerName
//                            + LISTENER_POSTFIX;
//
//                    listenerToBeanNameMap.put(listenerName, beanName);
//
//                    SimpleRabbitListenerContainerFactory listenerFactory = createListenerContainerFactory(
//                            connectionFactory,
//                            getCustomizer(
//                                    rabbitProperties.properties()
//                                            .get(microservice)
//                                            .get(name)
//                                            .queueingProperties()
//                                            .listeners()
//                                            .get(listenerName)
//                                            .customizerName(),
//                                    applicationContext
//                            )
//                    );
//
//                    log.debug("Create listener bean: {}", beanName);
//                    beanFactory.registerSingleton(beanName, listenerFactory);
//                });
//                serviceMap.put(name, listenerToBeanNameMap);
//            });
//            listenersBeanNames.put(microservice, serviceMap);
//        });
//
//        beanFactory.registerSingleton(LISTENERS_BEAN_NAMES_BEAN_NAME, listenersBeanNames);
//    }
//
//    private SimpleRabbitListenerContainerFactory createListenerContainerFactory(
//            TracedConnectionFactory connectionFactory,
//            SimpleRabbitListenerContainerFactoryCustomizer customizer
//    ) {
//        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
//        if (customizer != null) {
//            customizer.customize(factory);
//        }
//        factory.setAcknowledgeMode(AcknowledgeMode.AUTO);
//        factory.setDefaultRequeueRejected(false);
//        factory.setConnectionFactory(connectionFactory);
//        factory.setAdviceChain(new AmqpMessageReceiveInterceptor(openTelemetry));
//
//        return factory;
//    }
//
//    private SimpleRabbitListenerContainerFactoryCustomizer getCustomizer(
//            String beanName,
//            ApplicationContext applicationContext
//    ) {
//        if (!StringUtils.hasText(beanName)) {
//            return null;
//        }
//
//        try {
//            return applicationContext.getBean(beanName, SimpleRabbitListenerContainerFactoryCustomizer.class);
//        } catch (NoSuchBeanDefinitionException e) {
//            throw new IllegalStateException(
//                    String.format("""
//                                    Не найден бин кастомайзера '%s'.
//
//                                    Создайте бин с этим именем:
//
//                                    @Bean("%s")
//                                    public SimpleRabbitListenerContainerFactoryCustomizer %s() {
//                                        return factory -> {
//                                            // настройка объекта
//                                        };
//                                    }
//                                    """,
//                            beanName,
//                            beanName,
//                            beanName
//                    ),
//                    e
//            );
//        }
//    }
//
//}
