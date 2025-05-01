# Common

Модуль, который содержит общие классы для всех остальных модулей. Идет в комплекте с модулем **_core_**.

### Классы

- **ExecutionContext** - контекст выполнения запросов. Содержит в себе **userID, userRoles, traceID, spanID**.
  Создается в самом начале выполнения запроса/сообщения, очищается в конце в code `finally` блоке.

    ```java
    ExecutionContext.create(userId, roles, traceId, spanId); // или ExecutionContext.create();

    try {
        // логика выполнения
    } catch () {
        // действия при ошибке
    } finally {
        ExecutionContext.clear(); // ОБЯЗАТЕЛЬНО
    }
    ```

- **Roles** - роли юзеров. Содержит статические методы для проверки прав роли на те или иные действия.

- **TimeUtil** - утилита для работы с временем.