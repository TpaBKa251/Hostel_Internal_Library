package ru.tpu.hostel.internal.common.logging;

import lombok.experimental.UtilityClass;

/**
 * Сообщения для логов
 */
@UtilityClass
class Message {

    static final String START_RABBIT_SENDING_METHOD_EXECUTION
            = "[RABBIT] Отправка {} сообщения: messageId={}, payload={}";

    static final String START_RABBIT_SENDING_METHOD_VIA_ROUTING_KEY_EXECUTION
            = "[RABBIT] Отправка сообщения в микросервис {} по ключу {}: messageId={}, payload={}";

    static final String START_RABBIT_SENDING_METHOD_VIA_ROUTING_KEY_AND_EXCHANGE_EXECUTION
            = "[RABBIT] Отправка сообщения в микросервис {} через обменник {} по ключу {}: messageId={}, payload={}";

    static final String FINISH_RABBIT_SENDING_METHOD_EXECUTION
            = "[RABBIT] Сообщение отправлено: messageId={}. Время выполнения {} мс";

    static final String FINISH_RABBIT_RECEIVING_RPC
            = "[RABBIT] Получен RPC ответ: messageId={}, playload={}. Время выполнения {} мс";

    static final String RABBIT_SENDING_OR_RECEIVING_EXCEPTION = "[RABBIT] Ошибка отправки или получения ответа на "
            + "сообщение: messageId={}. Ошибка: {}, время старта: {}, время выполнения: {} мс";

}
