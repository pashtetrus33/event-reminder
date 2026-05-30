package ru.bakanov.eventreminder.users.domain;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

interface UserRepository extends JpaRepository<UserEntity, String> {

    Optional<UserEntity> findByUsername(String username);

    Optional<UserEntity> findByTelegramChatId(String telegramChatId);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);
}
