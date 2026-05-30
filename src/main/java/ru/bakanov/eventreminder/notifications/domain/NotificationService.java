package ru.bakanov.eventreminder.notifications.domain;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.bakanov.eventreminder.config.AppProperties;
import ru.bakanov.eventreminder.notifications.domain.models.NotificationChannel;
import ru.bakanov.eventreminder.notifications.domain.models.NotificationType;
import ru.bakanov.eventreminder.notifications.domain.models.PendingLogInfo;
import ru.bakanov.eventreminder.notifications.domain.models.RuleInfo;
import ru.bakanov.eventreminder.shared.exception.ResourceNotFoundException;

@Service
public class NotificationService {

    private static final Logger LOG = LoggerFactory.getLogger(NotificationService.class);
    private static final long LOOKBACK_MINUTES = 5;
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern(
                    "d MMMM yyyy, HH:mm", Locale.forLanguageTag("ru"))
            .withZone(ZoneOffset.UTC);

    private final NotificationRuleRepository ruleRepository;
    private final NotificationLogRepository logRepository;
    private final Map<NotificationChannel, NotificationPort> adapters;
    private final AppProperties appProperties;

    NotificationService(
            NotificationRuleRepository ruleRepository,
            NotificationLogRepository logRepository,
            List<NotificationPort> ports,
            AppProperties appProperties) {
        this.ruleRepository = ruleRepository;
        this.logRepository = logRepository;
        this.adapters = ports.stream().collect(Collectors.toMap(NotificationPort::channel, Function.identity()));
        this.appProperties = appProperties;
    }

    @Transactional
    public RuleInfo createRule(
            String eventId,
            NotificationChannel channel,
            NotificationType notifyType,
            Integer notifyBeforeMinutes,
            Integer repeatIntervalMinutes,
            Instant eventAt,
            int startOfDayHour) {
        var rule = ruleRepository.save(
                new NotificationRuleEntity(eventId, channel, notifyType, notifyBeforeMinutes, repeatIntervalMinutes));
        scheduleLog(rule, eventAt, startOfDayHour);
        return toRuleInfo(rule);
    }

    @Transactional(readOnly = true)
    public List<RuleInfo> getRulesForEvent(String eventId) {
        return ruleRepository.findAllByEventId(eventId).stream()
                .map(this::toRuleInfo)
                .toList();
    }

    @Transactional
    public void deleteRulesForEvent(String eventId) {
        ruleRepository.deleteAllByEventId(eventId);
    }

    @Transactional(readOnly = true)
    public List<PendingLogInfo> findPendingDue() {
        Instant now = Instant.now();
        return logRepository.findPendingDue(now, now.minus(LOOKBACK_MINUTES, ChronoUnit.MINUTES)).stream()
                .map(l -> new PendingLogInfo(
                        l.getId(), l.getEventId(), l.getChannel(), l.getScheduledFor(), l.getEventAt()))
                .toList();
    }

    @Transactional
    public boolean dispatch(
            String logId,
            String recipient,
            String eventTitle,
            String eventDescription,
            Instant eventAt,
            String timezone) {
        var log = logRepository
                .findById(logId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification log not found: " + logId));
        if (recipient == null || recipient.isBlank()) {
            log.markFailed("Recipient not configured");
            logRepository.save(log);
            return false;
        }
        var adapter = adapters.get(log.getChannel());
        if (adapter == null) {
            log.markFailed("No adapter for channel: " + log.getChannel());
            logRepository.save(log);
            return false;
        }
        try {
            var result = adapter.send(
                    recipient,
                    "🔔 Напоминание: " + eventTitle,
                    buildBody(eventTitle, eventDescription, eventAt, timezone, log.getChannel()));
            if (result.success()) {
                log.markSent();
            } else {
                log.markFailed(result.errorMessage());
            }
        } catch (Exception e) {
            LOG.error("Failed to send notification {}", logId, e);
            log.markFailed(e.getMessage());
        }
        logRepository.save(log);
        scheduleRepeat(log, eventAt);
        return "SENT".equals(log.getStatus());
    }

    @Transactional(readOnly = true)
    public long countPending() {
        return logRepository.countByStatus("PENDING");
    }

    @Transactional(readOnly = true)
    public NotificationRuleEntity getRuleEntity(String ruleId) {
        return ruleRepository
                .findById(ruleId)
                .orElseThrow(() -> new ResourceNotFoundException("Rule not found: " + ruleId));
    }

    @Transactional
    public void scheduleNextOccurrence(String ruleId, Instant nextOccurrenceAt, int startOfDayHour) {
        var rule = getRuleEntity(ruleId);
        Instant scheduledFor = computeScheduledFor(rule, nextOccurrenceAt, startOfDayHour);
        if (!scheduledFor.isBefore(Instant.now())) {
            logRepository.save(new NotificationLogEntity(
                    rule.getEventId(), rule.getId(), rule.getChannel(), scheduledFor, nextOccurrenceAt));
        }
    }

    Instant computeScheduledFor(NotificationRuleEntity rule, Instant eventAt, int startOfDayHour) {
        if (rule.getNotifyType() == NotificationType.START_OF_DAY) {
            return eventAt.atZone(ZoneOffset.UTC)
                    .toLocalDate()
                    .atTime(startOfDayHour, 0)
                    .toInstant(ZoneOffset.UTC);
        }
        int minutes = rule.getNotifyBeforeMinutes() != null ? rule.getNotifyBeforeMinutes() : 0;
        return eventAt.minus(minutes, ChronoUnit.MINUTES);
    }

    private void scheduleLog(NotificationRuleEntity rule, Instant eventAt, int startOfDayHour) {
        Instant scheduledFor = computeScheduledFor(rule, eventAt, startOfDayHour);
        if (!scheduledFor.isBefore(Instant.now())) {
            logRepository.save(new NotificationLogEntity(
                    rule.getEventId(), rule.getId(), rule.getChannel(), scheduledFor, eventAt));
        }
    }

    private String buildBody(
            String eventTitle, String eventDescription, Instant eventAt, String timezone, NotificationChannel channel) {
        ZoneId zone = resolveZone(timezone);
        return channel == NotificationChannel.TELEGRAM
                ? buildTelegramBody(eventTitle, eventDescription, eventAt, zone)
                : buildEmailBody(eventTitle, eventDescription, eventAt, zone);
    }

    private String buildTelegramBody(String eventTitle, String eventDescription, Instant eventAt, ZoneId zone) {
        var sb = new StringBuilder();
        sb.append("🔔 <b>Напоминание о событии!</b>\n\n");
        sb.append("📌 <b>").append(eventTitle).append("</b>");
        if (eventDescription != null && !eventDescription.isBlank()) {
            sb.append("\n\n📝 ").append(eventDescription);
        }
        sb.append("\n\n📅 ").append(DATE_FMT.withZone(zone).format(eventAt));
        sb.append("\n").append(formatTimeRemaining(eventAt));
        return sb.toString();
    }

    private String buildEmailBody(String eventTitle, String eventDescription, Instant eventAt, ZoneId zone) {
        String desc = (eventDescription != null && !eventDescription.isBlank())
                ? "<p style='color:#555;font-size:15px;margin:0 0 16px'>📝 " + eventDescription + "</p>"
                : "";
        return """
                <div style='font-family:Arial,sans-serif;background:#f5f5f5;padding:32px'>
                  <div style='background:#fff;border-radius:10px;padding:28px;max-width:480px;
                              margin:0 auto;border-left:4px solid #8b5cf6;box-shadow:0 2px 8px rgba(0,0,0,.08)'>
                    <h2 style='color:#8b5cf6;margin:0 0 20px'>🔔 Напоминание о событии!</h2>
                    <h3 style='color:#222;margin:0 0 12px'>📌 %s</h3>
                    %s
                    <p style='color:#555;margin:0 0 8px'>📅 %s</p>
                    <p style='color:#8b5cf6;font-weight:bold;margin:0 0 20px'>%s</p>
                    <hr style='border:none;border-top:1px solid #eee;margin:20px 0'>
                    <p style='color:#999;font-size:13px;margin:0'>RemindMe — не пропусти важное!</p>
                  </div>
                </div>""".formatted(eventTitle, desc, DATE_FMT.withZone(zone).format(eventAt), formatTimeRemaining(eventAt));
    }

    private String formatTimeRemaining(Instant eventAt) {
        long seconds = ChronoUnit.SECONDS.between(Instant.now(), eventAt);
        if (seconds <= 0) return "🎉 Событие уже началось!";
        long minutes = (seconds + 59) / 60;
        if (minutes == 0) return "⏳ Осталось меньше минуты";
        if (minutes < 60) return "⏳ До события осталось " + minutes + " " + minuteWord(minutes);
        long hours = minutes / 60;
        long mins = minutes % 60;
        String result = "⏳ До события осталось " + hours + " " + hourWord(hours);
        if (mins > 0) result += " " + mins + " " + minuteWord(mins);
        return result;
    }

    private void scheduleRepeat(NotificationLogEntity log, Instant eventAt) {
        var rule = ruleRepository.findById(log.getRuleId()).orElse(null);
        if (rule == null || rule.getRepeatIntervalMinutes() == null) return;
        Instant next = log.getScheduledFor().plus(rule.getRepeatIntervalMinutes(), ChronoUnit.MINUTES);
        if (!next.isAfter(eventAt)) {
            logRepository.save(
                    new NotificationLogEntity(rule.getEventId(), rule.getId(), rule.getChannel(), next, eventAt));
        }
    }

    private ZoneId resolveZone(String timezone) {
        if (timezone == null || timezone.isBlank()) return ZoneOffset.UTC;
        try {
            return ZoneId.of(timezone);
        } catch (Exception e) {
            return ZoneOffset.UTC;
        }
    }

    private String minuteWord(long n) {
        long mod100 = n % 100;
        if (mod100 >= 11 && mod100 <= 19) return "минут";
        return switch ((int) (n % 10)) {
            case 1 -> "минута";
            case 2, 3, 4 -> "минуты";
            default -> "минут";
        };
    }

    private String hourWord(long n) {
        long mod100 = n % 100;
        if (mod100 >= 11 && mod100 <= 19) return "часов";
        return switch ((int) (n % 10)) {
            case 1 -> "час";
            case 2, 3, 4 -> "часа";
            default -> "часов";
        };
    }

    private RuleInfo toRuleInfo(NotificationRuleEntity e) {
        return new RuleInfo(
                e.getId(),
                e.getEventId(),
                e.getChannel(),
                e.getNotifyType(),
                e.getNotifyBeforeMinutes(),
                e.getRepeatIntervalMinutes(),
                e.isActive());
    }
}
