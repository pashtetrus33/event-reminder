package ru.bakanov.eventreminder.events.rest.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record CreateEventRequest(
        @NotBlank String title,
        String description,
        @NotNull String eventAt,
        boolean recurring,
        String recurrenceType,
        int recurrenceInterval,
        String recurrenceEndAt,
        List<String> labelIds,
        List<NotificationRuleRequest> rules) {

    public record NotificationRuleRequest(String channel, String notifyType, Integer notifyBeforeMinutes) {}
}
