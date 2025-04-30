package ru.tpu.hostel.internal.common.logging;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;

import static ru.tpu.hostel.internal.common.logging.Message.FINISH_CONTROLLER_METHOD_EXECUTION;

/**
 * Класс-фильтр для логирования ответа клиенту. Логирует завершение выполнения запросов
 *
 * @author Илья Лапшин
 * @version 1.0.0
 * @since 1.0.0
 */
@Component
@Slf4j
public class ResponseLoggingFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        if (!(response instanceof HttpServletResponse res)) {
            chain.doFilter(request, response);
            return;
        }

        long startTime = System.currentTimeMillis();
        chain.doFilter(request, response);
        long duration = System.currentTimeMillis() - startTime;

        logResponse(res, duration);
    }

    private void logResponse(HttpServletResponse response, long duration) {
        int status = response.getStatus();
        log.info(FINISH_CONTROLLER_METHOD_EXECUTION, status, duration);
    }
}

