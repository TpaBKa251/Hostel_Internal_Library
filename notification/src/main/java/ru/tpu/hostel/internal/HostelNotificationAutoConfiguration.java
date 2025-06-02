package ru.tpu.hostel.internal;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Import;
import ru.tpu.hostel.internal.builder.impl.DefaultNotificationRequestBuilder;
import ru.tpu.hostel.internal.config.amqp.RabbitNotificationQueueingPropertiesSend;
import ru.tpu.hostel.internal.config.amqp.RabbitNotificationServiceConfig;
import ru.tpu.hostel.internal.config.amqp.RabbitNotificationServiceProperties;
import ru.tpu.hostel.internal.service.impl.DefaultNotificationSender;

@AutoConfiguration
@Import({
        DefaultNotificationRequestBuilder.class,
        DefaultNotificationSender.class,
        RabbitNotificationServiceProperties.class,
        RabbitNotificationQueueingPropertiesSend.class,
        RabbitNotificationServiceConfig.class
})
public class HostelNotificationAutoConfiguration {
}
