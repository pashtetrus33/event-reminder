package ru.bakanov.eventreminder.scheduler;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.bakanov.eventreminder.notifications.NotificationsAPI;

@ExtendWith(MockitoExtension.class)
class ReminderSchedulerJobTest {

    @Mock
    NotificationsAPI notificationsAPI;

    ReminderSchedulerJob job;

    @BeforeEach
    void setUp() {
        job = new ReminderSchedulerJob(notificationsAPI);
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
