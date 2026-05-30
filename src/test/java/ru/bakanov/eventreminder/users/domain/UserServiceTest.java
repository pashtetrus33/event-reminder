package ru.bakanov.eventreminder.users.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import ru.bakanov.eventreminder.shared.exception.DomainException;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    UserRepository repository;

    @Mock
    PasswordEncoder passwordEncoder;

    @InjectMocks
    UserService service;

    @Test
    void register_success() {
        when(repository.existsByUsername("alice")).thenReturn(false);
        when(repository.existsByEmail("alice@example.com")).thenReturn(false);
        when(passwordEncoder.encode("secret")).thenReturn("hash");
        var saved = new UserEntity("alice", "alice@example.com", "hash");
        when(repository.save(any())).thenReturn(saved);

        var result = service.register("alice", "alice@example.com", "secret");

        assertThat(result.username()).isEqualTo("alice");
        assertThat(result.email()).isEqualTo("alice@example.com");
    }

    @Test
    void register_duplicateUsername_throws() {
        when(repository.existsByUsername("alice")).thenReturn(true);

        assertThatThrownBy(() -> service.register("alice", "alice@example.com", "secret"))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("Username already taken");
    }

    @Test
    void register_duplicateEmail_throws() {
        when(repository.existsByUsername("alice")).thenReturn(false);
        when(repository.existsByEmail("alice@example.com")).thenReturn(true);

        assertThatThrownBy(() -> service.register("alice", "alice@example.com", "secret"))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("Email already registered");
    }

    @Test
    void changePassword_wrongCurrent_throws() {
        var entity = new UserEntity("alice", "alice@example.com", "hash");
        when(repository.findById(any())).thenReturn(Optional.of(entity));
        when(passwordEncoder.matches("wrong", "hash")).thenReturn(false);

        assertThatThrownBy(() -> service.changePassword("id", "wrong", "newpass"))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("Current password is incorrect");
    }
}
