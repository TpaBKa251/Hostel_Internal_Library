package ru.tpu.hostel.internal.utils;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Collections;
import java.util.Set;
import java.util.UUID;

/**
 * Контекст выполнения запроса сервисом. Содержит в себе {@code userID, userRoles, traceID, spanID}
 *
 * <p>💡Создавать в самом начале выполнения запроса/сообщения, очищать в конце в {@code finally} блоке.
 * <pre><code>
 *     ExecutionContext.create(userId, roles, traceId, spanId); // или ExecutionContext.create();
 *
 *     try {
 *         // логика выполнения
 *     } catch() {
 *         // действия при ошибке
 *     } finally {
 *         ExecutionContext.clear(); // ОБЯЗАТЕЛЬНО
 *     }
 * </code></pre>
 *
 * @author Илья Лапшин
 * @version 1.0.0
 * @since 1.0.0
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public final class ExecutionContext {

    private static final ThreadLocal<ExecutionContext> CONTEXT_HOLDER = new ThreadLocal<>();

    @Getter
    private UUID userID;

    private Set<Roles> userRoles;

    @Getter
    private String traceId;

    @Getter
    private String spanId;

    /**
     * Создает пустой контекст
     *
     * @return созданный контекст
     * @since 1.0.0
     */
    public static ExecutionContext create() {
        if (CONTEXT_HOLDER.get() != null) {
            return CONTEXT_HOLDER.get();
        }
        ExecutionContext context = new ExecutionContext();
        CONTEXT_HOLDER.set(context);
        return context;
    }

    /**
     * Создает контекст с полным набором параметров
     *
     * @param userID    ID юзера, отправившего запрос
     * @param userRoles роли юзера, отправившего запрос
     * @param traceId   ID трассировки
     * @param spanId    ID спана
     * @return созданный контекст
     * @since 1.0.0
     */
    public static ExecutionContext create(UUID userID, Set<Roles> userRoles, String traceId, String spanId) {
        if (CONTEXT_HOLDER.get() != null) {
            return CONTEXT_HOLDER.get();
        }
        ExecutionContext context = new ExecutionContext(userID, userRoles, traceId, spanId);
        CONTEXT_HOLDER.set(context);
        return context;
    }

    /**
     * Возвращает текущий контекст
     *
     * @return текущий контекст
     * @since 1.0.0
     */
    public static ExecutionContext get() {
        return CONTEXT_HOLDER.get();
    }

    /**
     * Очищает текущий контекст.
     *
     * <p>💡Обязательно выполнять, если контекст создавался. Вызывать в {@code finally} блоке</p>
     *
     * @since 1.0.0
     */
    public static void clear() {
        CONTEXT_HOLDER.remove();
    }

    public Set<Roles> getUserRoles() {
        return this.userRoles == null
                ? Collections.emptySet()
                : Collections.unmodifiableSet(this.userRoles);
    }

}