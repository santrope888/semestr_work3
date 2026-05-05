# Aura Motum — Система аренды автомобилей

Учебный проект на Spring Boot. Платформа для аренды автомобилей с личным кабинетом, бронированием, оплатой и ИИ-помощником.

## Технологии

- **Backend:** Java 21, Spring Boot 4.x, Spring MVC, Spring Security, Spring Data JPA
- **Шаблонизация:** Thymeleaf
- **БД:** PostgreSQL 16, Flyway (миграции)
- **Маппинг:** MapStruct
- **Документация API:** springdoc-openapi (Swagger UI)
- **Тесты:** JUnit 5, Mockito, AssertJ, Spring Security Test, Testcontainers
- **Покрытие:** JaCoCo, enforcer на 100% сервисного слоя
- **Инфраструктура:** Docker Compose (app + postgres + ollama)
- **ИИ-помощник:** Ollama (локальная LLM) — без облачных API-ключей
- **Внешние API:** exchangerate-api (RestTemplate), Google OAuth 2.0 (RestTemplate)

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

Приложение доступно на `http://localhost:8080`.

Сервисы:
- `app` — Spring Boot приложение
- `postgres` — PostgreSQL
- `ollama` — локальная LLM для ИИ-помощника

## Локальный запуск (через IDEA / mvnw)

**Требования:** Java 21, Maven, PostgreSQL, Docker (для интеграционных тестов).

1. Создать БД:
```sql
CREATE DATABASE carrent;
```

2. Скопировать `.env`:
```bash
cp .env.example .env
```

3. Заполнить Google OAuth переменные в `.env` (см. выше).

4. Проверить настройки БД в `application.properties`:
```properties
spring.datasource.url=jdbc:postgresql://localhost:5433/carrent
spring.datasource.username=postgres
spring.datasource.password=postgres

# Папка с пользовательскими файлами (аватары, документы, фото машин)
app.upload.dir=${user.dir}/uploads
```

5. Запустить:
```bash
./mvnw spring-boot:run
```

Или через IDEA: запустить `SemestrWork3Application.main()`.

## Тестовые учётные записи

Создаются автоматически миграцией `V17__seed_default_users.sql` при первом старте.

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
- Кастомные страницы ошибок 400/403/404/500 (`templates/error/`)

### REST API chain (`apiFilterChain`, `@Order(1)`)
Для программных клиентов:
- HTTP Basic Auth (`Authorization: Basic <base64>`)
- Поддержка обоих сценариев одновременно через `requireCsrfProtectionMatcher`:
  - Браузер → cookie-сессия + CSRF-токен в `X-CSRF-TOKEN`
  - Postman/curl → Basic Auth, CSRF не применяется (нет cookie — нечего подделывать)
- Кастомные `AuthenticationEntryPoint` и `AccessDeniedHandler` отдают JSON, не HTML
- ADMIN-only endpoint'ы для CRUD машин, подтверждения броней, refund платежей

### Авторизация на уровне методов
Включена через `@EnableMethodSecurity`. Используется `@PreAuthorize` с SpEL и
bean-помощником `PaymentSecurity` для проверки владельца ресурса:

```java
@PreAuthorize("hasAuthority('ADMIN') or @paymentSecurity.isOwner(#id, principal)")
public PaymentDto findById(@PathVariable Long id) { ... }
```

### Дополнительно
- Google OAuth 2.0 — собственная реализация через `RestTemplate`
  (`OAuthService` / `OAuthController`), без специализированной библиотеки.
  CSRF-защита через state-параметр.
- Сторонний API курсов валют — через `RestTemplate` (`CurrencyService`).

## Защита бизнес-инвариантов

Критичные операции защищены от невалидных переходов состояний на уровне
сервисного слоя — независимо от того, через какой UI пришёл запрос:

- `PaymentService.pay()` — нельзя оплатить уже оплаченный или возвращённый платёж
- `PaymentService.refund()` — идемпотентен (повторный refund не меняет состояния)
- `BookingService.confirm()` — только из `PENDING`
- `BookingService.cancel()` — только из `PENDING` или `CONFIRMED`
- `BookingService.complete()` — только из `CONFIRMED`

При нарушении бросается `IllegalStateException`, корректно обрабатываемый в
`MvcExceptionHandler` (страница 400) и `ApiExceptionHandler` (JSON 400).

## Swagger UI

Доступен только для администратора:

```
http://localhost:8080/swagger-ui.html
```

## Тестирование REST API

### IntelliJ IDEA HTTP Client

Файл `requests.http` в корне проекта. Использует HTTP Basic Auth с
переменными `{{adminAuth}}` и `{{userAuth}}` из `http-client.env.json`.

1. Запустить приложение
2. Открыть `requests.http` в IDEA
3. Кликать ▶ слева от любого запроса

### Postman

Коллекция `Aura_Motum.postman_collection.json` — 46 запросов в 8 папках
(CARS / BOOKINGS / PAYMENTS / REVIEWS / FAVORITES / NOTIFICATIONS / CHAT / MVC).

Импорт: **Postman → File → Import → выбрать JSON-файл.**

Авторизация — Basic Auth на уровне коллекции через переменные
`{{username}}` / `{{password}}`. По умолчанию `admin/admin`; для проверки
ролевого разделения замените на `user/user` в `Variables` коллекции.

## Тесты и покрытие

```bash
# Все тесты (unit + интеграционные)
./mvnw verify
```

⚠️ Для интеграционных тестов нужен **запущенный Docker** — Testcontainers
поднимет PostgreSQL автоматически.

Отчёт JaCoCo:
```
target/site/jacoco/index.html
```

### Структура тестов

- **Unit-тесты** (`src/test/java/.../service/`) — Mockito-моки, тестируют
  сервисный слой в изоляции. Покрытие сервисов 100% по методам и строкам
  (jacoco-maven-plugin `<minimum>1.00</minimum>`).
- **Интеграционные тесты** (`src/test/java/.../integration/*IT.java`) —
  поднимают полный Spring-контекст с реальной PostgreSQL через Testcontainers,
  прогоняют HTTP-запросы через MockMvc. Проверяют end-to-end:
  фильтры безопасности → контроллер → сервис → БД (Flyway-миграции).

## Структура проекта

```
src/
├── main/
│   ├── java/ru/itis/semestr_work3/
│   │   ├── config/          — SecurityConfig, WebConfig, SwaggerConfig, AppConfig
│   │   ├── controllers/
│   │   │   ├── api/         — REST API контроллеры (Swagger-аннотации)
│   │   │   └── (mvc)        — HomeController, BookingPageController,
│   │   │                       ProfilePageController, ChatPageController,
│   │   │                       PaymentPageController (read-only),
│   │   │                       AdminBookingController, AdminCarController,
│   │   │                       AdminUserController, AuthController, OAuthController
│   │   ├── service/         — бизнес-логика, защита инвариантов состояний
│   │   ├── repository/      — Spring Data JPA + кастомные @Query
│   │   ├── entity/          — 12 JPA сущностей (M2M, O2M, O2O связи)
│   │   ├── dto/             — DTO / Request / Form / Filter классы
│   │   ├── converter/       — MapStruct mappers + кастомные String-конвертеры
│   │   ├── specifications/  — JPA Criteria Specifications (вкл. подзапрос)
│   │   ├── security/        — PaymentSecurity bean для @PreAuthorize SpEL
│   │   └── exception/       — кастомные исключения, MvcExceptionHandler,
│   │                           ApiExceptionHandler (JSON для /api/**)
│   └── resources/
│       ├── static/css/      — общий style.css + per-page CSS
│       ├── static/js/       — AJAX-фронт (fetch + CSRF из meta-тегов)
│       ├── templates/       — Thymeleaf (с фрагментами header/footer)
│       └── db/migration/    — Flyway V1–V18
└── test/
    ├── java/ru/itis/semestr_work3/
    │   ├── service/         — unit-тесты сервисов (Mockito)
    │   ├── integration/     — интеграционные тесты с Testcontainers
    │   ├── TestcontainersConfiguration.java
    │   └── TestSemestrWork3Application.java
```

## Технические заметки

### Файловое хранилище

Загруженные пользователем файлы (аватары, документы, фото машин) сохраняются
на диске в `${app.upload.dir}/{avatars,cars,documents}/`. По умолчанию это
`${user.dir}/uploads`. Раздаются через `WebConfig.addResourceHandlers` под
URL-префиксами `/uploads/avatars/**` и `/uploads/cars/**`. При старте
приложения путь логируется (см. `WebConfig.logUploadDir`).

### Переменные окружения

Чувствительные данные (Google OAuth) хранятся в `.env` (в `.gitignore`).
В репозитории — только `.env.example` с пустыми значениями.

⚠️ Если ваш OAuth secret когда-либо попадал в git-историю — обязательно
перевыпустить его в Google Cloud Console.

### CI / pre-commit чек

```bash
./mvnw clean verify
```

Должно быть зелёным:
- Все unit-тесты
- Все интеграционные тесты
- JaCoCo enforcer на 100% сервисного слоя
- Без warnings от компилятора (кроме известных deprecated в legacy-коде)
