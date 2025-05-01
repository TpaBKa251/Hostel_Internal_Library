package ru.tpu.hostel.internal.common.exception;

import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import ru.tpu.hostel.internal.exception.ServiceException;

import java.util.HashMap;
import java.util.Map;

/**
 * Глобальный обработчик исключений.
 * <p>Обрабатывает {@link ServiceException}, {@link DataIntegrityViolationException} и
 * {@link ConstraintViolationException}, а также любые другие в общем виде.
 * <p>💡При необходимости явно обрабатывать какое-то
 * исключение, создавать свой ExceptionHandler
 */
@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ServiceException.class)
    public ResponseEntity<Map<String, String>> handleServiceException(ServiceException ex) {
        if (ex.getStatus().value() >= 500 && ex.getStatus().value() < 600) {
            log.error(ex.getMessage(), ex);
        }
        if (ex.getCause() != null) {
            return getResponseEntity(ex.getStatus(), ex.getCause().getMessage());
        }
        return getResponseEntity(ex.getStatus(), ex.getMessage());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, String>> handleDataIntegrityViolationException(
            DataIntegrityViolationException ex
    ) {
        return getResponseEntity(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Map<String, String>> handleConstraintViolationException(ConstraintViolationException ex) {
        return getResponseEntity(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleException(Exception ex) {
        log.error(ex.getMessage(), ex);
        return getResponseEntity(HttpStatus.INTERNAL_SERVER_ERROR, ex.getMessage());
    }

    private ResponseEntity<Map<String, String>> getResponseEntity(HttpStatus status, String message) {
        Map<String, String> map = new HashMap<>();
        map.put("code", String.valueOf(status.value()));
        map.put("message", message);

        return new ResponseEntity<>(map, status);
    }

}
