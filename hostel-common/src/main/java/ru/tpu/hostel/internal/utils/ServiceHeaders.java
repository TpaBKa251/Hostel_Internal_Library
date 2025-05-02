package ru.tpu.hostel.internal.utils;

import lombok.experimental.UtilityClass;

@UtilityClass
public class ServiceHeaders {

    public static final String USER_ID_HEADER = "X-User-Id";

    public static final String USER_ROLES_HEADER = "X-User-Roles";

    public static final String TRACEPARENT_HEADER = "traceparent";

    public static final String TRACEPARENT_PATTERN = "00-%s-%s-01";

}
