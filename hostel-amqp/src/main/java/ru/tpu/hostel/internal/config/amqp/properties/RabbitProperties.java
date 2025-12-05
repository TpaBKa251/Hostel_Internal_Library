package ru.tpu.hostel.internal.config.amqp.properties;

import jakarta.validation.Valid;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;
import ru.tpu.hostel.internal.external.amqp.Microservice;

import java.util.Map;

@ConfigurationProperties(prefix = "rabbitmq")
@Validated
public record RabbitProperties(

        @Valid
        Map<Microservice, Map<String, RabbitServiceProperties>> properties

) {
}
