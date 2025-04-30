package ru.tpu.hostel.internal.common.logging;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import static ru.tpu.hostel.internal.common.logging.Message.START_CONTROLLER_METHOD_EXECUTION;

/**
 * Аспект для логирования методов контроллеров (запросов от клиента) классов, в названии которых есть
 * <b><i>Controller</i></b>, в пакете <b><i>controller</i></b>.
 * Логирует старт выполнения запросов.
 *
 * @author Илья Лапшин
 * @version 1.0.0
 * @since 1.0.0
 */
@Aspect
@Component
@Slf4j
@Order(0)
public class RequestLoggingFilter {

    @Before("execution(public * ru.tpu.hostel..controller.*Controller*.*(..))")
    public void logControllerMethod() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        HttpServletRequest request = attributes != null ? attributes.getRequest() : null;
        if (request != null) {
            logRequest(request);
        }
    }

    private void logRequest(HttpServletRequest request) {
        String method = request.getMethod();
        String requestURI = request.getRequestURI();

        log.info(START_CONTROLLER_METHOD_EXECUTION, method, requestURI);
    }

}

