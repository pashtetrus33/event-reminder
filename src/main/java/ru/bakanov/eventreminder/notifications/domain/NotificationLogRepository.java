package ru.bakanov.eventreminder.notifications.domain;

import java.time.Instant;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface NotificationLogRepository extends JpaRepository<NotificationLogEntity, String> {

    @Query(
            "SELECT n FROM NotificationLogEntity n WHERE n.status = 'PENDING' AND n.scheduledFor <= :now AND n.scheduledFor >= :lookback")
    List<NotificationLogEntity> findPendingDue(@Param("now") Instant now, @Param("lookback") Instant lookback);

    long countByStatus(String status);
}
