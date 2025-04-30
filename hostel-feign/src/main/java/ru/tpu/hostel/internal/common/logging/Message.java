package ru.tpu.hostel.internal.common.logging;

import lombok.experimental.UtilityClass;

/**
 * Сообщения для логов Feign клиентов
 *
 * @author Лапшин Илья
 * @version 1.0.0
 * @see FeignClientLoggingFilter
 * @since 1.0.0
 */
@UtilityClass
class Message {

    static final String START_FEIGN_SENDING_REQUEST = "[FEIGN] Отправляется запрос в {}: {} {}";

    static final String START_FEIGN_SENDING_REQUEST_WITH_PARAMS
            = "[FEIGN] Отправляется запрос в {}: {} {}, параметры: {}";

    static final String FEIGN_RECEIVING_RESPONSE_WITHOUT_RESULT
            = "[FEIGN] Запрос выполнен. Время выполнения: {} мс";

    static final String FEIGN_RECEIVING_RESPONSE
            = "[FEIGN] Запрос выполнен. Статус: {}, ответ: {}, время выполнения: {} мс";

    static final String FEIGN_SENDING_REQUEST_WITH_EXCEPTION
            = "[FEIGN] Ошибка запроса: {}. Ошибка: {}, время старта: {}, время выполнения: {} мс";

}
