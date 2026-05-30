package ru.bakanov.eventreminder.users.domain;

import java.util.List;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository repository;

    UserDetailsServiceImpl(UserRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return repository
                .findByUsername(username)
                .map(u -> new User(
                        u.getUsername(), u.getPasswordHash(), List.of(new SimpleGrantedAuthority("ROLE_USER"))))
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));
    }
}
