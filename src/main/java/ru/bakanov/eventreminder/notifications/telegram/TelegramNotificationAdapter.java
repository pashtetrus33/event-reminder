package ru.bakanov.eventreminder.notifications.telegram;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import ru.bakanov.eventreminder.config.AppProperties;
import ru.bakanov.eventreminder.notifications.domain.NotificationPort;
import ru.bakanov.eventreminder.notifications.domain.models.NotificationChannel;
import ru.bakanov.eventreminder.notifications.domain.models.NotifyResult;

@Service
class TelegramNotificationAdapter implements NotificationPort {

    private static final Logger LOG = LoggerFactory.getLogger(TelegramNotificationAdapter.class);
    private static final String API_BASE = "https://api.telegram.org";

    private final RestClient restClient;
    private final AppProperties appProperties;

    TelegramNotificationAdapter(RestClient.Builder builder, AppProperties appProperties) {
        this.restClient = builder.baseUrl(API_BASE).build();
        this.appProperties = appProperties;
    }

    @Override
    public NotificationChannel channel() {
        return NotificationChannel.TELEGRAM;
    }

    @Override
    public NotifyResult send(String chatId, String subject, String htmlBody) {
        String token = appProperties.telegram().botToken();
        if (token == null || token.isBlank()) {
            return NotifyResult.failed("Telegram bot token not configured");
        }
        if (chatId == null || chatId.isBlank()) {
            return NotifyResult.failed("Telegram chat ID is empty");
        }
        try {
            restClient
                    .post()
                    .uri("/bot{token}/sendMessage", token)
                    .body(new SendMessageRequest(chatId, htmlBody, "HTML"))
                    .retrieve()
                    .toBodilessEntity();
            LOG.debug("Telegram message sent to chat {}", chatId);
            return NotifyResult.ok();
        } catch (Exception e) {
            LOG.error("Failed to send Telegram message to {}: {}", chatId, e.getMessage());
            return NotifyResult.failed(e.getMessage());
        }
    }

    record SendMessageRequest(String chat_id, String text, String parse_mode) {}
}
