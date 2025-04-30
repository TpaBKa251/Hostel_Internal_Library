# Библиотека для Сервиса для общежитий ТПУ

## Последняя версия 
![GitHub release](https://img.shields.io/github/v/release/TpaBKa251/Hostel_Internal_Library?style=flat-square)

## Описание
Это набор библиотек из нескольких модулей, куда вынесена общая логика микросервисов, общие классы и утилиты, 
а также платформы для удобной и простой работы с различными технологиями. Подробнее можно посмотреть в документации 
каждого модуля.

## Модули

| Модуль   | Назначение                           | Ссылка на README                          |
|----------|--------------------------------------|-------------------------------------------|
| `common` | Общие классы                         | [Документация](./hostel-common/README.md) |
| `core`   | Основная логика библиотеки           | [Документация](./hostel-core/README.md)   |
| `feign`  | Расширение для работы с Feign Client | [Документация](./hostel-feign/README.md)  |
| `amqp`   | Расширение для работы с AMQP         | [Документация](./hostel-amqp/README.md)   |

## Подключение

Для подключения библиотеки необходимо создать файл `gradle/gradle.properties` (**НЕ ДОБАВЛЯТЬ В VCS, НЕ КОММИТИТЬ**). 
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
```groovy
dependencies {
    // common подключать не нужно
    implementation 'ru.tpu.hostel:hostel-core:1.0.3' // Обязательно подключить
    implementation 'ru.tpu.hostel:hostel-feign:1.0.3' // Если используется Feign
    implementation 'ru.tpu.hostel:hostel-amqp:1.0.3' // Если используется AMQP
}
```

## Дополнение

Дополнительную информацию можно посмотреть в документациях других модулей и JavaDoc.