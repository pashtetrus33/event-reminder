package ru.bakanov.eventreminder.config;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.stereotype.Component;

@Component
public class ReminderMetrics {

    private final AtomicLong totalEvents = new AtomicLong(0);
    private final AtomicLong totalUsers = new AtomicLong(0);
    private final AtomicLong pendingNotifications = new AtomicLong(0);
    private final AtomicLong lastRunTimestamp = new AtomicLong(0);

    private final Counter eventsCreated;
    private final MeterRegistry registry;
    public final Timer schedulerTimer;

    public ReminderMetrics(MeterRegistry registry) {
        this.registry = registry;

        registry.gauge("reminder.events.total", totalEvents, AtomicLong::get);
        registry.gauge("reminder.users.total", totalUsers, AtomicLong::get);
        registry.gauge("reminder.notifications.pending", pendingNotifications, AtomicLong::get);
        registry.gauge("reminder.last.run.timestamp", lastRunTimestamp, AtomicLong::get);

        this.eventsCreated = Counter.builder("reminder.events.created")
                .description("Total events created")
                .register(registry);

        this.schedulerTimer = Timer.builder("reminder.scheduler.duration")
                .description("Scheduler job execution time")
                .register(registry);
    }

    public void incrementEventsCreated() {
        eventsCreated.increment();
    }

    public void recordNotificationSent(String channel, boolean success) {
        Counter.builder("reminder.notifications.sent")
                .tag("channel", channel)
                .tag("status", success ? "success" : "error")
                .register(registry)
                .increment();
    }

    public void setTotalEvents(long count) {
        totalEvents.set(count);
    }

    public void setTotalUsers(long count) {
        totalUsers.set(count);
    }

    public void setPendingNotifications(long count) {
        pendingNotifications.set(count);
    }

    public void setLastRunTimestamp(long epochSeconds) {
        lastRunTimestamp.set(epochSeconds);
    }
}
