package ru.bakanov.eventreminder.users.domain;

import java.util.Optional;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.bakanov.eventreminder.shared.exception.DomainException;
import ru.bakanov.eventreminder.shared.exception.ResourceNotFoundException;
import ru.bakanov.eventreminder.users.domain.models.UserInfo;

@Service
public class UserService {

    private final UserRepository repository;
    private final PasswordEncoder passwordEncoder;

    UserService(UserRepository repository, PasswordEncoder passwordEncoder) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public UserInfo register(String username, String email, String rawPassword) {
        if (repository.existsByUsername(username)) {
            throw new DomainException("Username already taken: " + username);
        }
        if (repository.existsByEmail(email)) {
            throw new DomainException("Email already registered: " + email);
        }
        var entity = new UserEntity(username, email, passwordEncoder.encode(rawPassword));
        return toInfo(repository.save(entity));
    }

    @Transactional(readOnly = true)
    public UserInfo getByUsername(String username) {
        return repository
                .findByUsername(username)
                .map(this::toInfo)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));
    }

    @Transactional(readOnly = true)
    public Optional<UserInfo> findByTelegramChatId(String chatId) {
        return repository.findByTelegramChatId(chatId).map(this::toInfo);
    }

    @Transactional(readOnly = true)
    public UserInfo getById(String id) {
        return repository
                .findById(id)
                .map(this::toInfo)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + id));
    }

    @Transactional
    public UserInfo updateProfile(
            String id,
            String telegramChatId,
            boolean emailEnabled,
            boolean telegramEnabled,
            int startOfDayHour,
            String timezone) {
        var entity = findEntityById(id);
        entity.updateProfile(telegramChatId, emailEnabled, telegramEnabled, startOfDayHour, timezone);
        return toInfo(repository.save(entity));
    }

    @Transactional
    public void changePassword(String id, String currentRaw, String newRaw) {
        var entity = findEntityById(id);
        if (!passwordEncoder.matches(currentRaw, entity.getPasswordHash())) {
            throw new DomainException("Current password is incorrect");
        }
        entity.changePassword(passwordEncoder.encode(newRaw));
        repository.save(entity);
    }

    @Transactional(readOnly = true)
    public long countAll() {
        return repository.count();
    }

    private UserEntity findEntityById(String id) {
        return repository.findById(id).orElseThrow(() -> new ResourceNotFoundException("User not found: " + id));
    }

    private UserInfo toInfo(UserEntity e) {
        return new UserInfo(
                e.getId(),
                e.getUsername(),
                e.getEmail(),
                e.getTelegramChatId(),
                e.isEmailNotificationsEnabled(),
                e.isTelegramNotificationsEnabled(),
                e.getStartOfDayHour(),
                e.getTimezone());
    }
}
