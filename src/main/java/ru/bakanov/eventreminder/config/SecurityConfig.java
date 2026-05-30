package ru.bakanov.eventreminder.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) {
        try {
            return http.authorizeHttpRequests(auth -> auth.requestMatchers(
                                    "/login",
                                    "/register",
                                    "/css/**",
                                    "/js/**",
                                    "/actuator/health",
                                    "/actuator/prometheus")
                            .permitAll()
                            .anyRequest()
                            .authenticated())
                    .formLogin(form -> form.loginPage("/login")
                            .defaultSuccessUrl("/", true)
                            .failureUrl("/login?error")
                            .permitAll())
                    .logout(logout -> logout.logoutSuccessUrl("/login?logout").permitAll())
                    .build();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to configure security filter chain", e);
        }
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
