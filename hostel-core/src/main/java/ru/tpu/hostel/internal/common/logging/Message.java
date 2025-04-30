package ru.tpu.hostel.internal.common.logging;

import lombok.experimental.UtilityClass;

/**
 * Сообщения для логов сервисного, репозиторного слоев и контроллеров
 *
 * @author Лапшин Илья
 * @version 1.0.0
 * @see ServiceLoggingFilter
 * @see RepositoryLoggingFilter
 * @see RequestLoggingFilter
 * @see ResponseLoggingFilter
 * @since 1.0.0
 */
@UtilityClass
class Message {

    static final String START_REPOSITORY_METHOD_EXECUTION
            = "[REPOSITORY] Выполняется репозиторный метод {}.{}()";

    static final String FINISH_REPOSITORY_METHOD_EXECUTION
            = "[REPOSITORY] Завершилось выполнение репозиторного метода {}.{}(). Время выполнения: {} мс";

    static final String REPOSITORY_METHOD_EXECUTION_EXCEPTION
            = "[REPOSITORY] Ошибка во время выполнения репозиторного метода {}.{}(). "
            + "Ошибка: {}, время старта: {}, время выполнения: {} мс";

    static final String START_SERVICE_METHOD_EXECUTION = "[SERVICE] Выполняется сервисный метод {}.{}()";

    static final String START_SERVICE_METHOD_EXECUTION_WITH_PARAMETERS
            = "[SERVICE] Выполняется сервисный метод {}.{}({})";

    static final String FINISH_SERVICE_METHOD_EXECUTION
            = "[SERVICE] Завершилось выполнение сервисного метода {}.{}(). Время выполнения: {} мс";

    static final String FINISH_SERVICE_METHOD_EXECUTION_WITH_RESULT
            = "[SERVICE] Завершилось выполнение сервисного метода {}.{}() с результатом {}. Время выполнения: {} мс";

    static final String SERVICE_METHOD_EXECUTION_EXCEPTION
            = "[SERVICE] Ошибка во время выполнения сервисного метода {}.{}(). "
            + "Ошибка: {}, время старта: {}, время выполнения: {} мс";

    static final String START_CONTROLLER_METHOD_EXECUTION = "[REQUEST] {} {}";

    static final String FINISH_CONTROLLER_METHOD_EXECUTION = "[RESPONSE] Статус: {}. Время выполнения: {} мс";

}
