package ru.tpu.hostel.internal.common.exception;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.util.HashMap;
import java.util.Map;

/**
 * Глобальный обработчик исключений. Обрабатывает {@link FeignException}.
 * <p>💡При необходимости явно обрабатывать какое-то исключение, создавать свой ExceptionHandler
 *
 * @author Лапшин Илья
 * @version 1.0.3
 * @since 1.0.0
 */
@ControllerAdvice
@Slf4j
@RequiredArgsConstructor
public class FeignExceptionHandler {

    private final ObjectMapper mapper;

    @ExceptionHandler(FeignException.class)
    public ResponseEntity<Map<String, String>> handleFeignException(FeignException ex) {
        if (ex.status() >= 500) {
            log.error(ex.contentUTF8(), ex);
        }
        if (ex.status() >= 200 && ex.status() < 600) {
            return getResponseEntity(HttpStatus.valueOf(ex.status()), mapHttpResponseErrorMessage(ex.contentUTF8()));
        }

        return getResponseEntity(HttpStatus.SERVICE_UNAVAILABLE, ex.getMessage());
    }

    private ResponseEntity<Map<String, String>> getResponseEntity(HttpStatus status, String message) {
        Map<String, String> map = new HashMap<>();
        map.put("code", String.valueOf(status.value()));
        map.put("message", message);

        return new ResponseEntity<>(map, status);
    }

    private String mapHttpResponseErrorMessage(String message) {
        try {
            JsonNode json = mapper.readTree(message);
            return json.get("message").asText();
        } catch (Exception e) {
            return message;
        }
    }

}
