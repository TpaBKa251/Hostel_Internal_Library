package ru.tpu.hostel.internal.common.logging;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Аннотация для скрытия чувствительных данных в параметрах (аргументах) метода из логов.
 * <p>💡Необходимо ставить аннотацию на параметры метода, если их необходимо скрыть из логов.
 * <p>❗<b><i>РАБОТАЕТ ТОЛЬКО НА СЕРВИСНЫХ КЛАССАХ И МЕТОДАХ (в пакете service)</i></b>
 *
 * @author Илья Лапшин
 * @version 1.0.0
 * @see LogFilter
 * @see ServiceLoggingFilter
 * @since 1.0.0
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface SecretArgument {
}
