package ru.bakanov.eventreminder.scheduler;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.bakanov.eventreminder.config.ReminderMetrics;
import ru.bakanov.eventreminder.events.EventsAPI;
import ru.bakanov.eventreminder.notifications.NotificationsAPI;
import ru.bakanov.eventreminder.users.UsersAPI;

@ExtendWith(MockitoExtension.class)
class ReminderSchedulerJobTest {

    @Mock
    NotificationsAPI notificationsAPI;

    @Mock
    EventsAPI eventsAPI;

    @Mock
    UsersAPI usersAPI;

    ReminderSchedulerJob job;

    @BeforeEach
    void setUp() {
        var metrics = new ReminderMetrics(new SimpleMeterRegistry());
        job = new ReminderSchedulerJob(notificationsAPI, eventsAPI, usersAPI, metrics);
    }

    @Test
    void run_callsProcessPending() {
        job.run();
        verify(notificationsAPI).processPending();
    }

    @Test
    void run_doesNotPropagateException() {
        doThrow(new RuntimeException("DB down")).when(notificationsAPI).processPending();
        job.run();
        // No exception thrown — scheduler must be resilient
    }
}
