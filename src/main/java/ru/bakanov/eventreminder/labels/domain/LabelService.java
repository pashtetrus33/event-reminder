package ru.bakanov.eventreminder.labels.domain;

import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.bakanov.eventreminder.labels.domain.models.LabelInfo;
import ru.bakanov.eventreminder.shared.exception.ResourceNotFoundException;

@Service
public class LabelService {

    private final LabelRepository repository;

    LabelService(LabelRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public LabelInfo create(String userId, String name, String color) {
        return toInfo(repository.save(new LabelEntity(userId, name, color)));
    }

    @Transactional(readOnly = true)
    public List<LabelInfo> getAllForUser(String userId) {
        return repository.findAllByUserIdOrderByName(userId).stream()
                .map(this::toInfo)
                .toList();
    }

    @Transactional
    public LabelInfo update(String id, String userId, String name, String color) {
        var entity = repository
                .findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Label not found: " + id));
        entity.update(name, color);
        return toInfo(repository.save(entity));
    }

    @Transactional
    public void delete(String id, String userId) {
        repository.deleteByIdAndUserId(id, userId);
    }

    private LabelInfo toInfo(LabelEntity e) {
        return new LabelInfo(e.getId(), e.getName(), e.getColor());
    }
}
