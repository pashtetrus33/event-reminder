package ru.bakanov.eventreminder.users.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import ru.bakanov.eventreminder.shared.persistence.AssertUtil;
import ru.bakanov.eventreminder.shared.persistence.BaseEntity;
import ru.bakanov.eventreminder.shared.persistence.IdGenerator;

@Entity
@Table(name = "users")
class UserEntity extends BaseEntity {

    @Id
    private String id;

    @Column(nullable = false, unique = true, length = 50)
    private String username;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "telegram_chat_id")
    private String telegramChatId;

    @Column(name = "email_notifications_enabled", nullable = false)
    private boolean emailNotificationsEnabled = true;

    @Column(name = "telegram_notifications_enabled", nullable = false)
    private boolean telegramNotificationsEnabled = false;

    @Column(name = "start_of_day_hour", nullable = false)
    private int startOfDayHour = 9;

    @Column(name = "timezone", nullable = false, length = 50)
    private String timezone = "UTC";

    protected UserEntity() {}

    UserEntity(String username, String email, String passwordHash) {
        this.id = IdGenerator.generateString();
        this.username = AssertUtil.requireNotBlank(username, "username is required");
        this.email = AssertUtil.requireNotBlank(email, "email is required");
        this.passwordHash = AssertUtil.requireNotBlank(passwordHash, "passwordHash is required");
    }

    String getId() {
        return id;
    }

    String getUsername() {
        return username;
    }

    String getEmail() {
        return email;
    }

    String getPasswordHash() {
        return passwordHash;
    }

    String getTelegramChatId() {
        return telegramChatId;
    }

    boolean isEmailNotificationsEnabled() {
        return emailNotificationsEnabled;
    }

    boolean isTelegramNotificationsEnabled() {
        return telegramNotificationsEnabled;
    }

    int getStartOfDayHour() {
        return startOfDayHour;
    }

    String getTimezone() {
        return timezone;
    }

    void updateProfile(
            String telegramChatId, boolean emailEnabled, boolean telegramEnabled, int startOfDayHour, String timezone) {
        this.telegramChatId = telegramChatId;
        this.emailNotificationsEnabled = emailEnabled;
        this.telegramNotificationsEnabled = telegramEnabled;
        this.startOfDayHour = Math.max(0, Math.min(23, startOfDayHour));
        this.timezone = (timezone != null && !timezone.isBlank()) ? timezone : "UTC";
    }

    void changePassword(String newPasswordHash) {
        this.passwordHash = AssertUtil.requireNotBlank(newPasswordHash, "passwordHash is required");
    }
}
