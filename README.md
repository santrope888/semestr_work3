# Aura Motum — Система аренды автомобилей

Платформа для аренды автомобилей с личным кабинетом,
бронированием, оплатой и ИИ-помощником по подбору.

## Технологии

- **Backend:** Java 21, Spring Boot 4.x, Spring MVC, Spring Security, Spring Data JPA
- **Шаблонизация:** Thymeleaf
- **Реляционная БД:** PostgreSQL 16, Flyway (миграции V1—V18)
- **Документная БД (NoSQL):** MongoDB 7 + GridFS — для хранения документов пользователя
- **Маппинг:** MapStruct
- **Документация API:** springdoc-openapi 3.0.2 (Swagger UI)
- **Тесты:** JUnit 5, Mockito, AssertJ, Spring Security Test, Testcontainers
- **Покрытие:** JaCoCo, enforcer на 100% (METHOD / LINE / BRANCH) сервисного слоя
- **Отказоустойчивость:** Spring Retry — повторные попытки при сбоях вызова ИИ
- **Инфраструктура:** Docker Compose (app + postgres + mongo + ollama)
- **ИИ-помощник:** Ollama-сервер в Docker + cloud-модель `gpt-oss:120b-cloud`
  через Ollama Cloud (требуется `ollama signin` один раз на машину)
- **Внешние API:** exchangerate-api (курсы валют через RestTemplate),
  Google OAuth 2.0 (через RestTemplate)

## Хранение данных (polyglot persistence)

Проект использует две базы данных, каждую под свой тип данных:

- **PostgreSQL** — реляционные бизнес-данные: пользователи, машины, брони,
  платежи, отзывы, страховки, уведомления, чат
- **MongoDB GridFS** — документы пользователя (паспорт, водительское
  удостоверение): бинарные файлы с метаданными, для которых документная БД
  подходит лучше реляционной
- **Файловая система** — изображения (аватары, фото машин), раздаются
  статически через `WebConfig`

## Запуск через Docker

1. Скопировать переменные окружения:

```bash
cp .env.example .env
```

Заполнить `.env`:

```dotenv
OAUTH_GOOGLE_CLIENT_ID=your-google-client-id
OAUTH_GOOGLE_CLIENT_SECRET=your-google-client-secret
OAUTH_GOOGLE_REDIRECT_URI=http://localhost:8080/oauth/google/callback
```

2. Запустить контейнеры:

```bash
docker compose up --build
```

Поднимаются четыре сервиса: `app`, `postgres`, `mongo`, `ollama`.

3. Один раз авторизовать Ollama Cloud (для ИИ-помощника):

```bash
docker exec -it semestr_work3-ollama-1 ollama signin
```

Откроется браузер для логина на ollama.com. Токен сохранится в Docker volume,
повторно делать не нужно.

Приложение доступно на `http://localhost:8080`.

## Локальный запуск (через IDEA)

**Требования:** Java 21, Maven, PostgreSQL, MongoDB, Docker (для интеграционных тестов).

1. Создать БД PostgreSQL:
```sql
CREATE DATABASE carrent;
```

2. Поднять MongoDB (проще всего контейнером):
```bash
docker compose up -d mongo
```

3. Скопировать `.env`, заполнить Google OAuth (см. выше).

4. Проверить настройки в `application.properties`:
```properties
spring.datasource.url=jdbc:postgresql://localhost:5433/carrent
spring.datasource.username=postgres
spring.datasource.password=postgres
spring.mongodb.uri=mongodb://localhost:27017/carrent_docs
spring.mongodb.database=carrent_docs
spring.data.mongodb.gridfs.database=carrent_docs
app.upload.dir=${user.dir}/uploads
```

5. Запустить:
```bash
./mvnw spring-boot:run
```

Или через IDEA: запустить `SemestrWork3Application.main()`.

## Тестовые учётные записи

Создаются автоматически миграцией `V17__seed_default_users.sql`:

| Роль  | Логин  | Пароль |
|-------|--------|--------|
| Admin | admin  | admin  |
| User  | user   | user   |

## Архитектура безопасности

В `SecurityConfig.java` настроены **два независимых SecurityFilterChain**.

### MVC chain (`mvcFilterChain`, `@Order(2)`)
Для браузера и форм:
- Form Login (`/login`) с сессионной аутентификацией (JSESSIONID)
- BCrypt-хэширование паролей
- CSRF-защита включена для всех state-changing запросов
- Кастомные страницы ошибок 400 / 403 / 404 / 500 (`templates/error/`)
- **`anyRequest().authenticated()`** — всё что не описано явно как `permitAll()`,
  требует логина. Защита от случайных открытых endpoint'ов

### REST API chain (`apiFilterChain`, `@Order(1)`)
Для программных клиентов:
- HTTP Basic Auth (`Authorization: Basic <base64>`)
- Параллельная поддержка cookie-сессии через `requireCsrfProtectionMatcher`:
    - Браузер → cookie + CSRF-токен в `X-CSRF-TOKEN`
    - Postman/curl → Basic Auth, CSRF не применяется
- Кастомные `AuthenticationEntryPoint` и `AccessDeniedHandler` отдают JSON, не HTML
- ADMIN-only endpoint'ы для CRUD машин, подтверждения броней, refund

### Авторизация на уровне методов
Включена через `@EnableMethodSecurity`. Используется `@PreAuthorize` с SpEL и
bean-помощником `PaymentSecurity`:

```java
@PreAuthorize("hasAuthority('ADMIN') or @paymentSecurity.isOwner(#id, principal)")
public PaymentDto findById(@PathVariable Long id) { ... }
```

### OAuth 2.0
Google OAuth — собственная реализация через `RestTemplate`
(`OAuthService` / `OAuthController`), без специализированной библиотеки.
CSRF-защита через state-параметр.

## Защита бизнес-инвариантов

Критичные операции защищены от невалидных переходов состояний на уровне
сервисного слоя — независимо от того, через какой UI пришёл запрос:

- `PaymentService.pay()` — нельзя оплатить уже оплаченный или возвращённый платёж;
  способ оплаты проверяется по whitelist (`CARD`, `CASH`)
- `PaymentService.refund()` — идемпотентен (повторный refund не меняет статуса)
- `BookingService.create()` — проверка пересечения дат: машину нельзя забронировать
  на период, пересекающийся с уже существующей активной бронью
- `BookingService.confirm()` — только из `PENDING`
- `BookingService.cancel()` — проверка владельца (отменить может только автор брони);
  только из `PENDING` или `CONFIRMED`; для оплаченных карт ставит `REFUNDED`
  (реальный возврат), для PENDING — `CANCELLED` (отзыв запроса)
- `BookingService.complete()` — только из `CONFIRMED`
- `ReviewService.create()` — отзыв можно оставить **только после завершённой
  аренды этой машины этим пользователем**, не более одного на машину

При нарушении бросается `IllegalStateException` / `IllegalArgumentException`,
корректно обрабатываемый в `MvcExceptionHandler` (страница 400) и
`ApiExceptionHandler` (JSON 400).

## Отказоустойчивость (Spring Retry)

Вызовы ИИ-помощника (Ollama) обёрнуты в `OllamaClientService` с аннотацией
`@Retryable`: при сбое (`RestClientException`) выполняется до 3 повторных
попыток с задержкой 1 секунда между ними. Если все попытки исчерпаны,
срабатывает `@Recover`-метод, и пользователю возвращается понятное сообщение
вместо стектрейса.

## Swagger UI

Доступен только для администратора:
http://localhost:8080/swagger-ui.html

Поддерживается две схемы аутентификации **на выбор**:
- **Basic Auth** — для Postman/curl
- **Cookie (JSESSIONID)** — для браузера после логина

## Тестирование REST API

### IntelliJ IDEA HTTP Client
Файл `requests.http`, переменные в `http-client.env.json`.

### Postman
Коллекция `Aura_Motum.postman_collection.json` — запросы по папкам
(CARS / BOOKINGS / PAYMENTS / REVIEWS / FAVORITES / NOTIFICATIONS / CHAT / MVC).
Авторизация — Basic Auth на уровне коллекции через `{{username}}` / `{{password}}`.

## Тесты и покрытие

```bash
./mvnw verify
```

⚠️ Нужен **запущенный Docker** — Testcontainers поднимет PostgreSQL и MongoDB.

Отчёт JaCoCo: `target/site/jacoco/index.html`

### Структура тестов
- **Unit-тесты** (`src/test/java/.../service/`) — Mockito-моки, 100%
  покрытие сервисного слоя по строкам и **ветвлениям** (jacoco enforcer
  `<minimum>1.00</minimum>` для METHOD / LINE / BRANCH)
- **Интеграционные тесты** (`src/test/java/.../integration/*IT.java`) —
  поднимают полный Spring-контекст с реальными PostgreSQL и MongoDB через
  Testcontainers, прогоняют HTTP-запросы через MockMvc

## Структура проекта
```text
src/
├── main/
│   ├── java/ru/itis/semestr_work3/
│   │   ├── config/          — SecurityConfig (2 FilterChain), WebConfig,
│   │   │                       SwaggerConfig, AppConfig
│   │   ├── controllers/
│   │   │   ├── api/         — REST API (Swagger-аннотации)
│   │   │   └── (mvc)        — HomeController, BookingPageController,
│   │   │                       ProfilePageController, ChatPageController,
│   │   │                       PaymentPageController (read-only),
│   │   │                       DocumentController (отдача документов из GridFS),
│   │   │                       AdminBookingController, AdminCarController,
│   │   │                       AdminUserController, AuthController, OAuthController
│   │   ├── service/         — бизнес-логика, state guards,
│   │   │                       GridFsDocumentService (MongoDB),
│   │   │                       OllamaClientService (retry)
│   │   ├── repository/      — Spring Data JPA + @Query
│   │   ├── entity/          — 12 JPA сущностей (M2M, O2M, O2O связи)
│   │   ├── dto/             — DTO / Request / Form / Filter
│   │   ├── converter/       — MapStruct + кастомные String-конвертеры
│   │   ├── specifications/  — JPA Criteria (вкл. подзапрос)
│   │   ├── security/        — PaymentSecurity bean для @PreAuthorize
│   │   └── exception/       — MvcExceptionHandler, ApiExceptionHandler
│   └── resources/
│       ├── static/css|js/   — общие + per-page файлы
│       ├── templates/       — Thymeleaf с фрагментами header/footer
│       └── db/migration/    — Flyway V1—V18
└── test/
    └── java/ru/itis/semestr_work3/
        ├── service/         — unit-тесты (Mockito)
        ├── integration/     — интеграционные тесты с Testcontainers
        ├── TestcontainersConfiguration.java  — PostgreSQL + MongoDB контейнеры
        └── TestSemestrWork3Application.java
```

## Технические заметки

### Хранение файлов
- **Документы пользователя** (паспорт, права) — в **MongoDB GridFS**.
  `GridFsDocumentService` сохраняет файл, возвращает ObjectId, который
  записывается в поля `licensePath` / `passportPath` сущности User.
  Отдаются через `DocumentController` с проверкой прав доступа.
- **Изображения** (аватары, фото машин) — в файловой системе
  `${app.upload.dir}/{avatars,cars}/`, раздаются через
  `WebConfig.addResourceHandlers` под `/uploads/avatars/**` и `/uploads/cars/**`.

И `FileStorageService` (файловая система), и `GridFsDocumentService` (GridFS)
проверяют файл по whitelist MIME-типов и размеру (≤5 МБ). Для файловой системы
расширение вычисляется **строго по Content-Type**, а не по имени файла —
защита от загрузки `evil.html` с поддельным MIME.

### Переменные окружения
Чувствительные данные (Google OAuth) хранятся в `.env` (в `.gitignore`).
В репозитории — только `.env.example` с пустыми значениями.

### Docker
- `Dockerfile` — multi-stage build, запуск от non-root пользователя `spring`
- `.dockerignore` — исключает `target/`, `.idea/`, `.env`, `uploads/` и прочее
- `compose.yaml` — четыре сервиса: app, postgres, mongo, ollama;
  данные сохраняются в volumes `pgdata`, `mongodata`, `uploads`
- Образ приложения публикуется как `santrope888/semestr_work3:latest`; в `compose.yaml`
  он указан через `image:` и при необходимости пересобирается из `build: .`

### CI / pre-commit
```bash
./mvnw clean verify
```
Должно быть зелёным:
- Все unit-тесты
- Все интеграционные тесты (нужен Docker — PostgreSQL + MongoDB)
- JaCoCo enforcer 100% по METHOD / LINE / BRANCH для сервисов
