package ru.bakanov.eventreminder.notifications.domain.models;

import java.time.Instant;

public record PendingLogInfo(
        String logId, String eventId, NotificationChannel channel, Instant scheduledFor, Instant eventAt) {}
