package ru.bakanov.eventreminder.labels.domain;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

interface LabelRepository extends JpaRepository<LabelEntity, String> {

    List<LabelEntity> findAllByUserIdOrderByName(String userId);

    Optional<LabelEntity> findByIdAndUserId(String id, String userId);

    void deleteByIdAndUserId(String id, String userId);
}
