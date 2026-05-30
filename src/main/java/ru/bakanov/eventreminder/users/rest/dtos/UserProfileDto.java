package ru.bakanov.eventreminder.users.rest.dtos;

public record UserProfileDto(
        String telegramChatId,
        boolean emailNotificationsEnabled,
        boolean telegramNotificationsEnabled,
        String currentPassword,
        String newPassword,
        String confirmNewPassword) {}
