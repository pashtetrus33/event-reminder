package ru.bakanov.eventreminder;

import org.springframework.boot.SpringApplication;

public class TestEventReminderApplication {

    public static void main(String[] args) {
        SpringApplication.from(EventReminderApplication::main)
                .with(TestcontainersConfiguration.class)
                .run(args);
    }
}
