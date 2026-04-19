# Aura Motum — Система аренды автомобилей

Учебный проект на Spring Boot. Платформа для аренды автомобилей с личным кабинетом, бронированием, оплатой и ИИ-помощником.

## Технологии

- Java 25, Spring Boot 4.x
- Spring MVC, Spring Security, Spring Data JPA
- Thymeleaf, PostgreSQL, Flyway
- Docker Compose, Ollama (локальный ИИ)
- springdoc-openapi / Swagger UI

## Запуск через Docker

```bash
docker compose up --build
```

Приложение будет доступно на `http://localhost:8080`

Сервисы:
- `app` — Spring Boot приложение
- `postgres` — база данных PostgreSQL
- `ollama` — локальная LLM для ИИ-помощника

## Локальный запуск

**Требования:** Java 25, Maven, PostgreSQL

1. Создать базу данных:
```sql
CREATE DATABASE carrent;
```

2. Проверить настройки в `application.properties`:
```properties
spring.datasource.url=jdbc:postgresql://localhost:5433/carrent
spring.datasource.username=postgres
spring.datasource.password=postgres
```

3. Запустить приложение:
```bash
./mvnw spring-boot:run
```

Приложение доступно на `http://localhost:8080`

## Тестовые данные

| Роль  | Логин  | Пароль |
|-------|--------|--------|
| Admin | admin  | admin  |
| User  | user   | user   |

## Swagger UI

Доступен только для администратора:

```
http://localhost:8080/swagger-ui.html
```

## Тестирование API

Готовая коллекция запросов в файле `requests.http` в корне проекта.

Порядок работы:
1. Запустить приложение
2. Выполнить **Login** запрос из раздела `AUTH` в `requests.http`
3. Выполнять остальные запросы в той же сессии IDEA

## Тесты и покрытие

```bash
./mvnw verify
```

Отчёт JaCoCo после выполнения:
```
target/site/jacoco/index.html
```

Покрытие сервисного слоя — 100% по методам и строкам (настроено через jacoco-maven-plugin в `pom.xml`).

## Структура проекта

```
src/
├── main/
│   ├── java/ru/itis/semestr_work3/
│   │   ├── config/          — Security, Web, Swagger
│   │   ├── controllers/     — MVC контроллеры
│   │   │   └── api/         — REST API контроллеры
│   │   ├── service/         — бизнес-логика
│   │   ├── repository/      — Spring Data JPA
│   │   ├── entity/          — JPA сущности (12 штук)
│   │   ├── dto/             — DTO и Request/Form классы
│   │   ├── converter/       — MapStruct mappers
│   │   ├── specifications/  — JPA Criteria Specifications
│   │   └── exception/       — обработка ошибок
│   └── resources/
│       ├── templates/       — Thymeleaf шаблоны
│       └── db/migration/    — Flyway миграции V1–V13
└── test/                    — Unit-тесты сервисов
```
