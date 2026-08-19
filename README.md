# User Service

Микросервис профилей пользователей и их контактов для распределённой (микросервисной) системы.
Написан на **Kotlin 2.3 / Spring Boot 4.1 / Java 21**, общается с внешним миром через **REST**,
а с соседними сервисами — асинхронно через **Apache Kafka**.

> Сервис намеренно не отвечает за аутентификацию: пароли, регистрация и выдача токенов живут
> в отдельном **AuthService**. User Service лишь *доверяет* его JWT (проверяя подпись публичным
> ключом) и слушает его события, создавая и удаляя локальные профили.

---

## Содержание

- [О проекте](#о-проекте)
- [Ключевые возможности](#ключевые-возможности)
- [Архитектура](#архитектура)
- [Технологический стек](#технологический-стек)
- [Структура проекта](#структура-проекта)
- [Модель данных](#модель-данных)
- [REST API](#rest-api)
- [События Kafka](#события-kafka)
- [Безопасность](#безопасность)
- [Быстрый старт (Docker Compose)](#быстрый-старт-docker-compose)
- [Локальный запуск без Docker](#локальный-запуск-без-docker)
- [Переменные окружения](#переменные-окружения)
- [Тестирование](#тестирование)
- [Наблюдаемость](#наблюдаемость)
- [Что показывает этот проект](#что-показывает-этот-проект)

---

## О проекте

Это учебно-портфолийный, но собранный «по-взрослому» микросервис: с чистой архитектурой,
транзакционным Outbox, dead-letter обработкой Kafka, миграциями БД, трёхуровневым тестированием
на Testcontainers и полной OpenAPI-документацией.

Сервис решает одну ограниченную предметную задачу:

1. **Профили** — хранение и выдача публичной/приватной информации о пользователе, обновление
   собственного профиля, поиск по username с пагинацией.
2. **Контакты** — «список друзей»: добавление, просмотр (постранично) и удаление контактов.
3. **Интеграция** — приём событий о создании/удалении пользователей из AuthService и публикация
   собственных доменных событий для остальных сервисов системы.

---

## Ключевые возможности

| Возможность | Реализация |
|---|---|
| Чистая архитектура | Разделение `domain` / `application` / `infrastructure`, use-case на операцию |
| Аутентификация без БД сессий | Stateless JWT (ES256), проверка подписи публичным ключом |
| Надёжная публикация событий | Транзакционный **Outbox** + планировщик-публикатор |
| Устойчивое потребление событий | Ручной коммит оффсетов, ретраи с backoff, **DLT** (`<topic>.DLT`) |
| Идемпотентность | Обработчики входящих событий безопасны к повторной доставке (at-least-once) |
| Мягкое удаление | `deleted_at` у пользователя вместо физического удаления |
| Версионирование схемы | Flyway-миграции в `db/migration` |
| Документация API | springdoc-openapi + Swagger UI |
| Единый формат ошибок | `@ControllerAdvice` + `ErrorDto { message, status }` |
| Health-check | Actuator + собственный `PostgresqlHealthIndicator` |
| Тесты | Unit / Integration / E2E на Testcontainers (PostgreSQL + Kafka) |

---

## Архитектура

### Место сервиса в системе

```
                    ┌───────────────┐
   HTTP + JWT  ───▶ │  User Service │ ───▶ Kafka: user.created / user.updated / user.deleted
                    │               │            contact.added / contact.removed
                    └───────┬───────┘
                            │  ▲
                 JPA/Flyway │  │ Kafka: auth.user.created / auth.user.deleted
                            ▼  │
                     ┌──────────┐        ┌─────────────┐
                     │PostgreSQL│        │ AuthService │
                     └──────────┘        └─────────────┘
```

### Внутренние слои

Каждый бизнес-модуль (`user`, `contact`, `outboxevent`) построен одинаково:

```
user/
├── domain/                 # Модель и контракты репозиториев — без Spring и JPA
│   ├── model/User.kt
│   └── repository/UserRepository.kt
├── application/            # Сценарии использования: один use-case = одна операция
│   └── use_case/
│       ├── me/GetMyInfoUseCase.kt
│       ├── update/UpdateUserUseCase.kt
│       ├── search/SearchUserUseCase.kt
│       ├── public_profile/PublicProfileUseCase.kt
│       ├── create_from_auth/CreateUserFromAuthUseCase.kt
│       └── delete_from_auth/DeleteUserFromAuthUseCase.kt
└── infrastructure/         # Всё «грязное»: web, persistence, messaging, security
    ├── web/UserController.kt + dto/
    ├── persistance/        # JPA-сущности и реализация доменных репозиториев
    ├── messaging/          # Kafka-слушатели и парсер событий
    ├── mapper/             # MapStruct-мапперы entity ↔ domain ↔ DTO
    └── security/           # JwtFilter, UserDetailsService, обработчики 401/403
```

Зависимости направлены строго внутрь: контроллер знает про use-case, use-case — про доменный
репозиторий, а реализация репозитория (JPA) подставляется в инфраструктурном слое.
Команды (`*Command`) — входные DTO уровня приложения, поэтому web-слой можно заменить,
не трогая бизнес-логику.

### Паттерн Transactional Outbox

Публиковать событие «в лоб» из транзакции нельзя: коммит в БД и отправка в брокер не атомарны.
Поэтому use-case в **той же транзакции**, что и бизнес-изменение, пишет запись в `outbox_events`,
а фоновый `OutboxPublisher` (`@Scheduled`, по умолчанию раз в 1000 мс) забирает пачку до 100
необработанных записей, синхронно отправляет их в Kafka и только после подтверждения брокера
помечает `processed = true`.

```
UpdateUserUseCase ──┐ одна транзакция
                    ├─▶ users (UPDATE)
                    └─▶ outbox_events (INSERT, processed = false)
                                   │
                    OutboxPublisher│ каждые ~1s, батч 100
                                   ▼
                        Kafka topic "user.updated"  ──▶ markAsProcessed
```

Гарантия — **at-least-once**: при падении между отправкой и пометкой событие уедет повторно,
что компенсируется идемпотентностью потребителей.

### Обработка входящих событий

Консьюмеры читают **сырую строку** (`StringDeserializer`), а JSON разбирается вручную в
`AuthEventParser`. Это сделано осознанно: ошибка формата становится обычным исключением приложения
(`InvalidEventException`), которое можно управляемо отправить в dead-letter топик, а не падением
на уровне десериализации.

- Транзиентные сбои (например, недоступна БД) → 3 попытки с паузой 1 с.
- `InvalidEventException` (битый JSON, отсутствующий `userId`) → сразу в `<topic>.DLT`, без ретраев.
- Оффсет коммитится вручную после успешной обработки записи (`AckMode.RECORD`).
- Парсер толерантен к контракту: принимает `userId` / `user_id` / `id`, `createdAt` / `created_at` /
  `occurredAt`, а также вложенный объект `payload`.

---

## Технологический стек

| Категория | Технологии |
|---|---|
| Язык и рантайм | Kotlin 2.3.21, JVM 21 (Eclipse Temurin) |
| Фреймворк | Spring Boot 4.1 (Web MVC, Security, Data JPA, Validation, Cache, Actuator) |
| БД | PostgreSQL 16, Hibernate, Flyway |
| Брокер | Apache Kafka 7.6 (Confluent) + ZooKeeper |
| Безопасность | JJWT 0.13 (ES256, EC-ключи) |
| Маппинг | MapStruct 1.6 (kapt, `defaultComponentModel = spring`) |
| Документация | springdoc-openapi 3.1 (Swagger UI) |
| Логирование | kotlin-logging-jvm |
| Тесты | JUnit 5, Mockito-Kotlin, Spring Boot Test, Testcontainers (PostgreSQL, Kafka) |
| Сборка и деплой | Gradle Kotlin DSL, многостадийный Dockerfile, Docker Compose |

---

## Структура проекта

```
.
├── build.gradle.kts            # Сборка, kapt/MapStruct, allOpen для JPA
├── Dockerfile                  # Многостадийная сборка: JDK build → JRE runtime
├── docker-compose.yaml         # app + postgres + kafka + zookeeper
├── .env-example                # Шаблон переменных окружения
├── zookeeper/log4j.properties  # Приглушённые логи ZooKeeper
└── src
    ├── main/kotlin/com/user
    │   ├── configs/            # Security, JWT, Kafka (producer/consumer), OpenAPI, Clock, Actuator
    │   ├── user/               # Модуль профилей
    │   ├── contact/            # Модуль контактов
    │   ├── outboxevent/        # Outbox: модель, writer, publisher
    │   ├── exceptions/         # Доменные исключения + GlobalExceptionHandler
    │   └── utils/              # JwtController, PageResponse
    ├── main/resources
    │   ├── application.properties
    │   └── db/migration/       # V1 users, V2 contacts, V3 outbox_events
    └── test/kotlin/com/user    # Unit / Integrated / E2E тесты по модулям
```

---

## Модель данных

**users**

| Колонка | Тип | Примечание |
|---|---|---|
| `id` | UUID PK | приходит из AuthService, не генерируется локально |
| `username` | VARCHAR(20) UNIQUE NOT NULL | латиница, цифры, `_` |
| `avatar_url` | VARCHAR(1024) | |
| `bio` | VARCHAR(1024) | |
| `created_at` / `updated_at` | TIMESTAMP | |
| `deleted_at` | TIMESTAMP | мягкое удаление, есть индекс |

**contacts**

| Колонка | Тип | Примечание |
|---|---|---|
| `id` | UUID PK | |
| `user_id` | UUID FK → users | `ON DELETE CASCADE`, индекс |
| `contact_user_id` | UUID FK → users | `ON DELETE CASCADE`, индекс |
| `created_at` | TIMESTAMP | |

Ограничения: `UNIQUE (user_id, contact_user_id)` и `CHECK (user_id != contact_user_id)` —
дубликаты и «контакт с самим собой» отсекаются на уровне БД, а не только кодом.

**outbox_events**

| Колонка | Тип | Примечание |
|---|---|---|
| `id` | UUID PK | попадает в заголовок `eventId` |
| `event_type` | VARCHAR | `UserCreated`, `ContactAdded`, … |
| `aggregate_id` | UUID | становится ключом сообщения Kafka |
| `payload` | JSONB | тело события |
| `create_at` | TIMESTAMP | |
| `processed` | BOOLEAN | частичный индекс `WHERE processed = false` |

---

## REST API

Базовый префикс — `/api/v1`. Полная интерактивная документация: **`/swagger-ui.html`**,
машинная схема — **`/v3/api-docs`**.

### Users

| Метод | Путь | Auth | Описание |
|---|---|---|---|
| `GET` | `/api/v1/users/{username}` | — | Публичный профиль по username |
| `GET` | `/api/v1/users/me` | JWT | Профиль владельца access-токена (включая счётчик контактов) |
| `PUT` | `/api/v1/users/{userId}` | JWT | Обновление собственного профиля |
| `GET` | `/api/v1/users/search?query=&page=&size=` | — | Поиск по username с пагинацией |

Коды ошибок: `400` — невалидный запрос или параметры поиска, `401` — нет/битый токен,
`403` — попытка изменить чужой профиль, `404` — пользователь не найден, `409` — username занят.

### Contacts

| Метод | Путь | Auth | Описание |
|---|---|---|---|
| `GET` | `/api/v1/contacts?page=&size=` | JWT | Контакты текущего пользователя, постранично |
| `POST` | `/api/v1/contacts` | JWT | Добавить контакт (`201` + заголовок `Location`) |
| `DELETE` | `/api/v1/contacts/{id}` | JWT | Удалить контакт (`204`) |

Коды ошибок: `400` — невалидное тело или контакт с самим собой, `401` — нет/битый токен,
`409` — такой контакт уже есть, `404` — контакт не найден.

### Формат ошибки

```json
{
  "message": "Пользователь не найден",
  "status": "NOT_FOUND"
}
```

Все ошибки проходят через `GlobalExceptionHandler`: доменные исключения отдают свой статус,
ошибки валидации сворачиваются в `400`, непойманные исключения логируются и превращаются в `500`
без утечки стектрейса наружу.

### Пример запроса

```bash
curl -H "Authorization: Bearer $ACCESS_TOKEN" http://localhost:8080/api/v1/users/me
```

---

## События Kafka

### Публикуемые (через Outbox)

| Тип события | Топик | Payload |
|---|---|---|
| `UserCreated` | `user.created` | `userId`, `username`, `createdAt` |
| `UserUpdated` | `user.updated` | `userId`, `displayName`, `avatarUrl`, `updatedAt` |
| `UserDeleted` | `user.deleted` | `userId`, `deletedAt` |
| `ContactAdded` | `contact.added` | `id`, `userId`, `contactId`, `addedAt` |
| `ContactRemoved` | `contact.removed` | `userId`, `contactId`, `deletedAt` |

Каждое сообщение несёт ключ (`aggregate_id`, что гарантирует порядок в рамках агрегата) и
заголовки `eventType` и `eventId` — последний удобно использовать для дедупликации на стороне
потребителя.

### Потребляемые

| Топик (настраивается) | Обработчик | Результат |
|---|---|---|
| `auth.user.created` | `CreateUserFromAuthUseCase` | Создаётся локальный профиль |
| `auth.user.deleted` | `DeleteUserFromAuthUseCase` | Профиль помечается удалённым (`deleted_at`) |

> Контракт `AuthUserDeleted` в коде помечен как предположительный — имя топика и структуру payload
> следует согласовать с командой AuthService. Имена топиков переопределяются переменными
> `KAFKA_TOPIC_AUTH_USER_CREATED` и `KAFKA_TOPIC_AUTH_USER_DELETED`.

Необработанные сообщения уходят в `auth.user.created.DLT` / `auth.user.deleted.DLT`.

---

## Безопасность

- **Stateless**: `SessionCreationPolicy.STATELESS`, CSRF отключён (нет cookie-сессий), CORS
  настраивается через `CORS_ALLOWED_ORIGIN_PATTERNS`.
- **JWT (ES256)**: `JwtFilter` достаёт токен из заголовка `Authorization: Bearer …`, проверяет
  подпись и тип токена (`access`), затем загружает пользователя и кладёт его в `SecurityContext`.
  Выпуск токенов остаётся за AuthService — здесь они только валидируются.
- **Авторизация на уровне ресурса**: изменить можно только свой профиль
  (`CannotUpdateAnotherUserException` → `403`), удалить — только свой контакт
  (`YouAreNotOwnerException`).
- **Единые 401/403**: `CustomAuthenticationEntryPoint` и `CustomAccessDeniedHandler` отдают тот же
  `ErrorDto`, что и остальные ошибки.

> ⚠️ Пара EC-ключей в `application.properties` и `docker-compose.yaml` — **только для локальной
> разработки**. Для продакшена обязательно задайте собственные `JWT_PRIVATE_KEY` / `JWT_PUBLIC_KEY`.

---

## Быстрый старт (Docker Compose)

Требуется только Docker с плагином Compose.

```bash
git clone <repository-url> && cd user
```

```bash
cp .env-example .env
```

```bash
docker compose up -d --build
```

Поднимутся четыре контейнера: `db` (PostgreSQL 16), `zookeeper`, `kafka` и `service`.
Приложение стартует после того, как healthcheck PostgreSQL пройдёт успешно; Flyway накатывает
миграции при старте.

Проверка:

```bash
curl http://localhost:8080/actuator/health
```

| Что | Адрес |
|---|---|
| API | http://localhost:8080/api/v1 |
| Swagger UI | http://localhost:8080/swagger-ui.html |
| OpenAPI JSON | http://localhost:8080/v3/api-docs |
| Actuator | http://localhost:8080/actuator |
| PostgreSQL | `localhost:5432` |
| Kafka (снаружи) | `localhost:9092` |

Логи:

```bash
docker compose logs -f service
```

Остановка:

```bash
docker compose down
```

Полный сброс вместе с данными (удалит тома `pg_data`, `kafka_data`, `zookeeper_data`):

```bash
docker compose down -v
```

---

## Локальный запуск без Docker

Нужны JDK 21, запущенный PostgreSQL и Kafka. Проще всего поднять только инфраструктуру:

```bash
docker compose up -d db kafka zookeeper
```

Сборка:

```bash
./gradlew build
```

Запуск:

```bash
./gradlew bootRun
```

Сборка исполняемого jar и запуск вручную:

```bash
./gradlew bootJar
```

```bash
java -jar build/libs/user-0.0.1-SNAPSHOT.jar
```

На Windows вместо `./gradlew` используйте `gradlew.bat`.

Значения по умолчанию рассчитаны на локальную машину: БД `jdbc:postgresql://localhost:5432/user`
(пользователь/пароль `admin`/`admin`), Kafka `localhost:9092`, порт приложения `8080`.

---

## Переменные окружения

| Переменная | По умолчанию | Назначение |
|---|---|---|
| `SPRING_PROFILES_ACTIVE` | `dev` | Активный профиль Spring |
| `APP_PORT` | `8080` | Порт HTTP |
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://localhost:5432/user` | JDBC-строка |
| `SPRING_DATASOURCE_USERNAME` / `SPRING_DATASOURCE_PASSWORD` | `admin` / `admin` | Учётные данные БД |
| `KAFKA_BOOTSTRAP_SERVERS` | `localhost:9092` | Брокеры Kafka |
| `KAFKA_CONSUMER_GROUP_ID` | `user` | Consumer group |
| `KAFKA_TOPIC_AUTH_USER_CREATED` | `auth.user.created` | Топик события создания из Auth |
| `KAFKA_TOPIC_AUTH_USER_DELETED` | `auth.user.deleted` | Топик события удаления из Auth |
| `JWT_PRIVATE_KEY` / `JWT_PUBLIC_KEY` | dev-ключи | Пара EC-ключей (Base64) |
| `JWT_ACCESS_EXPIRATION` | `900` | TTL access-токена, сек |
| `JWT_REFRESH_EXPIRATION` | `864000` | TTL refresh-токена, сек |
| `TOKEN_PAIR_ID_CLAIM` / `TOKEN_TYPE_CLAIM` | `tokenPairId` / `tokenType` | Имена claim-ов JWT |
| `ACCESS_TOKEN_TYPE` / `REFRESH_TOKEN_TYPE` | `access` / `refresh` | Значения claim-а типа токена |
| `CORS_ALLOWED_ORIGIN_PATTERNS` | `http://localhost:*,http://127.0.0.1:*` | Разрешённые источники |
| `SWAGGER_UI_ENABLED` / `SWAGGER_API_DOCS_ENABLED` | `true` | Выключатели документации для prod |
| `SPRING_LOGGING_LEVEL_ROOT` | `info` | Уровень логирования |
| `outbox.publisher.poll-interval-ms` | `1000` | Период опроса outbox-таблицы |

Шаблон со всеми переменными лежит в `.env-example`.

---

## Тестирование

```bash
./gradlew test
```

Тесты организованы по трём уровням, и для большинства сценариев есть все три:

| Уровень | Что проверяет | Инструменты |
|---|---|---|
| **Unit** (`*UnitTests`) | Логика use-case в изоляции | JUnit 5, Mockito-Kotlin |
| **Integrated** (`*IntegratedTests`) | Связка use-case ↔ репозиторий ↔ реальная БД, запись в outbox, публикация | Spring Boot Test, Testcontainers PostgreSQL/Kafka |
| **E2E** (`*E2eTests`) | HTTP-контракт: коды ответов, тела, авторизация | Полный контекст приложения + MockMvc |

Покрытые сценарии: получение своего профиля, публичный профиль по path-переменной, обновление
профиля, поиск пользователей, создание/чтение/удаление контактов, запись и публикация
outbox-событий, парсинг и обработка событий AuthService.

Для интеграционных тестов нужен работающий Docker — Testcontainers поднимает PostgreSQL и Kafka
автоматически (`TestcontainersConfiguration`, `TestContainersConfig`).

---

## Наблюдаемость

- `GET /actuator/health` — общий статус; `management.endpoint.health.show-details=always`.
- Кастомный индикатор `postgresql` реально открывает соединение (`isValid`, таймаут 2 с) и
  показывает продукт и версию БД — в отличие от поверхностной проверки, он ловит ситуацию
  «пул живой, база мёртвая».
- Healthcheck PostgreSQL в Compose (`pg_isready`) не даёт приложению стартовать раньше БД.
- Ошибки публикации outbox и уходы сообщений в DLT логируются с topic / partition / offset.
- Endpoints Actuator и Swagger вынесены из-под авторизации (`permitAll`) — при выкатке в публичную
  сеть их следует закрыть на уровне ingress или выключить переменными `SWAGGER_*_ENABLED`.

---

## Что показывает этот проект

- Проектирование микросервиса с чёткими границами: сервис владеет своими данными и не ходит в чужие БД.
- Слоистая (гексагональная по духу) архитектура на Kotlin без «жирных сервисов»: одна операция —
  один use-case с явным `Command` на входе.
- Работа с распределёнными гарантиями доставки: Outbox, at-least-once, идемпотентность,
  ретраи и dead-letter топики — а не «отправили в Kafka и надеемся».
- Эксплуатационная зрелость: миграции, health-чеки, конфигурация целиком через переменные окружения,
  многостадийный Docker-образ (сборка на JDK, рантайм на JRE).
- Дисциплина тестирования: unit / integration / e2e на реальных зависимостях через Testcontainers.
