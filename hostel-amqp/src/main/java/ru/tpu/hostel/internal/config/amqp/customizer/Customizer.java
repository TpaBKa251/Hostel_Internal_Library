package ru.tpu.hostel.internal.config.amqp.customizer;

@FunctionalInterface
public interface Customizer<T> {

    void customize(T t);

}
