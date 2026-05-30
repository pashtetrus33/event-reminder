package ru.bakanov.eventreminder.events.domain.models;

import java.time.Instant;
import java.util.List;

public record EventInfo(
        String id,
        String userId,
        String title,
        String description,
        Instant eventAt,
        boolean recurring,
        RecurrenceType recurrenceType,
        int recurrenceInterval,
        Instant recurrenceEndAt,
        Instant nextOccurrenceAt,
        List<String> labelIds,
        String timezone) {

    /** For datetime-local input: yyyy-MM-ddTHH:mm */
    public String eventAtInput() {
        return eventAt != null ? eventAt.toString().substring(0, 16) : "";
    }

    /** For datetime-local input: yyyy-MM-ddTHH:mm */
    public String recurrenceEndAtInput() {
        return recurrenceEndAt != null ? recurrenceEndAt.toString().substring(0, 16) : "";
    }

    /** For display: "29 May 2026, 09:00 UTC" */
    public String nextOccurrenceDisplay() {
        if (nextOccurrenceAt == null) return "";
        String s = nextOccurrenceAt.toString(); // 2026-05-29T09:00:00Z
        return s.substring(8, 10) + "." + s.substring(5, 7) + "." + s.substring(0, 4) + " " + s.substring(11, 16)
                + " UTC";
    }
}
