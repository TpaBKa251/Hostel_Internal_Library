# Библиотека для Сервиса для общежитий ТПУ

## Последняя версия

![GitHub release](https://img.shields.io/github/v/release/TpaBKa251/Hostel_Internal_Library?style=flat-square) -
подключайте ее.

## Описание

Это набор библиотек из нескольких модулей, куда вынесена общая логика микросервисов, общие классы и утилиты,
а также платформы для удобной и простой работы с различными технологиями. Подробнее можно посмотреть в документации
каждого модуля.

**ВАЖНО** - версии модулей должны быть одинаковыми.

## Модули

| Модуль         | Назначение                                   | Ссылка на README              | Минимальная стабильная версия |
|----------------|----------------------------------------------|-------------------------------|-------------------------------|
| `common`       | Общие классы                                 | [Документация](#Common)       | 1.2.2                         |
| `core`         | Основная логика библиотеки                   | [Документация](#Core)         | 1.2.2                         |
| `feign`        | Расширение для работы с Feign                | [Документация](#Feign)        | 1.2.2                         |
| `amqp`         | Расширение для работы с AMQP                 | [Документация](#AMQP)         | 1.2.2                         |
| `notification` | Расширение для AMQP для отправки уведомлений | [Документация](#Notification) | 1.3.0                         |    

## Подключение

Для подключения библиотеки необходимо создать файл `gradle.properties` в корне проекта
(**НЕ ДОБАВЛЯТЬ В VCS, НЕ КОММИТИТЬ**).
В нем указать следующие данные:

```properties
privateRepoUsername=логин
privateRepoPassword=пароль
```

Затем в `build.gradle` добавить Maven репозиторий:

```groovy
def repoUser = project.findProperty("privateRepoUsername") ?: System.getenv("INTERNAL_REPO_LOGIN")
def repoPass = project.findProperty("privateRepoPassword") ?: System.getenv("INTERNAL_REPO_PASSWORD")

repositories {
    maven {
        url "http://82.202.138.26:8081/repository/internal/"
        allowInsecureProtocol = true
        credentials {
            username = repoUser
            password = repoPass
        }
    }
}
```

В `dependencies` прописать необходимые модули (***модуль core подключаем всегда, common идет в комплекте с core***):

**ВАЖНО** - версии модулей должны быть одинаковыми.

```groovy
dependencies {
    // common подключать не нужно
    implementation 'ru.tpu.hostel:hostel-core:1.0.3' // Обязательно подключить
    implementation 'ru.tpu.hostel:hostel-feign:1.0.3' // Если используется Feign
    implementation 'ru.tpu.hostel:hostel-amqp:1.0.3' // Если используется AMQP
    implementation 'ru.tpu.hostel:notification:1.3.0' // Если используется отправка уведомлений
}
```

## Дополнение

Дополнительную информацию можно посмотреть в документациях других модулей ниже и JavaDoc по ссылке:
[**JavaDoc**](https://tpabka251.github.io/Hostel_Internal_Library/).

---

## Common

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
- **ServiceException** - общее исключение сервиса. Имеет вложенные классы для всех 4хх и 5хх ошибок, названия
  соответствуют ошибкам

### [JavaDoc Common](https://tpabka251.github.io/Hostel_Internal_Library/hostel-common/index.html)

---

## Core

Модуль, который содержит основные классы библиотеки:

- Аспекты и аннотации для логирования репозиториев, сервисов и контроллеров
- Исключения
- Глобальный обработчик ошибок
- Классы для настройки трассировки и ее экспорта
- Интерцептор для работы с контекстом

Включает в себя модуль **_common_**.

### Классы

- **GlobalExceptionHandler** - Глобальный обработчик исключений. Обрабатывает **ServiceException,
  DataIntegrityViolationException и ConstraintViolationException**, а также любые другие в общем виде
- **RepositoryLoggingFilter** - аспект для логирования репозиторных методов классов, в названии которых есть
  **_Repository_**, в пакете **_repository_**
- **ServiceLoggingFilter** - аспект для логирования сервисных методов классов в пакете **_service_**
- **RequestLoggingFilter** - аспект для логирования методов контроллеров (запросов от клиента) классов, в названии
  которых есть **_Controller_**, в пакете **_controller_**
- **ResponseLoggingFilter** - класс-фильтр для логирования ответа клиенту
- **LogFilter** - аннотация для фильтрации способов логирования **_сервисного слоя_**. Способы: логирование метода,
  логирование параметров, логирование вывода
- **SecretArgument** - аннотация для скрытия чувствительных данных в параметрах (аргументах) метода из логов
  **_сервисного слоя_**
- **OpenTelemetryProperties** - свойства для Open Telemetry трассировки. Пишутся в **_application.yaml_**
- **OpenTelemetryConfig** - конфигурация для настройки трассировки через Open Telemetry и ее экспорта
- **HttpRestInterceptor** - интерцептор для создания **ExecutionContext** на старте выполнения запроса и очистке
  контекста по завершении обработки запроса

### [JavaDoc Core](https://tpabka251.github.io/Hostel_Internal_Library/hostel-core/index.html)

---

## Feign

Модуль-расширение для **_core_** для работы с Feign. Расширяет функции основного модуля, добавляя логирование клиентов
Feign, обработку FeignException, установку заголовков для передачи данных из ExecutionContext
(трассировка, userId, userRoles) при отправке HTTP запросов.

### Классы

- **FeignExceptionHandler** - глобальный обработчик исключений для FeignException
- **FeignClientLoggingFilter** - аспект для логирования методов Feign клиентов (классов с аннотацией **FeignClient**)
- **HttpRestInterceptor** - интерцептор для добавления в заголовок REST запросов, которые отправляются через Feign
  клиент, информации о трассировке и данных текущего пользователя

### [JavaDoc Feign](https://tpabka251.github.io/Hostel_Internal_Library/hostel-feign/index.html)

---

## AMQP

Модуль-расширение для **_core_** для работы с AMQP (по большей части с RabbitMQ). Расширяет функционал основного модуля,
добавляя логирование отправки и получения сообщений через RabbitMQ, передачу контекста при отправке сообщений
(данные юзера и трассировка). Также предоставляет классы и интерфейсы для удобной работы с Rabbit.

### Классы

- **AmqpMessageReceiveInterceptor** - интерцептор для создания ExecutionContext при получении сообщения. Нужно вручную
  добавлять в фабрику слушателя
- **AmqpMessagingConfig** - интерфейс конфига для отправки сообщений. Необходимо создавать бины-реализации, на основе
  которых будет происходить отправка
- **AmqpMessageSender** - интерфейс отправителя сообщений. Имеет дефолтную универсальную реализацию - писать свою
  необязательно
- **DefaultAmqpMessageSender** - дефолтная реализация интерфейса AmqpMessageSender. Можно использовать везде и всюду,
  необходимо лишь написать бины конфигов для отправки AmqpMessagingConfig, на их основе происходит отправка сообщений
  через этот класс
- **Microservice** - енам микросервисов

### [JavaDoc AMQP](https://tpabka251.github.io/Hostel_Internal_Library/hostel-amqp/index.html)

## Notification

Модуль-расширение для **_AMQP_** для асинхронной и безопасной отправки уведомлений в Notification Service
через RabbitMQ. Предоставляет классы для удобной и простой отправки, в проекте нужно лишь прописать свойства в
application.yaml/application.properties (подключение к Rabbit и свойства очередей).

### Классы

- **NotificationRequestDto** - ДТО для отправки сообщения о создании уведомления в Notification Service
- **NotificationRequestBuilder** - Интерфейс билдера для ДТО отправки уведомления в Notification Service.
  Имеет стандартную реализацию для безопасного создания ДТО DefaultNotificationRequestBuilder
- **DefaultNotificationRequestBuilder** -Дефолтная реализация NotificationRequestBuilder. Безопасно билдит ДТО
  (не кидает исключения)
- **NotificationSender** - Интерфейс для отправки уведомлений. Имеет дефолтную безопасную реализацию
  DefaultNotificationSender
- **DefaultNotificationSender** - Дефолтная реализация NotificationSender. Безопасно отправляет сообщения с ДТО
  через RabbitMQ (не кидает исключения)

### [JavaDoc AMQP](https://tpabka251.github.io/Hostel_Internal_Library/notification/index.html)