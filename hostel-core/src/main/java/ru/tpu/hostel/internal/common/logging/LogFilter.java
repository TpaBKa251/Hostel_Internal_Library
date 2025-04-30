package ru.tpu.hostel.internal.common.logging;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Аннотация для фильтрации способов логирования <b><i>сервисного слоя</i></b>. Может ставиться на методы классов и
 * интерфейсов, а также на сами классы и интерфейсы.
 * <p>💡Необходимо ставить на классы, интерфейсы или методы, если необходимо отключить какой-то способ логирования. При
 * установке на интерфейс/класс, параметры применяются ко всем методам и реализациям/наследникам.
 * <p>❗<b><i>РАБОТАЕТ ТОЛЬКО НА СЕРВИСНЫХ КЛАССАХ И МЕТОДАХ (в пакете service)</i></b>
 *
 * @author Илья Лапшин
 * @version 1.0.0
 * @see SecretArgument
 * @see ServiceLoggingFilter
 * @since 1.0.0
 */
@Inherited
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface LogFilter {

    /**
     * Логирование метода. По умолчанию {@code true}
     */
    boolean enableMethodLogging() default true;

    /**
     * Логирование параметров (аргументов) метода. По умолчанию {@code true}
     */
    boolean enableParamsLogging() default true;

    /**
     * Логирование результата метода. По умолчанию {@code true}
     */
    boolean enableResultLogging() default true;

}
