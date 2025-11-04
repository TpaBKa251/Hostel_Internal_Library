package ru.tpu.hostel.internal.common.logging;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import ru.tpu.hostel.internal.utils.LogFilter;
import ru.tpu.hostel.internal.utils.SecretArgument;
import ru.tpu.hostel.internal.utils.TimeUtil;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

import static ru.tpu.hostel.internal.common.logging.Message.FEIGN_RECEIVING_RESPONSE;
import static ru.tpu.hostel.internal.common.logging.Message.FEIGN_RECEIVING_RESPONSE_WITHOUT_RESULT;
import static ru.tpu.hostel.internal.common.logging.Message.FEIGN_SENDING_REQUEST_WITH_EXCEPTION;
import static ru.tpu.hostel.internal.common.logging.Message.START_FEIGN_SENDING_REQUEST;
import static ru.tpu.hostel.internal.common.logging.Message.START_FEIGN_SENDING_REQUEST_WITH_PARAMS;

/**
 * Аспект для логирования методов Feign клиентов (классов с аннотацией {@link FeignClient}). Логирует старт выполнения
 * запросов. Есть поддержка {@link LogFilter} и {@link ru.tpu.hostel.internal.utils.SecretArgument}
 *
 * @author Илья Лапшин
 * @version 1.3.6
 * @since 1.0.0
 */
@Slf4j
@Aspect
@Component
public class FeignClientLoggingFilter {

    private static final String UNKNOWN_HTTP_METHOD = "UNKNOWN";

    @Around("within(@org.springframework.cloud.openfeign.FeignClient *)")
    public Object logFeignClientRequest(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();

        LogFilter logFilter = getLogFilter(method);

        boolean methodLogging = logFilter == null || logFilter.enableMethodLogging();
        boolean paramsLogging = logFilter == null || logFilter.enableParamsLogging();
        boolean resultLogging = logFilter == null || logFilter.enableResultLogging();

        FeignClient feignClient = method.getDeclaringClass().getAnnotation(FeignClient.class);
        String serviceName = feignClient != null ? feignClient.name() : "";

        RequestMapping requestMapping = method.getAnnotation(RequestMapping.class);
        String httpMethod;
        String[] paths;

        if (requestMapping == null) {
            httpMethod = resolveHttpMethod(method);
            paths = resolvePath(method);
        } else {
            httpMethod = requestMapping.method().length > 0
                    ? requestMapping.method()[0].name()
                    : UNKNOWN_HTTP_METHOD;
            paths = requestMapping.value();
        }

        String fullPath = paths.length > 0 ? paths[0] : "";

        Parameter[] parameters = method.getParameters();
        Map<String, Object> paramsMap = new LinkedHashMap<>();
        for (int i = 0; i < parameters.length; i++) {
            if (parameters[i].getAnnotation(SecretArgument.class) != null) {
                continue;
            }
            String previousFullPath = fullPath;
            fullPath = fullPath.replace("{" + parameters[i].getName() + "}", joinPoint.getArgs()[i].toString());

            if (previousFullPath.equals(fullPath)) {
                paramsMap.put(parameters[i].getName(), joinPoint.getArgs()[i]);
            }
        }

        String args = paramsMap.entrySet()
                .stream()
                .map(entry -> entry.getKey() + " = " + entry.getValue())
                .collect(Collectors.joining(", "));

        logRequest(serviceName, httpMethod, fullPath, args, methodLogging, paramsLogging);

        long startTime = System.currentTimeMillis();
        try {
            Object response = joinPoint.proceed();
            long executionTime = System.currentTimeMillis() - startTime;
            logResponse(response, executionTime, methodLogging, resultLogging);
            return response;
        } catch (Throwable throwable) {
            long executionTime = System.currentTimeMillis() - startTime;
            log.error(
                    FEIGN_SENDING_REQUEST_WITH_EXCEPTION,
                    fullPath,
                    throwable.getMessage(),
                    TimeUtil.getLocalDateTimeStingFromMillis(startTime),
                    executionTime
            );
            throw throwable;
        }
    }

    private LogFilter getLogFilter(Method method) {
        LogFilter annotation = AnnotationUtils.findAnnotation(method, LogFilter.class);
        return annotation == null
                ? AnnotationUtils.findAnnotation(method.getDeclaringClass(), LogFilter.class)
                : annotation;
    }

    private String resolveHttpMethod(Method method) {
        if (method.isAnnotationPresent(GetMapping.class)) return "GET";
        if (method.isAnnotationPresent(PostMapping.class)) return "POST";
        if (method.isAnnotationPresent(PutMapping.class)) return "PUT";
        if (method.isAnnotationPresent(DeleteMapping.class)) return "DELETE";
        if (method.isAnnotationPresent(PatchMapping.class)) return "PATCH";
        return UNKNOWN_HTTP_METHOD;
    }

    private String[] resolvePath(Method method) {
        if (method.isAnnotationPresent(GetMapping.class))
            return method.getAnnotation(GetMapping.class).value();
        if (method.isAnnotationPresent(PostMapping.class))
            return method.getAnnotation(PostMapping.class).value();
        if (method.isAnnotationPresent(PutMapping.class))
            return method.getAnnotation(PutMapping.class).value();
        if (method.isAnnotationPresent(DeleteMapping.class))
            return method.getAnnotation(DeleteMapping.class).value();
        if (method.isAnnotationPresent(PatchMapping.class))
            return method.getAnnotation(PatchMapping.class).value();
        return new String[0];
    }

    private void logRequest(
            String serviceName,
            String httpMethod,
            String fullPath,
            String args,
            boolean methodLogging,
            boolean paramsLogging
    ) {
        if (methodLogging) {
            if (args.isEmpty() || !paramsLogging) {
                log.info(
                        START_FEIGN_SENDING_REQUEST,
                        serviceName,
                        httpMethod,
                        fullPath
                );
            } else {
                log.info(
                        START_FEIGN_SENDING_REQUEST_WITH_PARAMS,
                        serviceName,
                        httpMethod,
                        fullPath,
                        args
                );
            }
        }
    }

    private void logResponse(
            Object response,
            long executionTime,
            boolean methodLogging,
            boolean resultLogging
    ) {
        if (methodLogging && resultLogging) {
            if (response instanceof ResponseEntity<?> responseEntity) {
                log.info(
                        FEIGN_RECEIVING_RESPONSE,
                        responseEntity.getStatusCode(),
                        response,
                        executionTime
                );
            } else {
                log.info(FEIGN_RECEIVING_RESPONSE_WITHOUT_RESULT, executionTime);
            }
        } else if (methodLogging) {
            log.info(FEIGN_RECEIVING_RESPONSE_WITHOUT_RESULT, executionTime);
        }
    }

}
