package ru.bakanov.eventreminder.notifications.domain.models;

public record RuleInfo(
        String id,
        String eventId,
        NotificationChannel channel,
        NotificationType notifyType,
        Integer notifyBeforeMinutes,
        Integer repeatIntervalMinutes,
        boolean active) {}
