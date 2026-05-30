package ru.bakanov.eventreminder.labels;

import java.util.List;
import org.springframework.stereotype.Service;
import ru.bakanov.eventreminder.labels.domain.LabelService;
import ru.bakanov.eventreminder.labels.domain.models.LabelInfo;

@Service
public class LabelsAPI {

    private final LabelService labelService;

    public LabelsAPI(LabelService labelService) {
        this.labelService = labelService;
    }

    public LabelInfo create(String userId, String name, String color) {
        return labelService.create(userId, name, color);
    }

    public List<LabelInfo> getAllForUser(String userId) {
        return labelService.getAllForUser(userId);
    }

    public LabelInfo update(String id, String userId, String name, String color) {
        return labelService.update(id, userId, name, color);
    }

    public void delete(String id, String userId) {
        labelService.delete(id, userId);
    }
}
