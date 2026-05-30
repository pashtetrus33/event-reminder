package ru.bakanov.eventreminder.events.domain;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.bakanov.eventreminder.events.domain.models.EventInfo;
import ru.bakanov.eventreminder.events.domain.models.RecurrenceType;
import ru.bakanov.eventreminder.shared.exception.ResourceNotFoundException;

@Service
public class EventService {

    private final EventRepository repository;

    EventService(EventRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public EventInfo create(
            String userId,
            String title,
            String description,
            Instant eventAt,
            boolean recurring,
            RecurrenceType recurrenceType,
            int recurrenceInterval,
            Instant recurrenceEndAt,
            List<String> labelIds,
            String timezone) {
        var entity = new EventEntity(userId, title, description, eventAt);
        entity.update(
                title,
                description,
                eventAt,
                recurring,
                recurrenceType,
                recurrenceInterval,
                recurrenceEndAt,
                labelIds,
                timezone);
        return toInfo(repository.save(entity));
    }

    @Transactional(readOnly = true)
    public List<EventInfo> getUpcoming(String userId) {
        Instant now = Instant.now();
        Instant in7Days = now.plus(7, ChronoUnit.DAYS);
        return repository.findUpcomingByUserId(userId, now, in7Days).stream()
                .map(this::toInfo)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<EventInfo> getAll(String userId) {
        return repository.findAllByUserId(userId).stream().map(this::toInfo).toList();
    }

    @Transactional(readOnly = true)
    public EventInfo getById(String id, String userId) {
        return repository
                .findByIdAndUserId(id, userId)
                .map(this::toInfo)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found: " + id));
    }

    @Transactional
    public EventInfo update(
            String id,
            String userId,
            String title,
            String description,
            Instant eventAt,
            boolean recurring,
            RecurrenceType recurrenceType,
            int recurrenceInterval,
            Instant recurrenceEndAt,
            List<String> labelIds,
            String timezone) {
        var entity = findEntity(id, userId);
        entity.update(
                title,
                description,
                eventAt,
                recurring,
                recurrenceType,
                recurrenceInterval,
                recurrenceEndAt,
                labelIds,
                timezone);
        return toInfo(repository.save(entity));
    }

    @Transactional
    public void delete(String id, String userId) {
        var entity = findEntity(id, userId);
        repository.delete(entity);
    }

    @Transactional
    public void advanceOccurrence(String id, Instant nextOccurrenceAt) {
        var entity = repository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Event not found: " + id));
        entity.advanceNextOccurrence(nextOccurrenceAt);
        repository.save(entity);
    }

    @Transactional(readOnly = true)
    public EventInfo getByIdInternal(String id) {
        return repository
                .findById(id)
                .map(this::toInfo)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found: " + id));
    }

    @Transactional(readOnly = true)
    public long countAll() {
        return repository.count();
    }

    private EventEntity findEntity(String id, String userId) {
        return repository
                .findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found: " + id));
    }

    private EventInfo toInfo(EventEntity e) {
        return new EventInfo(
                e.getId(),
                e.getUserId(),
                e.getTitle(),
                e.getDescription(),
                e.getEventAt(),
                e.isRecurring(),
                e.getRecurrenceType(),
                e.getRecurrenceInterval(),
                e.getRecurrenceEndAt(),
                e.getNextOccurrenceAt(),
                e.getLabelIds(),
                e.getTimezone());
    }
}
