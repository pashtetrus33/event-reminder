# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Commands

```powershell
# Format + build + test (full verification)
./mvnw.cmd clean spotless:apply verify

# Run tests only
./mvnw.cmd test

# Run a single test class
./mvnw.cmd test -Dtest=NotificationServiceTest

# Run the app locally (Spring Boot auto-starts compose.yml services)
./mvnw.cmd spring-boot:run

# Format code
./mvnw.cmd spotless:apply

# Full deployment with Docker Compose (needs .env file)
docker compose -f docker-compose.yml up -d

# Local dev (postgres + prometheus + grafana + loki only)
docker compose up -d
```

## Architecture

Spring Boot 3.4.5 / Java 17 / Thymeleaf application. Multi-user event reminder with Telegram and email notifications, a Bootstrap 5 dark-theme UI, cron-based notification scheduler, and Prometheus metrics for Grafana.

### Package structure (domain-driven, modular)

```
ru.bakanov.eventreminder/
├── EventReminderApplication.java         @SpringBootApplication + @EnableScheduling + @ConfigurationPropertiesScan
├── shared/
│   ├── exception/    DomainException, ResourceNotFoundException
│   └── persistence/  BaseEntity (auditing), IdGenerator (TSID), AssertUtil
├── users/
│   ├── domain/       UserEntity, UserRepository, UserService, UserDetailsServiceImpl
│   │   └── models/   UserInfo
│   ├── rest/
│   │   ├── controllers/ AuthController, ProfileController
│   │   └── dtos/        RegisterRequest, UserProfileDto
│   └── UsersAPI.java
├── labels/
│   ├── domain/       LabelEntity, LabelRepository, LabelService
│   │   └── models/   LabelInfo
│   ├── rest/
│   │   └── controllers/ LabelController
│   └── LabelsAPI.java
├── events/
│   ├── domain/       EventEntity, EventRepository, EventService
│   │   └── models/   EventInfo, RecurrenceType
│   └── EventsAPI.java
├── notifications/
│   ├── domain/       NotificationRuleEntity, NotificationLogEntity, NotificationRuleRepository,
│   │                 NotificationLogRepository, NotificationService, NotificationPort
│   │   └── models/   NotificationChannel, NotificationType, NotifyResult, RuleInfo, PendingLogInfo
│   ├── email/        EmailNotificationAdapter (implements NotificationPort)
│   ├── telegram/     TelegramNotificationAdapter (implements NotificationPort)
│   └── NotificationsAPI.java
├── web/
│   └── controllers/  EventController, DashboardController  ← depends on both events+notifications
├── scheduler/        ReminderSchedulerJob (@Scheduled every minute)
└── config/           AppProperties, SecurityConfig, JpaConfig, WebConfig,
                      GlobalExceptionHandler, ReminderMetrics
```

### Key conventions

- **Entities, repositories** are package-private; inter-module calls go through `*API` facades.
- **Services** use `@Transactional` for writes, `@Transactional(readOnly = true)` for reads.
- **`web.controllers`** package is the only place allowed to depend on multiple domain modules simultaneously — this is intentional and how the cycle between `events` and `notifications` is avoided.
- **Jackson**: `com.fasterxml.jackson.*` (standard SB 3.x — NOT `tools.jackson`).
- **Naming**: `*Entity`, `*Repository`, `*Service`, `*Controller`, `*API`.
- **`RestClient.Builder`** is configured in `WebConfig`; inject and call `.baseUrl().build()` in constructors.

### Domain: Events

- `EventEntity` stores label IDs as `@ElementCollection` (no `@ManyToMany` to avoid cross-module entity references).
- `next_occurrence_at` is updated by the scheduler after each recurring notification fires.
- `recurrence_type`: `NONE | DAILY | WEEKLY | MONTHLY | YEARLY`.

### Domain: Notifications

- `NotificationRuleEntity` — per-event rule (channel + timing).
- `NotificationLogEntity` — scheduled dispatch entries (`status: PENDING → SENT/FAILED`).
- Scheduler runs every minute, finds `PENDING` entries where `scheduled_for <= now`, dispatches, and for recurring events creates the next log entries.
- Notification timing: `BEFORE` subtracts `notify_before_minutes` from `event_at`; `START_OF_DAY` uses 09:00 UTC on the event day (configurable via `app.scheduler.start-of-day-hour`).

### Configuration (env vars or application.yml)

| Property | Env override | Default |
|---|---|---|
| `app.telegram.bot-token` | `TELEGRAM_BOT_TOKEN` | *(empty — disables Telegram)* |
| `app.mail.from` | `MAIL_FROM` | `noreply@event-reminder.app` |
| `app.scheduler.cron` | — | `0 * * * * *` (every minute) |
| `app.scheduler.start-of-day-hour` | — | `9` |
| `spring.mail.*` | `MAIL_HOST/PORT/USERNAME/PASSWORD` | localhost:25 |
| `spring.datasource.*` | `SPRING_DATASOURCE_*` | `localhost:5432/event_reminder` |

### UI (Thymeleaf + Bootstrap 5)

| URL | Page |
|---|---|
| `/login` | Login |
| `/register` | Registration |
| `/` | Dashboard — upcoming events (7 days) |
| `/events` | All events list |
| `/events/new` | Create event + notification rules |
| `/events/{id}/edit` | Edit event + rules |
| `/labels` | Manage labels (AJAX CRUD) |
| `/profile` | Notifications settings, change password |

- Theme: dark (`data-bs-theme="dark"`), purple accent (`#8b5cf6`).
- Static: `src/main/resources/static/css/app.css`, `static/js/app.js`.
- Navbar fragment: `templates/layout/navbar.html`.
- CSRF token exposed via `<meta name="_csrf">` for AJAX fetch calls.

### Infrastructure

| File | Purpose |
|---|---|
| `compose.yml` | Dev — Spring Boot auto-starts postgres + prometheus + grafana + loki + promtail |
| `docker-compose.yml` | Full deployment — includes `app` container |
| `docker/prometheus/prometheus*.yml` | Scrape configs |
| `docker/loki/loki-config.yml` | Single-process Loki |
| `docker/promtail/promtail-dev-config.yml` | Tails `./logs/app.json` |
| `.env.example` | Template for secrets |

### Metrics (ReminderMetrics)

| Metric | Type |
|---|---|
| `reminder.notifications.sent` | Counter (`channel`, `status` tags) |
| `reminder.events.total` | Gauge |
| `reminder.users.total` | Gauge |
| `reminder.notifications.pending` | Gauge |
| `reminder.scheduler.duration` | Timer |
| `reminder.last.run.timestamp` | Gauge |
| `reminder.events.created` | Counter |

### Tests

- Unit tests mock dependencies with Mockito; `AppProperties` is a record — use a real instance, not a mock.
- Integration tests extend `BaseIT`, use `@ActiveProfiles("test")`, Testcontainers PostgreSQL, and `MockMvc`.
- ArchUnit (`ArchUnitTests`) enforces: no deprecated APIs, no generic exceptions, utility class conventions, no import cycles, logger naming, Spring service/repository naming.
- Testcontainers 2.x API: `org.testcontainers.containers.PostgreSQLContainer` (SB 3.x path).
- Mockito config: `src/test/resources/mockito-extensions/org.mockito.plugins.MockMaker` = `mock-maker-subclass` (Java 26 compatibility).

### Grafana access

After `docker compose up -d`: http://localhost:3000 — user `admin` / `admin`
