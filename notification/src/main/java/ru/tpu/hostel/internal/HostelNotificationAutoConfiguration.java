package ru.tpu.hostel.internal;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Import;
import ru.tpu.hostel.internal.builder.impl.DefaultNotificationRequestBuilder;
import ru.tpu.hostel.internal.service.impl.DefaultNotificationSender;

@AutoConfiguration
@Import({
        DefaultNotificationRequestBuilder.class,
        DefaultNotificationSender.class
        //RabbitNotificationServiceConfig.class
})
public class HostelNotificationAutoConfiguration {
}
