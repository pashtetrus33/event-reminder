package ru.bakanov.eventreminder.notifications;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import ru.bakanov.eventreminder.config.ReminderMetrics;
import ru.bakanov.eventreminder.events.EventsAPI;
import ru.bakanov.eventreminder.events.domain.models.EventInfo;
import ru.bakanov.eventreminder.events.domain.models.RecurrenceType;
import ru.bakanov.eventreminder.notifications.domain.NotificationService;
import ru.bakanov.eventreminder.notifications.domain.models.NotificationChannel;
import ru.bakanov.eventreminder.notifications.domain.models.NotificationType;
import ru.bakanov.eventreminder.notifications.domain.models.PendingLogInfo;
import ru.bakanov.eventreminder.notifications.domain.models.RuleInfo;
import ru.bakanov.eventreminder.users.UsersAPI;

@Service
public class NotificationsAPI {

    private static final Logger LOG = LoggerFactory.getLogger(NotificationsAPI.class);

    private final NotificationService notificationService;
    private final EventsAPI eventsAPI;
    private final UsersAPI usersAPI;
    private final ReminderMetrics metrics;

    public NotificationsAPI(
            NotificationService notificationService, EventsAPI eventsAPI, UsersAPI usersAPI, ReminderMetrics metrics) {
        this.notificationService = notificationService;
        this.eventsAPI = eventsAPI;
        this.usersAPI = usersAPI;
        this.metrics = metrics;
    }

    public RuleInfo createRule(
            String eventId,
            NotificationChannel channel,
            NotificationType notifyType,
            Integer notifyBeforeMinutes,
            Integer repeatIntervalMinutes,
            Instant eventAt,
            int startOfDayHour) {
        return notificationService.createRule(
                eventId, channel, notifyType, notifyBeforeMinutes, repeatIntervalMinutes, eventAt, startOfDayHour);
    }

    public List<RuleInfo> getRulesForEvent(String eventId) {
        return notificationService.getRulesForEvent(eventId);
    }

    public void deleteRulesForEvent(String eventId) {
        notificationService.deleteRulesForEvent(eventId);
    }

    public void processPending() {
        List<PendingLogInfo> pending = notificationService.findPendingDue();
        LOG.debug("Processing {} pending notifications", pending.size());

        for (var log : pending) {
            try {
                var event = eventsAPI.getByIdInternal(log.eventId());
                var user = usersAPI.getById(event.userId());
                String recipient = log.channel() == NotificationChannel.EMAIL
                        ? (user.emailNotificationsEnabled() ? user.email() : null)
                        : (user.telegramNotificationsEnabled() ? user.telegramChatId() : null);

                Instant eventAt = log.eventAt() != null
                        ? log.eventAt()
                        : (event.nextOccurrenceAt() != null ? event.nextOccurrenceAt() : event.eventAt());
                int startOfDayHour = user.startOfDayHour();
                boolean sent = notificationService.dispatch(
                        log.logId(), recipient, event.title(), event.description(), eventAt, event.timezone());
                metrics.recordNotificationSent(log.channel().name(), sent);

                if (event.recurring()) {
                    scheduleNextOccurrence(event, startOfDayHour);
                }
            } catch (Exception e) {
                LOG.error("Error processing notification log {}", log.logId(), e);
            }
        }
    }

    public long countPending() {
        return notificationService.countPending();
    }

    private void scheduleNextOccurrence(EventInfo event, int startOfDayHour) {
        Instant nextOccurrence = computeNext(event);
        if (nextOccurrence == null) return;
        if (event.recurrenceEndAt() != null && nextOccurrence.isAfter(event.recurrenceEndAt())) return;

        eventsAPI.advanceOccurrence(event.id(), nextOccurrence);

        for (var rule : notificationService.getRulesForEvent(event.id())) {
            notificationService.scheduleNextOccurrence(rule.id(), nextOccurrence, startOfDayHour);
        }
    }

    private Instant computeNext(EventInfo event) {
        Instant base = event.nextOccurrenceAt();
        if (event.recurrenceType() == null || event.recurrenceType() == RecurrenceType.NONE) return null;
        return switch (event.recurrenceType()) {
            case MINUTELY -> base.plus(event.recurrenceInterval(), ChronoUnit.MINUTES);
            case HOURLY -> base.plus(event.recurrenceInterval(), ChronoUnit.HOURS);
            case DAILY -> base.plus(event.recurrenceInterval(), ChronoUnit.DAYS);
            case WEEKLY -> base.plus(event.recurrenceInterval() * 7L, ChronoUnit.DAYS);
            case MONTHLY ->
                base.atZone(ZoneOffset.UTC)
                        .plusMonths(event.recurrenceInterval())
                        .toInstant();
            case YEARLY ->
                base.atZone(ZoneOffset.UTC)
                        .plusYears(event.recurrenceInterval())
                        .toInstant();
            default -> null;
        };
    }
}
