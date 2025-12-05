package ru.tpu.hostel.internal.config.amqp.util;

import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import ru.tpu.hostel.internal.external.amqp.Microservice;

import java.util.Map;

/**
 * Класс для получения имени бина фабрики RabbitMQ слушателя.
 */
@Component
@RequiredArgsConstructor
public class RabbitListenerContainerFactoryNameResolver {

    private final Map<Microservice, Map<String, Map<String, String>>> listenersBeanNames;

    /**
     * Возвращает имя бина по параметрам.
     *
     * @param microservice          енам микросервиса, из которого слушаем.
     * @param servicePropertiesName имя сервисных свойств (комплекта свойств).
     * @param listenerName          имя слушателя.
     * @return имя бина фабрики слушателя. Если не найдено, то null.
     */
    @Nullable
    public String resolveListenerContainerFactoryName(
            @NotNull Microservice microservice,
            @NotNull String servicePropertiesName,
            @NotNull String listenerName
    ) {
        if (!StringUtils.hasText(servicePropertiesName) || !StringUtils.hasText(listenerName)) {
            return null;
        }
        return listenersBeanNames.get(microservice)
                .get(servicePropertiesName)
                .get(listenerName);
    }

    /**
     * Возвращает имя бина по параметрам.
     *
     * @param microserviceStr       имя микросервиса, из которого слушаем (в любом формате: user, USER, user-service).
     * @param servicePropertiesName имя сервисных свойств (комплекта свойств).
     * @param listenerName          имя слушателя.
     * @return имя бина фабрики слушателя. Если не найдено, то null.
     */
    @Nullable
    public String resolveListenerContainerFactoryName(
            @NotNull String microserviceStr,
            @NotNull String servicePropertiesName,
            @NotNull String listenerName
    ) {
        if (!StringUtils.hasText(microserviceStr)
                || !StringUtils.hasText(servicePropertiesName)
                || !StringUtils.hasText(listenerName)) {
            return null;
        }
        return listenersBeanNames.get(Microservice.fromServiceName(microserviceStr))
                .get(servicePropertiesName)
                .get(listenerName);
    }

}
