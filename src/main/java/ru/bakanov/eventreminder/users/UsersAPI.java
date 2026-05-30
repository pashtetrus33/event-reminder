package ru.bakanov.eventreminder.users;

import java.util.Optional;
import org.springframework.stereotype.Service;
import ru.bakanov.eventreminder.users.domain.UserService;
import ru.bakanov.eventreminder.users.domain.models.UserInfo;

@Service
public class UsersAPI {

    private final UserService userService;

    public UsersAPI(UserService userService) {
        this.userService = userService;
    }

    public UserInfo register(String username, String email, String rawPassword) {
        return userService.register(username, email, rawPassword);
    }

    public UserInfo getByUsername(String username) {
        return userService.getByUsername(username);
    }

    public UserInfo getById(String id) {
        return userService.getById(id);
    }

    public Optional<UserInfo> findByTelegramChatId(String chatId) {
        return userService.findByTelegramChatId(chatId);
    }

    public UserInfo updateProfile(
            String id,
            String telegramChatId,
            boolean emailEnabled,
            boolean telegramEnabled,
            int startOfDayHour,
            String timezone) {
        return userService.updateProfile(id, telegramChatId, emailEnabled, telegramEnabled, startOfDayHour, timezone);
    }

    public void changePassword(String id, String currentRaw, String newRaw) {
        userService.changePassword(id, currentRaw, newRaw);
    }

    public long countAll() {
        return userService.countAll();
    }
}
