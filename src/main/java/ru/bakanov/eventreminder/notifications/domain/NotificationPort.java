package ru.bakanov.eventreminder.notifications.domain;

import ru.bakanov.eventreminder.notifications.domain.models.NotificationChannel;
import ru.bakanov.eventreminder.notifications.domain.models.NotifyResult;

public interface NotificationPort {

    NotificationChannel channel();

    NotifyResult send(String recipient, String subject, String htmlBody);
}
