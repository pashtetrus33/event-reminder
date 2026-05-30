package ru.bakanov.eventreminder.users.domain.models;

public record UserInfo(
        String id,
        String username,
        String email,
        String telegramChatId,
        boolean emailNotificationsEnabled,
        boolean telegramNotificationsEnabled,
        int startOfDayHour,
        String timezone) {}
