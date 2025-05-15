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
import org.springframework.amqp.core.MessageProperties;
import org.springframework.stereotype.Component;
import ru.tpu.hostel.internal.exception.ServiceException;
import ru.tpu.hostel.internal.external.amqp.AmqpMessageSender;
import ru.tpu.hostel.internal.external.amqp.Microservice;

import static ru.tpu.hostel.internal.common.logging.Message.FINISH_RABBIT_RECEIVING_RPC;
import static ru.tpu.hostel.internal.common.logging.Message.FINISH_RABBIT_SENDING_METHOD_EXECUTION;
import static ru.tpu.hostel.internal.common.logging.Message.RABBIT_SENDING_OR_RECEIVING_EXCEPTION;
import static ru.tpu.hostel.internal.common.logging.Message.START_RABBIT_SENDING_METHOD_EXECUTION;
import static ru.tpu.hostel.internal.common.logging.Message.START_RABBIT_SENDING_METHOD_VIA_ROUTING_KEY_AND_EXCHANGE_EXECUTION;
import static ru.tpu.hostel.internal.common.logging.Message.START_RABBIT_SENDING_METHOD_VIA_ROUTING_KEY_EXECUTION;
import static ru.tpu.hostel.internal.utils.TimeUtil.getLocalDateTimeStingFromMillis;

/**
 * Аспект для логирования сендеров RabbitMQ. Логирует методы всех наследников {@link AmqpMessageSender}
 *
 * @author Илья Лапшин
 * @version 1.1.0
 * @since 1.1.0
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
        String messageId = (String) args[args.length - 2];
        logStartSend(args, messageId, 3);
        return executeSendMethod(joinPoint, messageId);
    }

    @Around("execution(* ru.tpu.hostel..external.amqp.AmqpMessageSender.sendAndReceive(..))")
    public Object logSendAndReceiveMessage(ProceedingJoinPoint joinPoint) throws Throwable {
        Object[] args = joinPoint.getArgs();
        String messageId = (String) args[args.length - 3];

        logStartSend(args, messageId, 4);
        long startTime = System.currentTimeMillis();

        try {
            Object result = joinPoint.proceed();
            long endTime = System.currentTimeMillis() - startTime;
            log.info(
                    FINISH_RABBIT_RECEIVING_RPC,
                    messageId,
                    result,
                    endTime
            );
            return result;
        } catch (Throwable throwable) {
            long endTime = System.currentTimeMillis() - startTime;
            logException(messageId, throwable, startTime, endTime);
            throw throwable;
        }
    }

    @Around("execution(* ru.tpu.hostel..external.amqp.AmqpMessageSender.sendReply(..))")
    public Object logSendReplyMessage(ProceedingJoinPoint joinPoint) throws Throwable {
        Object[] args = joinPoint.getArgs();
        MessageProperties properties = (MessageProperties) args[1];
        logStartSend(args, properties.getMessageId(), 3);
        return executeSendMethod(joinPoint, properties.getMessageId());
    }

    private Object executeSendMethod(ProceedingJoinPoint joinPoint, String messageId) throws Throwable {
        long startTime = System.currentTimeMillis();
        try {
            Object result = joinPoint.proceed();
            long endTime = System.currentTimeMillis() - startTime;
            log.info(
                    FINISH_RABBIT_SENDING_METHOD_EXECUTION,
                    messageId,
                    endTime
            );
            return result;
        } catch (Throwable throwable) {
            long endTime = System.currentTimeMillis() - startTime;
            logException(messageId, throwable, startTime, endTime);
            throw throwable;
        }
    }

    private void logStartSend(Object[] args, String messageId, int minArgs) {
        if (args.length == minArgs) {
            Enum<?> messageType = (Enum<?>) args[0];
            String messagePayload = safeMapToJson(args[2]);
            log.info(START_RABBIT_SENDING_METHOD_EXECUTION, messageType, messageId, messagePayload);
        } else if (args.length == minArgs + 1) {
            Microservice microservice = (Microservice) args[0];
            String routingKey = (String) args[1];
            String messagePayload = safeMapToJson(args[3]);
            log.info(
                    START_RABBIT_SENDING_METHOD_VIA_ROUTING_KEY_EXECUTION,
                    microservice,
                    routingKey,
                    messageId,
                    messagePayload
            );
        } else {
            Microservice microservice = (Microservice) args[0];
            String exchange = (String) args[1];
            String routingKey = (String) args[2];
            String messagePayload = safeMapToJson(args[4]);
            log.info(
                    START_RABBIT_SENDING_METHOD_VIA_ROUTING_KEY_AND_EXCHANGE_EXECUTION,
                    microservice,
                    exchange,
                    routingKey,
                    messageId,
                    messagePayload
            );
        }
    }

    private void logException(String messageId, Throwable throwable, long startTime, long endTime) {
        if (throwable instanceof ServiceException serviceException) {
            log.error(
                    RABBIT_SENDING_OR_RECEIVING_EXCEPTION,
                    messageId,
                    serviceException.getMessage(),
                    getLocalDateTimeStingFromMillis(startTime),
                    endTime
            );
        } else {
            log.error(
                    RABBIT_SENDING_OR_RECEIVING_EXCEPTION,
                    messageId,
                    throwable.getMessage(),
                    getLocalDateTimeStingFromMillis(startTime),
                    endTime,
                    throwable
            );
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

