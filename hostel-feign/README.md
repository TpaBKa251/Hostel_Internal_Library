# Feign

Модуль-расширение для **_core_** для работы с Feign. Расширяет функции основного модуля, добавляя логирование клиентов
Feign, обработку FeignException, установку заголовков для передачи данных из ExecutionContext 
(трассировка, userId, userRoles) при отправке HTTP запросов.

### Классы

- **FeignExceptionHandler** - глобальный обработчик исключений для FeignException
- **FeignClientLoggingFilter** - аспект для логирования методов Feign клиентов (классов с аннотацией **FeignClient**)
- **HttpRestInterceptor** - интерцептор для добавления в заголовок REST запросов, которые отправляются через Feign 
  клиент, информации о трассировке и данных текущего пользователя