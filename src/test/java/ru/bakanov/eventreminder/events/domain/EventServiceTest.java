package ru.bakanov.eventreminder.events.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.bakanov.eventreminder.events.domain.models.RecurrenceType;
import ru.bakanov.eventreminder.shared.exception.ResourceNotFoundException;

@ExtendWith(MockitoExtension.class)
class EventServiceTest {

    @Mock
    EventRepository repository;

    @InjectMocks
    EventService service;

    @Test
    void create_returnsEventInfo() {
        Instant now = Instant.now().plusSeconds(3600);
        var entity = new EventEntity("user1", "Meeting", "Weekly sync", now);
        when(repository.save(any())).thenReturn(entity);

        var result = service.create(
                "user1", "Meeting", "Weekly sync", now, false, RecurrenceType.NONE, 1, null, List.of(), null);

        assertThat(result.title()).isEqualTo("Meeting");
        assertThat(result.userId()).isEqualTo("user1");
        assertThat(result.recurring()).isFalse();
    }

    @Test
    void delete_notFound_throws() {
        when(repository.findByIdAndUserId("x", "u")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.delete("x", "u")).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void delete_found_callsRepositoryDelete() {
        var entity = new EventEntity("u", "title", null, Instant.now().plusSeconds(3600));
        when(repository.findByIdAndUserId(any(), any())).thenReturn(Optional.of(entity));

        service.delete("id", "u");

        verify(repository).delete(entity);
    }

    @Test
    void getUpcoming_returnsOnlyFutureEvents() {
        Instant futureDate = Instant.now().plusSeconds(3600);
        var entity = new EventEntity("u", "Future event", null, futureDate);
        when(repository.findUpcomingByUserId(any(), any(), any())).thenReturn(List.of(entity));

        var result = service.getUpcoming("u");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).title()).isEqualTo("Future event");
    }
}
