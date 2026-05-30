package ru.bakanov.eventreminder.events.domain;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface EventRepository extends JpaRepository<EventEntity, String> {

    @Query("SELECT e FROM EventEntity e WHERE e.userId = :userId ORDER BY e.nextOccurrenceAt ASC")
    List<EventEntity> findAllByUserId(@Param("userId") String userId);

    @Query(
            "SELECT e FROM EventEntity e WHERE e.userId = :userId AND e.nextOccurrenceAt BETWEEN :from AND :to ORDER BY e.nextOccurrenceAt ASC")
    List<EventEntity> findUpcomingByUserId(
            @Param("userId") String userId, @Param("from") Instant from, @Param("to") Instant to);

    Optional<EventEntity> findByIdAndUserId(String id, String userId);

    long countByUserId(String userId);
}
