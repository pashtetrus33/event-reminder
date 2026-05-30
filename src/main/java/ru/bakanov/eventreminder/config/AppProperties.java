package ru.bakanov.eventreminder.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public record AppProperties(Telegram telegram, Mail mail, Scheduler scheduler) {

    public record Telegram(String botToken) {}

    public record Mail(String from) {}

    public record Scheduler(String cron, int startOfDayHour) {}
}
