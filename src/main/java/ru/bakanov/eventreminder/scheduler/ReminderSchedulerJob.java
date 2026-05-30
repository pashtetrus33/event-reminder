package ru.bakanov.eventreminder.scheduler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import ru.bakanov.eventreminder.notifications.NotificationsAPI;

@Component
public class ReminderSchedulerJob {

    private static final Logger LOG = LoggerFactory.getLogger(ReminderSchedulerJob.class);

    private final NotificationsAPI notificationsAPI;

    ReminderSchedulerJob(NotificationsAPI notificationsAPI) {
        this.notificationsAPI = notificationsAPI;
    }

    @Scheduled(cron = "${app.scheduler.cron:0 * * * * *}")
    public void run() {
        LOG.debug("Running reminder scheduler job");
        try {
            notificationsAPI.processPending();
        } catch (Exception e) {
            LOG.error("Scheduler job failed", e);
        }
    }
}
