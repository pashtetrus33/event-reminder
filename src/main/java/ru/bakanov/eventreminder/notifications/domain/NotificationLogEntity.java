package ru.bakanov.eventreminder.notifications.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import ru.bakanov.eventreminder.notifications.domain.models.NotificationChannel;
import ru.bakanov.eventreminder.shared.persistence.AssertUtil;
import ru.bakanov.eventreminder.shared.persistence.IdGenerator;

@Entity
@Table(name = "notification_log")
@EntityListeners(AuditingEntityListener.class)
class NotificationLogEntity {

    @Id
    private String id;

    @Column(name = "event_id", nullable = false)
    private String eventId;

    @Column(name = "rule_id", nullable = false)
    private String ruleId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private NotificationChannel channel;

    @Column(name = "scheduled_for", nullable = false)
    private Instant scheduledFor;

    @Column(name = "event_at")
    private Instant eventAt;

    @Column(name = "sent_at")
    private Instant sentAt;

    @Column(nullable = false, length = 20)
    private String status = "PENDING";

    @Column(name = "error_message", length = 500)
    private String errorMessage;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected NotificationLogEntity() {}

    NotificationLogEntity(
            String eventId, String ruleId, NotificationChannel channel, Instant scheduledFor, Instant eventAt) {
        this.id = IdGenerator.generateString();
        this.eventId = AssertUtil.requireNotBlank(eventId, "eventId is required");
        this.ruleId = AssertUtil.requireNotBlank(ruleId, "ruleId is required");
        this.channel = AssertUtil.requireNotNull(channel, "channel is required");
        this.scheduledFor = AssertUtil.requireNotNull(scheduledFor, "scheduledFor is required");
        this.eventAt = eventAt;
    }

    String getId() {
        return id;
    }

    String getEventId() {
        return eventId;
    }

    String getRuleId() {
        return ruleId;
    }

    NotificationChannel getChannel() {
        return channel;
    }

    Instant getScheduledFor() {
        return scheduledFor;
    }

    Instant getEventAt() {
        return eventAt;
    }

    String getStatus() {
        return status;
    }

    void markSent() {
        this.status = "SENT";
        this.sentAt = Instant.now();
    }

    void markFailed(String error) {
        this.status = "FAILED";
        this.errorMessage = error != null && error.length() > 500 ? error.substring(0, 500) : error;
    }
}
