package ru.bakanov.eventreminder.events.domain;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import ru.bakanov.eventreminder.events.domain.models.RecurrenceType;
import ru.bakanov.eventreminder.shared.persistence.AssertUtil;
import ru.bakanov.eventreminder.shared.persistence.BaseEntity;
import ru.bakanov.eventreminder.shared.persistence.IdGenerator;

@Entity
@Table(name = "events")
class EventEntity extends BaseEntity {

    @Id
    private String id;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "event_at", nullable = false)
    private Instant eventAt;

    @Column(name = "is_recurring", nullable = false)
    private boolean recurring = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "recurrence_type", nullable = false, length = 20)
    private RecurrenceType recurrenceType = RecurrenceType.NONE;

    @Column(name = "recurrence_interval", nullable = false)
    private int recurrenceInterval = 1;

    @Column(name = "recurrence_end_at")
    private Instant recurrenceEndAt;

    @Column(name = "next_occurrence_at", nullable = false)
    private Instant nextOccurrenceAt;

    @Column(name = "timezone", length = 50)
    private String timezone;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "event_labels", joinColumns = @JoinColumn(name = "event_id"))
    @Column(name = "label_id")
    private List<String> labelIds = new ArrayList<>();

    protected EventEntity() {}

    EventEntity(String userId, String title, String description, Instant eventAt) {
        this.id = IdGenerator.generateString();
        this.userId = AssertUtil.requireNotBlank(userId, "userId is required");
        this.title = AssertUtil.requireNotBlank(title, "title is required");
        this.description = description;
        this.eventAt = AssertUtil.requireNotNull(eventAt, "eventAt is required");
        this.nextOccurrenceAt = eventAt;
    }

    String getId() {
        return id;
    }

    String getUserId() {
        return userId;
    }

    String getTitle() {
        return title;
    }

    String getDescription() {
        return description;
    }

    Instant getEventAt() {
        return eventAt;
    }

    boolean isRecurring() {
        return recurring;
    }

    RecurrenceType getRecurrenceType() {
        return recurrenceType;
    }

    int getRecurrenceInterval() {
        return recurrenceInterval;
    }

    Instant getRecurrenceEndAt() {
        return recurrenceEndAt;
    }

    Instant getNextOccurrenceAt() {
        return nextOccurrenceAt;
    }

    List<String> getLabelIds() {
        return labelIds;
    }

    String getTimezone() {
        return timezone;
    }

    void update(
            String title,
            String description,
            Instant eventAt,
            boolean recurring,
            RecurrenceType recurrenceType,
            int recurrenceInterval,
            Instant recurrenceEndAt,
            List<String> labelIds,
            String timezone) {
        this.title = AssertUtil.requireNotBlank(title, "title is required");
        this.description = description;
        this.eventAt = AssertUtil.requireNotNull(eventAt, "eventAt is required");
        this.recurring = recurring;
        this.recurrenceType = recurrenceType != null ? recurrenceType : RecurrenceType.NONE;
        this.recurrenceInterval = recurrenceInterval > 0 ? recurrenceInterval : 1;
        this.recurrenceEndAt = recurrenceEndAt;
        this.nextOccurrenceAt = eventAt;
        this.labelIds = labelIds != null ? new ArrayList<>(labelIds) : new ArrayList<>();
        if (timezone != null && !timezone.isBlank()) this.timezone = timezone;
    }

    void advanceNextOccurrence(Instant nextOccurrence) {
        this.nextOccurrenceAt = nextOccurrence;
    }
}
