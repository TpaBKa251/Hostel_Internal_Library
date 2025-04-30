package ru.tpu.hostel.internal.utils;

import lombok.experimental.UtilityClass;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.TimeZone;

/**
 * Утилита для работы с временем
 *
 * @author Лапшин Илья
 * @version 1.0.3
 * @since 1.0.0
 */
@UtilityClass
public class TimeUtil {

    /**
     * Таймзона Томска (UTC+7)
     *
     * @since 1.0.3
     */
    public static final ZoneId UTC7_ZONE = ZoneId.of("UTC+7");

    /**
     * Возвращает текущее время в таймзоне UTC+7
     *
     * @return текущее время по UTC+7
     * @since 1.0.0
     */
    public static LocalDateTime now() {
        return ZonedDateTime.now(UTC7_ZONE).toLocalDateTime();
    }

    /**
     * Возвращает таймзону UTC+7
     *
     * @return таймзону UTC+7
     * @since 1.0.0
     */
    public static TimeZone getTimeZone() {
        return TimeZone.getTimeZone(UTC7_ZONE);
    }

    /**
     * Возврщает текущее время в виде класса {@link ZonedDateTime} в таймзоне UTC+7
     *
     * @return текущее время по UTC+7
     * @since 1.0.0
     */
    public static ZonedDateTime getZonedDateTime() {
        return ZonedDateTime.now(UTC7_ZONE);
    }

    /**
     * Возвращает отформатированную строку даты и времени, поданных в виде числа миллисекунд.
     * <p>
     * Паттерн: yyyy-MM-dd HH:mm:ss.SSS
     *
     * @param millis число миллисекунд
     * @return строку времени и даты
     * @since 1.0.0
     */
    public static String getLocalDateTimeStingFromMillis(long millis) {
        return LocalDateTime
                .ofInstant(Instant.ofEpochMilli(millis), UTC7_ZONE)
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS"));
    }

}
