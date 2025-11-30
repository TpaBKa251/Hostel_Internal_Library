package ru.tpu.hostel.internal;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Import;
import ru.tpu.hostel.internal.common.exception.FeignExceptionHandler;
import ru.tpu.hostel.internal.common.logging.FeignClientLoggingFilter;
import ru.tpu.hostel.internal.config.rest.HttpFeignInterceptor;
import ru.tpu.hostel.internal.config.rest.TracingFeignAspect;

@AutoConfiguration
@Import({
        FeignExceptionHandler.class,
        FeignClientLoggingFilter.class,
        HttpFeignInterceptor.class,
        TracingFeignAspect.class
})
public class HostelFeignAutoConfiguration {
}
