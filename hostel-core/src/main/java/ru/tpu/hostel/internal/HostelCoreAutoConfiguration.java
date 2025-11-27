package ru.tpu.hostel.internal;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Import;
import ru.tpu.hostel.internal.common.exception.GlobalExceptionHandler;
import ru.tpu.hostel.internal.common.logging.RepositoryLoggingFilter;
import ru.tpu.hostel.internal.common.logging.ServiceLoggingFilter;
import ru.tpu.hostel.internal.config.otlp.OpenTelemetryConfig;
import ru.tpu.hostel.internal.config.rest.HttpRestInterceptor;

@AutoConfiguration
@Import({
        GlobalExceptionHandler.class,
        RepositoryLoggingFilter.class,
        ServiceLoggingFilter.class,
        OpenTelemetryConfig.class,
        HttpRestInterceptor.class
})
public class HostelCoreAutoConfiguration {
}
