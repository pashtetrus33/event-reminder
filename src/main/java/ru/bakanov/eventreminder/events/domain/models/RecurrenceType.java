package ru.bakanov.eventreminder.events.domain.models;

public enum RecurrenceType {
    NONE,
    MINUTELY,
    HOURLY,
    DAILY,
    WEEKLY,
    MONTHLY,
    YEARLY;

    public String displayName() {
        return switch (this) {
            case NONE -> "Нет";
            case MINUTELY -> "Каждые N минут";
            case HOURLY -> "Каждые N часов";
            case DAILY -> "Ежедневно";
            case WEEKLY -> "Еженедельно";
            case MONTHLY -> "Ежемесячно";
            case YEARLY -> "Ежегодно";
        };
    }
}
