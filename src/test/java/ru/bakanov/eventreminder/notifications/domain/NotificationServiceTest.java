package ru.bakanov.eventreminder.notifications.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.bakanov.eventreminder.config.AppProperties;
import ru.bakanov.eventreminder.notifications.domain.models.NotificationChannel;
import ru.bakanov.eventreminder.notifications.domain.models.NotificationType;
import ru.bakanov.eventreminder.notifications.domain.models.NotifyResult;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    NotificationRuleRepository ruleRepository;

    @Mock
    NotificationLogRepository logRepository;

    @Mock
    NotificationPort emailPort;

    // Use real instance — AppProperties is a record (final) and cannot be mocked
    AppProperties appProperties = new AppProperties(
            new AppProperties.Telegram(""),
            new AppProperties.Mail("from@test.com"),
            new AppProperties.Scheduler("0 * * * * *", 9));

    NotificationService service;

    @BeforeEach
    void setUp() {
        when(emailPort.channel()).thenReturn(NotificationChannel.EMAIL);
        service = new NotificationService(ruleRepository, logRepository, List.of(emailPort), appProperties);
    }

    @Test
    void computeScheduledFor_before_subtractsMinutes() {
        var rule = new NotificationRuleEntity("ev1", NotificationChannel.EMAIL, NotificationType.BEFORE, 60, null);
        Instant eventAt = Instant.now().plusSeconds(7200);

        Instant scheduled = service.computeScheduledFor(rule, eventAt, 9);

        assertThat(scheduled).isCloseTo(eventAt.minus(60, ChronoUnit.MINUTES), within(1, ChronoUnit.SECONDS));
    }

    @Test
    void computeScheduledFor_startOfDay_setsTo9AM() {
        var rule =
                new NotificationRuleEntity("ev1", NotificationChannel.EMAIL, NotificationType.START_OF_DAY, null, null);
        Instant eventAt = Instant.parse("2026-06-15T14:00:00Z");

        Instant scheduled = service.computeScheduledFor(rule, eventAt, 9);

        assertThat(scheduled).isEqualTo(Instant.parse("2026-06-15T09:00:00Z"));
    }

    @Test
    void dispatch_noRecipient_marksFailed() {
        var log = new NotificationLogEntity("ev1", "rule1", NotificationChannel.EMAIL, Instant.now(), null);
        when(logRepository.findById("log1")).thenReturn(Optional.of(log));

        boolean sent =
                service.dispatch("log1", null, "Test event", null, Instant.now().plusSeconds(60), null);

        assertThat(sent).isFalse();
        assertThat(log.getStatus()).isEqualTo("FAILED");
        verify(emailPort, never()).send(any(), any(), any());
    }

    @Test
    void dispatch_withRecipient_callsAdapter() {
        var log = new NotificationLogEntity("ev1", "rule1", NotificationChannel.EMAIL, Instant.now(), null);
        when(logRepository.findById("log1")).thenReturn(Optional.of(log));
        when(emailPort.send(any(), any(), any())).thenReturn(NotifyResult.ok());

        boolean sent = service.dispatch(
                "log1", "user@example.com", "Test event", null, Instant.now().plusSeconds(60), null);

        assertThat(sent).isTrue();
        assertThat(log.getStatus()).isEqualTo("SENT");
        verify(emailPort).send(eq("user@example.com"), any(), any());
    }

    @Test
    void findPendingDue_returnsFromRepository() {
        var log = new NotificationLogEntity("ev1", "rule1", NotificationChannel.EMAIL, Instant.now(), null);
        when(logRepository.findPendingDue(any(), any())).thenReturn(List.of(log));

        var result = service.findPendingDue();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).eventId()).isEqualTo("ev1");
    }
}
