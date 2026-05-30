package ru.bakanov.eventreminder.notifications.domain;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

interface NotificationRuleRepository extends JpaRepository<NotificationRuleEntity, String> {

    List<NotificationRuleEntity> findAllByEventId(String eventId);

    void deleteAllByEventId(String eventId);
}
