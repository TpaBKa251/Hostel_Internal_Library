package ru.tpu.hostel.internal.common.logging;

import lombok.experimental.UtilityClass;

/**
 * Сообщения для логов
 */
@UtilityClass
class Message {

    static final String START_RABBIT_SENDING_METHOD_EXECUTION = "[RABBIT] Отправка сообщения: messageId={}, payload={}";

    static final String START_RABBIT_SENDING_METHOD_VIA_ROUTING_KEY_EXECUTION
            = "[RABBIT] Отправка сообщения по ключу {}: messageId={}, payload={}";

    static final String START_RABBIT_SENDING_METHOD_VIA_RPC_EXECUTION
            = "[RABBIT] Отправка RPC сообщения: messageId={}, payload={}";

    static final String FINISH_RABBIT_SENDING_METHOD_EXECUTION
            = "[RABBIT] Сообщение отправлено: messageId={}, playload={}. Время выполнения {} мс";

    static final String FINISH_RABBIT_RECEIVING_RPC
            = "[RABBIT] Получен RPC ответ: messageId={}, playload={}. Время выполнения {} мс";

    static final String RABBIT_SENDING_METHOD_EXECUTION_EXCEPTION = "[RABBIT] Ошибка отправки сообщения: "
            + "messageId={}, payload={}. Ошибка: {}, время старта: {}, время выполнения: {} мс";

    static final String RABBIT_RECEIVING_RPC_EXCEPTION = "[RABBIT] Ошибка получения ответа на сообщение: "
            + "messageId={}, payload={}. Ошибка: {}, время старта: {}, время выполнения: {} мс";

    static final String FINISH_RABBIT_SENDING_METHOD_VIA_RPC_EXECUTION_WITH_EMPTY_RESPONSE = "[RABBIT] Пустой ответ "
            + "на сообщение: messageId={}, payload={}, время старта: {}, время выполнения: {} мс";

}
