package ru.bakanov.eventreminder.telegram;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import org.springframework.stereotype.Service;
import ru.bakanov.eventreminder.events.EventsAPI;
import ru.bakanov.eventreminder.events.domain.models.RecurrenceType;
import ru.bakanov.eventreminder.notifications.NotificationsAPI;
import ru.bakanov.eventreminder.notifications.domain.models.NotificationChannel;
import ru.bakanov.eventreminder.notifications.domain.models.NotificationType;
import ru.bakanov.eventreminder.users.UsersAPI;
import ru.bakanov.eventreminder.users.domain.models.UserInfo;

@Service
class TelegramBotHandler {

    private static final DateTimeFormatter INPUT_PATTERN = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final DateTimeFormatter DISPLAY_PATTERN =
            DateTimeFormatter.ofPattern("d MMMM yyyy, HH:mm", Locale.forLanguageTag("ru"));

    private final UsersAPI usersAPI;
    private final EventsAPI eventsAPI;
    private final NotificationsAPI notificationsAPI;

    // Last shown event list per chatId — for numbered /delete
    private final Map<String, List<String>> lastList = new ConcurrentHashMap<>();

    TelegramBotHandler(UsersAPI usersAPI, EventsAPI eventsAPI, NotificationsAPI notificationsAPI) {
        this.usersAPI = usersAPI;
        this.eventsAPI = eventsAPI;
        this.notificationsAPI = notificationsAPI;
    }

    void handle(TelegramBotPoller.Message message, BiConsumer<String, String> reply) {
        String chatId = String.valueOf(message.from().id());
        String text = message.text().trim();
        String cmd = text.split("\\s+")[0].toLowerCase();

        if (cmd.equals("/start") || cmd.equals("/help")) {
            reply.accept(chatId, helpText(chatId));
            return;
        }

        var userOpt = usersAPI.findByTelegramChatId(chatId);
        if (userOpt.isEmpty()) {
            reply.accept(
                    chatId,
                    "❌ Аккаунт не привязан.\n\nУкажите ваш Chat ID в настройках профиля на сайте:\n<code>" + chatId
                            + "</code>");
            return;
        }

        UserInfo user = userOpt.get();
        switch (cmd) {
            case "/list", "/список" -> handleList(chatId, user, reply);
            case "/add", "/добавить" -> handleAdd(chatId, user, text, reply);
            case "/delete", "/удалить" -> handleDelete(chatId, user, text, reply);
            default -> reply.accept(chatId, "Неизвестная команда. /help — справка.");
        }
    }

    private void handleList(String chatId, UserInfo user, BiConsumer<String, String> reply) {
        var events = eventsAPI.getAll(user.id());
        if (events.isEmpty()) {
            reply.accept(chatId, "📭 Событий нет. Создайте первое:\n<code>/add Название 2026-06-15 10:30</code>");
            return;
        }
        ZoneId zone = userZone(user);
        var ids = new ArrayList<String>();
        var sb = new StringBuilder("📅 <b>Ваши события:</b>\n\n");
        for (int i = 0; i < events.size(); i++) {
            var e = events.get(i);
            ids.add(e.id());
            Instant at = e.nextOccurrenceAt() != null ? e.nextOccurrenceAt() : e.eventAt();
            sb.append(i + 1).append(". <b>").append(esc(e.title())).append("</b>");
            if (e.recurring()) sb.append(" 🔁");
            sb.append("\n   🕐 ")
                    .append(DISPLAY_PATTERN.withZone(zone).format(at))
                    .append("\n\n");
        }
        sb.append("Удалить: <code>/delete N</code>");
        lastList.put(chatId, ids);
        reply.accept(chatId, sb.toString());
    }

    private void handleAdd(String chatId, UserInfo user, String text, BiConsumer<String, String> reply) {
        // /add <title> YYYY-MM-DD HH:mm
        String[] parts = text.split("\\s+");
        if (parts.length < 4) {
            reply.accept(
                    chatId,
                    "❌ Формат: /add Название ГГГГ-ММ-ДД ЧЧ:мм\n\nПример:\n<code>/add Встреча 2026-06-15 10:30</code>");
            return;
        }
        String datePart = parts[parts.length - 2];
        String timePart = parts[parts.length - 1];
        String title = String.join(" ", List.of(parts).subList(1, parts.length - 2));
        if (title.isBlank()) {
            reply.accept(chatId, "❌ Укажите название события.");
            return;
        }
        ZoneId zone = userZone(user);
        Instant eventAt;
        try {
            eventAt = INPUT_PATTERN.withZone(zone).parse(datePart + " " + timePart, Instant::from);
        } catch (DateTimeParseException e) {
            reply.accept(
                    chatId,
                    "❌ Неверный формат даты. Используйте: ГГГГ-ММ-ДД ЧЧ:мм\n\nПример: <code>2026-06-15 10:30</code>");
            return;
        }
        try {
            var event = eventsAPI.create(
                    user.id(), title, null, eventAt, false, RecurrenceType.NONE, 1, null, null, zone.getId());
            notificationsAPI.createRule(
                    event.id(),
                    NotificationChannel.TELEGRAM,
                    NotificationType.BEFORE,
                    0,
                    null,
                    eventAt,
                    user.startOfDayHour());
            notificationsAPI.createRule(
                    event.id(),
                    NotificationChannel.EMAIL,
                    NotificationType.BEFORE,
                    0,
                    null,
                    eventAt,
                    user.startOfDayHour());
            reply.accept(
                    chatId,
                    "✅ Событие создано!\n\n📌 <b>" + esc(title) + "</b>\n🕐 "
                            + DISPLAY_PATTERN.withZone(zone).format(eventAt));
        } catch (Exception e) {
            reply.accept(chatId, "❌ Ошибка: " + esc(e.getMessage()));
        }
    }

    private void handleDelete(String chatId, UserInfo user, String text, BiConsumer<String, String> reply) {
        String[] parts = text.split("\\s+");
        if (parts.length < 2) {
            reply.accept(chatId, "❌ Укажите номер: /delete N\n\nСначала выполните /list.");
            return;
        }
        int num;
        try {
            num = Integer.parseInt(parts[1]);
        } catch (NumberFormatException e) {
            reply.accept(chatId, "❌ Укажите номер из списка /list.");
            return;
        }
        List<String> ids = lastList.get(chatId);
        if (ids == null || num < 1 || num > ids.size()) {
            reply.accept(chatId, "❌ Сначала выполните /list, затем укажите номер.");
            return;
        }
        String eventId = ids.get(num - 1);
        try {
            var event = eventsAPI.getById(eventId, user.id());
            eventsAPI.delete(eventId, user.id());
            lastList.put(chatId, ids.stream().filter(id -> !id.equals(eventId)).toList());
            reply.accept(chatId, "🗑 Удалено: <b>" + esc(event.title()) + "</b>");
        } catch (Exception e) {
            reply.accept(chatId, "❌ Не удалось удалить событие.");
        }
    }

    private String helpText(String chatId) {
        return """
                🤖 <b>RemindMe Bot</b>

                <b>Команды:</b>
                /list — список событий
                /add Название ГГГГ-ММ-ДД ЧЧ:мм — создать событие
                /delete N — удалить событие №N из списка

                <b>Пример:</b>
                <code>/add Встреча 2026-06-15 10:30</code>

                ⏰ Время вводится в вашем часовом поясе (определяется по событиям из браузера).

                Ваш Chat ID: <code>%s</code>
                Укажите его в настройках профиля, чтобы привязать аккаунт.""".formatted(chatId);
    }

    private ZoneId userZone(UserInfo user) {
        String tz = user.timezone();
        if (tz == null || tz.isBlank()) return ZoneOffset.UTC;
        try {
            return ZoneId.of(tz);
        } catch (Exception e) {
            return ZoneOffset.UTC;
        }
    }

    private String esc(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
