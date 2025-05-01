package ru.tpu.hostel.internal.common.logging;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

import static ru.tpu.hostel.internal.common.logging.Message.FINISH_RABBIT_RECEIVING_RPC;
import static ru.tpu.hostel.internal.common.logging.Message.FINISH_RABBIT_SENDING_METHOD_EXECUTION;
import static ru.tpu.hostel.internal.common.logging.Message.FINISH_RABBIT_SENDING_METHOD_VIA_RPC_EXECUTION_WITH_EMPTY_RESPONSE;
import static ru.tpu.hostel.internal.common.logging.Message.RABBIT_RECEIVING_RPC_EXCEPTION;
import static ru.tpu.hostel.internal.common.logging.Message.RABBIT_SENDING_METHOD_EXECUTION_EXCEPTION;
import static ru.tpu.hostel.internal.common.logging.Message.START_RABBIT_SENDING_METHOD_EXECUTION;
import static ru.tpu.hostel.internal.common.logging.Message.START_RABBIT_SENDING_METHOD_VIA_ROUTING_KEY_EXECUTION;
import static ru.tpu.hostel.internal.common.logging.Message.START_RABBIT_SENDING_METHOD_VIA_RPC_EXECUTION;
import static ru.tpu.hostel.internal.utils.TimeUtil.getLocalDateTimeStingFromMillis;

/**
 * Аспект для логирования сендеров RabbitMQ
 */
@Aspect
@Component
@Slf4j
public class AmqpMessageSenderLoggingFilter {

    private static final ObjectWriter WRITER = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .disable(SerializationFeature.WRITE_DATE_TIMESTAMPS_AS_NANOSECONDS)
            .disable(DeserializationFeature.READ_DATE_TIMESTAMPS_AS_NANOSECONDS)
            .enable(SerializationFeature.INDENT_OUTPUT)
            .writer();

    @Around("execution(* ru.tpu.hostel..external.amqp.AmqpMessageSender.send(..))")
    public Object logSendMessage(ProceedingJoinPoint joinPoint) throws Throwable {
        Object[] args = joinPoint.getArgs();
        String messageId = (String) args[0];
        Object messagePayload = args[1];
        String payloadJson = safeMapToJson(messagePayload);

        if (args.length == 3) {
           String routingKey = (String) args[2];
            log.info(START_RABBIT_SENDING_METHOD_VIA_ROUTING_KEY_EXECUTION, routingKey, messageId, payloadJson);
        } else {
            log.info(START_RABBIT_SENDING_METHOD_EXECUTION, messageId, payloadJson);
        }

        long startTime = System.currentTimeMillis();

        try {
            Object result = joinPoint.proceed();
            long endTime = System.currentTimeMillis() - startTime;
            log.info(
                    FINISH_RABBIT_SENDING_METHOD_EXECUTION,
                    messageId,
                    payloadJson,
                    endTime
            );
            return result;
        } catch (Throwable throwable) {
            long endTime = System.currentTimeMillis() - startTime;
            log.error(
                    RABBIT_SENDING_METHOD_EXECUTION_EXCEPTION,
                    messageId,
                    payloadJson,
                    throwable.getMessage(),
                    getLocalDateTimeStingFromMillis(startTime),
                    endTime,
                    throwable
            );
            throw throwable;
        }
    }

    @Around("execution(* ru.tpu.hostel..external.amqp.AmqpMessageSender.sendAndReceive(..))")
    public Object logSendAndReceive(ProceedingJoinPoint joinPoint) throws Throwable {
        Object[] args = joinPoint.getArgs();
        String messageId = (String) args[0];
        Object messagePayload = args[1];

        String payloadJson = safeMapToJson(messagePayload);
        log.info(START_RABBIT_SENDING_METHOD_VIA_RPC_EXECUTION, messageId, payloadJson);

        long startTime = System.currentTimeMillis();

        try {
            Object result = joinPoint.proceed();
            long endTime = System.currentTimeMillis() - startTime;

            if (result instanceof org.springframework.amqp.core.Message replyMessage) {
                String replyMessageId = replyMessage.getMessageProperties().getMessageId();
                String responseBody = replyMessage.getBody() != null
                        ? new String(replyMessage.getBody())
                        : "<пусто>";
                log.info(
                        FINISH_RABBIT_RECEIVING_RPC,
                        replyMessageId,
                        responseBody,
                        endTime
                );
            } else {
                log.warn(
                        FINISH_RABBIT_SENDING_METHOD_VIA_RPC_EXECUTION_WITH_EMPTY_RESPONSE,
                        messageId,
                        payloadJson,
                        getLocalDateTimeStingFromMillis(startTime),
                        endTime
                );
            }

            return result;
        } catch (Throwable throwable) {
            long endTime = System.currentTimeMillis() - startTime;
            log.error(
                    RABBIT_RECEIVING_RPC_EXCEPTION,
                    messageId,
                    payloadJson,
                    throwable.getMessage(),
                    getLocalDateTimeStingFromMillis(startTime),
                    endTime,
                    throwable
            );
            throw throwable;
        }
    }

    private String safeMapToJson(Object obj) {
        try {
            return WRITER.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            return "<ошибка сериализации>";
        }
    }
}

