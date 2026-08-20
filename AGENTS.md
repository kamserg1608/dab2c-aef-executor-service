# AGENTS.md

## Контекст проекта

`dab2c-executor` — многомодульный сервис DA B2C Executor на Gradle Kotlin DSL.

- `rootProject.name` — `dab2c-executor`.
- Java 21 и JVM target 21.
- Kotlin 2.1.20, официальный Kotlin code style.
- Gradle 8.12.1 через Gradle Wrapper.
- Spring Boot 3.5.16.
- Тесты: JUnit 5, MockK, Kotlin Coroutines Test, WireMock.
- Статический анализ: detekt, Checkstyle и SpotBugs; ошибки не игнорируются.

Всегда использовать `./gradlew`, а не системный Gradle. Не добавлять в файлы проекта логины, пароли, токены или значения корпоративных credentials.

## Структура

| Каталог | Назначение |
| --- | --- |
| `executor-application` | Spring Boot entry point, конфигурация приложения и профили |
| `executor-services/voice-executor` | Оркестрация голосовой сессии, settings/function call и обработка chunks |
| `executor-clients/*` | Интеграции с Configurator, EFS Adapter, Giga Agent/Giga Voice, IAG и KAP |
| `executor-domain-model` | Общие доменные модели |
| `executor-libraries/*` | Общие библиотеки context, logging, monitoring, audit, tracing, time и test-support |
| `executor-e2e-tests` | Сквозные gRPC/integration-тесты и WireMock-стенды |
| `executor-distribution` | Сборка дистрибутива и конфигурационных ресурсов |
| `buildSrc` | Общие Gradle convention plugins |
| `config` | Конфигурации detekt, Checkstyle и SpotBugs |
| `jenkins` | Jenkins pipeline scripts для сборки и PR-проверок |

Перед изменением определить затронутый модуль и прочитать его `build.gradle.kts`, соседние реализации и релевантные тесты. Не расширять область изменений без необходимости.

## Правила реализации

- Следовать существующим Kotlin- и Java-паттернам проекта; не смешивать слои API, mapper, client и service.
- Для новых или изменённых полей проверять полный путь данных: внешний DTO/proto → mapper/converter → доменная модель → API или downstream-запрос.
- Для nullable и коллекционных полей покрывать позитивный, `null`, empty и отсутствующий сценарии, если они допустимы контрактом.
- Сохранять coroutine/reactive context, MDC и tracing при добавлении асинхронных границ и декораторов.
- Не блокировать coroutine/reactive код без уже принятого в модуле основания.
- Не редактировать сгенерированные OpenAPI, protobuf или KSP-файлы вручную. Менять исходную спецификацию или mapper и запускать штатную генерацию Gradle.
- При изменении OpenAPI или protobuf проверять совместимость контракта и все использующие его клиенты/сервисы.
- Новые настройки размещать в типизированных configuration properties и проверять профили `PROM`, `STUB` и тестовую конфигурацию, когда они затронуты.
- Названия тестов должны соответствовать фактическим входным данным и ожидаемому результату.
- Не оставлять unused, redundant или wildcard imports.

## Проверка изменений

Сначала запускать минимальный релевантный тест:

```bash
./gradlew :<module>:test --tests '<fully.qualified.TestClass>'
```

Для всех тестов модуля:

```bash
./gradlew :<module>:test
```

Для сквозных сценариев:

```bash
./gradlew :executor-e2e-tests:test
```

Перед завершением задачи запускать полную проверку:

```bash
./gradlew clean build
```

`build` должен фактически выполнить тесты и подключённые проверки detekt, Checkstyle и SpotBugs. Не отключать их через `-x`, `skip.all.checks`, `ignoreFailures` или аналогичные флаги без прямого запроса пользователя. Если полную сборку выполнить нельзя, перечислить отдельно, какие команды запускались, что прошло и что осталось непроверенным.

Для доступа к корпоративным Nexus-репозиториям использовать уже настроенные credentials через системные свойства Gradle. Не записывать их в `gradle.properties`, скрипты или документацию.

## Критерии готовности

Задача завершена, только если:

- реализация соответствует существующей архитектуре модулей;
- затронутые mapper/converter и контракты согласованы;
- добавлены или обновлены релевантные тесты;
- позитивные и допустимые `null`/empty сценарии покрыты;
- релевантные тесты успешно выполнены;
- `./gradlew clean build` успешно выполнен либо явно зафиксирована причина, почему он не запускался или не завершился;
- отдельно указан фактический результат detekt, Checkstyle и SpotBugs;
- в изменённых файлах нет случайных generated/build-артефактов и секретов.

