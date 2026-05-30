package ru.bakanov.eventreminder.events;

import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Service;
import ru.bakanov.eventreminder.config.ReminderMetrics;
import ru.bakanov.eventreminder.events.domain.EventService;
import ru.bakanov.eventreminder.events.domain.models.EventInfo;
import ru.bakanov.eventreminder.events.domain.models.RecurrenceType;

@Service
public class EventsAPI {

    private final EventService eventService;
    private final ReminderMetrics metrics;

    public EventsAPI(EventService eventService, ReminderMetrics metrics) {
        this.eventService = eventService;
        this.metrics = metrics;
    }

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
        var event = eventService.create(
                userId,
                title,
                description,
                eventAt,
                recurring,
                recurrenceType,
                recurrenceInterval,
                recurrenceEndAt,
                labelIds,
                timezone);
        metrics.incrementEventsCreated();
        return event;
    }

    public List<EventInfo> getUpcoming(String userId) {
        return eventService.getUpcoming(userId);
    }

    public List<EventInfo> getAll(String userId) {
        return eventService.getAll(userId);
    }

    public EventInfo getById(String id, String userId) {
        return eventService.getById(id, userId);
    }

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
        return eventService.update(
                id,
                userId,
                title,
                description,
                eventAt,
                recurring,
                recurrenceType,
                recurrenceInterval,
                recurrenceEndAt,
                labelIds,
                timezone);
    }

    public void delete(String id, String userId) {
        eventService.delete(id, userId);
    }

    public void advanceOccurrence(String id, Instant nextOccurrenceAt) {
        eventService.advanceOccurrence(id, nextOccurrenceAt);
    }

    public EventInfo getByIdInternal(String id) {
        return eventService.getByIdInternal(id);
    }

    public long countAll() {
        return eventService.countAll();
    }
}
