package ru.bakanov.eventreminder.telegram;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.annotation.PostConstruct;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import ru.bakanov.eventreminder.config.AppProperties;

@Component
class TelegramBotPoller {

    private static final Logger LOG = LoggerFactory.getLogger(TelegramBotPoller.class);
    private static final String API_BASE = "https://api.telegram.org";

    private final AtomicLong offset = new AtomicLong(0);
    private final RestClient restClient;
    private final AppProperties appProperties;
    private final TelegramBotHandler handler;

    TelegramBotPoller(RestClient.Builder builder, AppProperties appProperties, TelegramBotHandler handler) {
        this.restClient = builder.baseUrl(API_BASE).build();
        this.appProperties = appProperties;
        this.handler = handler;
    }

    @PostConstruct
    void skipOldUpdates() {
        String token = appProperties.telegram().botToken();
        if (token == null || token.isBlank()) return;
        try {
            var response = fetch(token, 0);
            if (response != null && response.ok() && response.result() != null) {
                response.result().forEach(u -> offset.updateAndGet(c -> Math.max(c, u.updateId() + 1)));
            }
        } catch (Exception e) {
            LOG.warn("Could not initialize Telegram offset: {}", e.getMessage());
        }
    }

    @Scheduled(fixedDelay = 2000)
    void poll() {
        String token = appProperties.telegram().botToken();
        if (token == null || token.isBlank()) return;
        try {
            var response = fetch(token, offset.get());
            if (response == null || !response.ok() || response.result() == null) return;
            for (var update : response.result()) {
                offset.updateAndGet(c -> Math.max(c, update.updateId() + 1));
                if (update.message() != null && update.message().text() != null) {
                    handler.handle(update.message(), (chatId, text) -> send(token, chatId, text));
                }
            }
        } catch (Exception e) {
            LOG.error("Telegram polling error: {}", e.getMessage());
        }
    }

    private UpdatesResponse fetch(String token, long fromOffset) {
        return restClient
                .get()
                .uri("/bot{t}/getUpdates?offset={o}&timeout=0", token, fromOffset)
                .retrieve()
                .body(UpdatesResponse.class);
    }

    void send(String token, String chatId, String text) {
        try {
            restClient
                    .post()
                    .uri("/bot{t}/sendMessage", token)
                    .body(new SendMessageRequest(chatId, text, "HTML"))
                    .retrieve()
                    .toBodilessEntity();
        } catch (Exception e) {
            LOG.error("Failed to send Telegram message to {}: {}", chatId, e.getMessage());
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record UpdatesResponse(boolean ok, List<Update> result) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Update(@JsonProperty("update_id") long updateId, Message message) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Message(From from, String text) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record From(long id) {}

    record SendMessageRequest(String chat_id, String text, String parse_mode) {}
}
