package ru.tpu.hostel.internal.common.exception;

import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.validation.method.MethodValidationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import ru.tpu.hostel.internal.exception.ServiceException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Глобальный обработчик исключений.
 * <p>Обрабатывает {@link ServiceException}, {@link DataIntegrityViolationException} и
 * {@link ConstraintViolationException}, {@link MethodValidationException}, {@link MethodArgumentNotValidException},
 * а также любые другие в общем виде.
 * <p>💡При необходимости явно обрабатывать какое-то
 * исключение, создавать свой ExceptionHandler
 *
 * @author Лапшин Илья
 * @version 1.1.0
 * @since 1.0.0
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
        String message = ex.getRootCause() != null && ex.getRootCause().getMessage() != null
                ? ex.getRootCause().getMessage()
                : "Нарушение целостности данных. Проверьте введённые значения.";

        return getResponseEntity(HttpStatus.CONFLICT, message);
    }

    @ExceptionHandler({ConstraintViolationException.class, MethodValidationException.class})
    public ResponseEntity<Map<String, String>> handle400ValidationException(Exception ex) {
        Map<String, List<String>> errors = new HashMap<>();

        if (ex instanceof ConstraintViolationException cve) {
            cve.getConstraintViolations().forEach(violation -> {
                String field = violation.getPropertyPath().toString();
                String message = violation.getMessage();
                errors.computeIfAbsent(field, ignore -> new ArrayList<>()).add(message);
            });
        } else if (ex instanceof MethodValidationException mve) {
            mve.getAllValidationResults().forEach(vr -> {
                String field = vr.getMethodParameter().getParameterName();
                String message = vr.getResolvableErrors().getFirst().getDefaultMessage();
                errors.computeIfAbsent(field, ignore -> new ArrayList<>()).add(message);
            });
        }

        return getResponseEntity(HttpStatus.BAD_REQUEST, errors.toString());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleMethodArgumentNotValidException(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });

        return getResponseEntity(HttpStatus.BAD_REQUEST, errors.toString());
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
