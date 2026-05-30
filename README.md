# RemindMe — Event Reminder App

Многопользовательский планировщик событий с уведомлениями в **Telegram** и на **Email**, веб-интерфейсом, Telegram-ботом для управления событиями и мониторингом через Grafana.

---

## Возможности

- **Веб-интерфейс** — Bootstrap 5, тёмная тема, адаптивный дизайн
- **Повторяющиеся события** — каждые N минут / часов / дней / недель / месяцев / лет
- **Гибкие правила уведомлений** — за N минут до, в начало дня, с повтором через интервал
- **Telegram и Email** — уведомления с красивым форматированием, временем события и обратным отсчётом
- **Telegram-бот** — управление событиями прямо из мессенджера (`/list`, `/add`, `/delete`)
- **Метки** — цветные теги для группировки событий
- **Мониторинг** — Prometheus + Grafana дашборд, структурированные логи через Loki
- **Авто-загрузка `.env`** — работает «из коробки» для локальной разработки

---

## Быстрый старт

### 1. Клонировать и настроить

```bash
git clone <repo-url>
cd holiday-notificator

cp .env.example .env
# Отредактируй .env — вставь токен Telegram-бота и SMTP-данные
```

### 2. Запустить локально

```bash
./mvnw.cmd spring-boot:run        # Windows
./mvnw spring-boot:run            # Linux / macOS
```

Spring Boot автоматически поднимет PostgreSQL, Prometheus, Grafana и Loki через Docker Compose.

Открой: **http://localhost:8080**

### 3. Полный деплой в Docker

```bash
docker compose -f docker-compose.yml up -d
```

---

## Конфигурация

Скопируй `.env.example` в `.env` и заполни:

```env
# Telegram Bot (оставь пустым чтобы отключить)
TELEGRAM_BOT_TOKEN=1234567890:AAxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx

# SMTP (пример для mail.ru)
MAIL_HOST=smtp.mail.ru
MAIL_PORT=465
MAIL_USERNAME=your@mail.ru
MAIL_PASSWORD=your_app_password
MAIL_FROM=your@mail.ru
```

> **Важно:** `.env` находится в `.gitignore` — секреты не попадут в репозиторий.

### Все параметры `application.yml`

| Параметр | Env-переменная | По умолчанию |
|---|---|---|
| `app.telegram.bot-token` | `TELEGRAM_BOT_TOKEN` | *(пусто — Telegram отключён)* |
| `app.mail.from` | `MAIL_FROM` | `noreply@event-reminder.app` |
| `app.scheduler.cron` | — | `0 * * * * *` (каждую минуту) |
| `app.scheduler.start-of-day-hour` | — | `9` (глобальный дефолт) |
| `spring.datasource.*` | `SPRING_DATASOURCE_*` | `localhost:5432/event_reminder` |
| `spring.mail.*` | `MAIL_HOST/PORT/USERNAME/PASSWORD` | `localhost:25` |

---

## Telegram-бот

Бот работает через long polling — публичный URL не нужен.

### Настройка

1. Отправь боту `/start` — он покажет твой **Chat ID**
2. Вставь Chat ID в **Настройки профиля** на сайте
3. Включи галочку «Telegram-уведомления»

### Команды

| Команда | Описание |
|---|---|
| `/start`, `/help` | Справка и Chat ID |
| `/list` | Список всех событий с нумерацией |
| `/add Название ГГГГ-ММ-ДД ЧЧ:мм` | Создать событие |
| `/delete N` | Удалить событие №N из списка |

```
/add Встреча с командой 2026-06-15 10:30
```

> Время вводится в твоём локальном часовом поясе (определяется по событиям из браузера).

---

## Веб-интерфейс

| URL | Страница |
|---|---|
| `/login` | Вход |
| `/register` | Регистрация |
| `/` | Дашборд — ближайшие события (7 дней) |
| `/events` | Все события |
| `/events/new` | Создать событие |
| `/events/{id}/edit` | Редактировать событие |
| `/labels` | Управление метками |
| `/profile` | Настройки уведомлений и часового пояса |

---

## Уведомления

### Типы

| Тип | Описание |
|---|---|
| **За N минут до** | Срабатывает за указанное время до события |
| **В начало дня** | Срабатывает в настроенный час (по умолчанию 9:00 UTC) |

### Пример сообщения Telegram

```
🔔 Напоминание о событии!

📌 Встреча с командой

📝 Обсуждение квартального плана

📅 15 июня 2026, 10:30
⏳ До события осталось 3 минуты
```

### Повторяющийся напоминалка

В строке правила есть поле «повт. каждые N мин» — уведомление будет приходить повторно через заданный интервал вплоть до времени события.

---

## Мониторинг

После `docker compose up -d`:

- **Grafana:** http://localhost:3000 — логин `admin` / `admin`
- **Prometheus:** http://localhost:9090

### Метрики

| Метрика | Тип | Описание |
|---|---|---|
| `reminder_users` | Gauge | Всего пользователей |
| `reminder_events_total` | Gauge | Всего событий |
| `reminder_notifications_pending` | Gauge | Уведомлений в очереди |
| `reminder_notifications_sent_total` | Counter | Отправлено (`channel`, `status`) |
| `reminder_events_created_total` | Counter | Создано событий |
| `reminder_scheduler_duration_seconds` | Timer | Время работы планировщика |

---

## Разработка

### Команды

```bash
# Запустить приложение (авто-поднимает Docker-сервисы)
./mvnw spring-boot:run

# Тесты
./mvnw test

# Один тест-класс
./mvnw test -Dtest=NotificationServiceTest

# Форматирование кода
./mvnw spotless:apply

# Сборка + форматирование + тесты
./mvnw clean spotless:apply verify
```

### Требования

- Java 17+
- Docker Desktop (для PostgreSQL и сервисов мониторинга)
- Maven Wrapper включён — устанавливать Maven отдельно не нужно

---

## Архитектура

```
ru.bakanov.eventreminder/
├── config/          AppProperties, SecurityConfig, ReminderMetrics
├── shared/          DomainException, BaseEntity, IdGenerator, AssertUtil
├── users/           Регистрация, аутентификация, профиль
├── labels/          Управление цветными метками
├── events/          Создание, редактирование, повторение событий
├── notifications/   Правила, лог отправки, Email/Telegram адаптеры
├── web/             Thymeleaf-контроллеры (зависят от нескольких доменов)
├── scheduler/       Cron-задача (каждую минуту)
└── telegram/        Telegram-бот (long polling)
```

**Ключевые принципы:**
- Сущности и репозитории — package-private; межмодульные вызовы через `*API` фасады
- `web/` — единственное место, где можно зависеть от нескольких доменов одновременно
- Все секреты через env-переменные, `.env` автоматически подгружается при локальном запуске

---

## Стек технологий

| Слой | Технологии |
|---|---|
| Backend | Spring Boot 3.4.5, Java 17, Spring Security, Spring Data JPA |
| База данных | PostgreSQL 17, Liquibase, Hibernate |
| UI | Thymeleaf, Bootstrap 5, Bootstrap Icons |
| Уведомления | Spring Mail (SMTP), Telegram Bot API |
| Мониторинг | Micrometer, Prometheus, Grafana, Loki, Alloy |
| Тесты | JUnit 5, Mockito, Testcontainers, ArchUnit |
| Сборка | Maven, Spotless (Palantir Java Format) |
