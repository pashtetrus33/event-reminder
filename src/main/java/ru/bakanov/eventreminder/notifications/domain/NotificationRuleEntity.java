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
import ru.bakanov.eventreminder.notifications.domain.models.NotificationType;
import ru.bakanov.eventreminder.shared.persistence.AssertUtil;
import ru.bakanov.eventreminder.shared.persistence.IdGenerator;

@Entity
@Table(name = "notification_rules")
@EntityListeners(AuditingEntityListener.class)
class NotificationRuleEntity {

    @Id
    private String id;

    @Column(name = "event_id", nullable = false)
    private String eventId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private NotificationChannel channel;

    @Enumerated(EnumType.STRING)
    @Column(name = "notify_type", nullable = false, length = 20)
    private NotificationType notifyType;

    @Column(name = "notify_before_minutes")
    private Integer notifyBeforeMinutes;

    @Column(name = "repeat_interval_minutes")
    private Integer repeatIntervalMinutes;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected NotificationRuleEntity() {}

    NotificationRuleEntity(
            String eventId,
            NotificationChannel channel,
            NotificationType notifyType,
            Integer notifyBeforeMinutes,
            Integer repeatIntervalMinutes) {
        this.id = IdGenerator.generateString();
        this.eventId = AssertUtil.requireNotBlank(eventId, "eventId is required");
        this.channel = AssertUtil.requireNotNull(channel, "channel is required");
        this.notifyType = AssertUtil.requireNotNull(notifyType, "notifyType is required");
        this.notifyBeforeMinutes = notifyBeforeMinutes;
        this.repeatIntervalMinutes = repeatIntervalMinutes;
    }

    String getId() {
        return id;
    }

    String getEventId() {
        return eventId;
    }

    NotificationChannel getChannel() {
        return channel;
    }

    NotificationType getNotifyType() {
        return notifyType;
    }

    Integer getNotifyBeforeMinutes() {
        return notifyBeforeMinutes;
    }

    Integer getRepeatIntervalMinutes() {
        return repeatIntervalMinutes;
    }

    boolean isActive() {
        return active;
    }
}
