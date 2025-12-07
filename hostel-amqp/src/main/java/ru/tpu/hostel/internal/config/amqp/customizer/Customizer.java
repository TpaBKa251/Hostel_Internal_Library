package ru.tpu.hostel.internal.config.amqp.customizer;

/**
 * Функциональный интерфейс кастомайзера для бинов.
 *
 * @param <T> кастомизируемый класс
 */
@FunctionalInterface
public interface Customizer<T> {

    /**
     * Метод кастомизации. Принимает экземпляр класса T, его необходимо модифицировать или изменить.
     *
     * @param t экземпляр класса для кастомизации.
     */
    void customize(T t);

}
