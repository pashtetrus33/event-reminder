package ru.bakanov.eventreminder.scheduler;

import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import ru.bakanov.eventreminder.config.ReminderMetrics;
import ru.bakanov.eventreminder.events.EventsAPI;
import ru.bakanov.eventreminder.notifications.NotificationsAPI;
import ru.bakanov.eventreminder.users.UsersAPI;

@Component
public class ReminderSchedulerJob {

    private static final Logger LOG = LoggerFactory.getLogger(ReminderSchedulerJob.class);

    private final NotificationsAPI notificationsAPI;
    private final EventsAPI eventsAPI;
    private final UsersAPI usersAPI;
    private final ReminderMetrics metrics;

    ReminderSchedulerJob(
            NotificationsAPI notificationsAPI, EventsAPI eventsAPI, UsersAPI usersAPI, ReminderMetrics metrics) {
        this.notificationsAPI = notificationsAPI;
        this.eventsAPI = eventsAPI;
        this.usersAPI = usersAPI;
        this.metrics = metrics;
    }

    @Scheduled(cron = "${app.scheduler.cron:0 * * * * *}")
    public void run() {
        LOG.debug("Running reminder scheduler job");
        metrics.schedulerTimer.record(() -> {
            try {
                notificationsAPI.processPending();
                metrics.setTotalEvents(eventsAPI.countAll());
                metrics.setTotalUsers(usersAPI.countAll());
                metrics.setPendingNotifications(notificationsAPI.countPending());
                metrics.setLastRunTimestamp(Instant.now().getEpochSecond());
            } catch (Exception e) {
                LOG.error("Scheduler job failed", e);
            }
        });
    }
}
