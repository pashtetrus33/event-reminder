package ru.bakanov.eventreminder.web.controllers;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import ru.bakanov.eventreminder.events.EventsAPI;
import ru.bakanov.eventreminder.events.domain.models.RecurrenceType;
import ru.bakanov.eventreminder.labels.LabelsAPI;
import ru.bakanov.eventreminder.notifications.NotificationsAPI;
import ru.bakanov.eventreminder.notifications.domain.models.NotificationChannel;
import ru.bakanov.eventreminder.notifications.domain.models.NotificationType;
import ru.bakanov.eventreminder.users.UsersAPI;

@Controller
@RequestMapping("/events")
class EventController {

    private static final DateTimeFormatter DT_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm").withZone(ZoneOffset.UTC);

    private final EventsAPI eventsAPI;
    private final LabelsAPI labelsAPI;
    private final UsersAPI usersAPI;
    private final NotificationsAPI notificationsAPI;

    EventController(EventsAPI eventsAPI, LabelsAPI labelsAPI, UsersAPI usersAPI, NotificationsAPI notificationsAPI) {
        this.eventsAPI = eventsAPI;
        this.labelsAPI = labelsAPI;
        this.usersAPI = usersAPI;
        this.notificationsAPI = notificationsAPI;
    }

    @GetMapping
    String listEvents(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        var user = usersAPI.getByUsername(userDetails.getUsername());
        model.addAttribute("events", eventsAPI.getAll(user.id()));
        model.addAttribute("labels", labelsAPI.getAllForUser(user.id()));
        return "events/list";
    }

    @GetMapping("/new")
    String newEventForm(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        var user = usersAPI.getByUsername(userDetails.getUsername());
        model.addAttribute("labels", labelsAPI.getAllForUser(user.id()));
        model.addAttribute("recurrenceTypes", RecurrenceType.values());
        model.addAttribute("editing", false);
        return "events/form";
    }

    @PostMapping("/new")
    String createEvent(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam String title,
            @RequestParam(required = false) String description,
            @RequestParam String eventAt,
            @RequestParam(defaultValue = "false") boolean recurring,
            @RequestParam(defaultValue = "NONE") String recurrenceType,
            @RequestParam(defaultValue = "1") int recurrenceInterval,
            @RequestParam(required = false) String recurrenceEndAt,
            @RequestParam(required = false) List<String> labelIds,
            @RequestParam(required = false) String timezone,
            @RequestParam(required = false) List<String> ruleChannels,
            @RequestParam(required = false) List<String> ruleNotifyTypes,
            @RequestParam(required = false) List<Integer> ruleMinutes,
            @RequestParam(required = false) List<Integer> ruleRepeatIntervals,
            RedirectAttributes redirectAttributes,
            Model model) {
        var user = usersAPI.getByUsername(userDetails.getUsername());
        try {
            Instant eventInstant = parseDateTime(eventAt);
            Instant endInstant =
                    recurrenceEndAt != null && !recurrenceEndAt.isBlank() ? parseDateTime(recurrenceEndAt) : null;
            RecurrenceType rt = parseRecurrenceType(recurrenceType);

            var event = eventsAPI.create(
                    user.id(),
                    title,
                    description,
                    eventInstant,
                    recurring,
                    rt,
                    recurrenceInterval,
                    endInstant,
                    labelIds,
                    timezone);

            createRulesFromForm(
                    event.id(),
                    ruleChannels,
                    ruleNotifyTypes,
                    ruleMinutes,
                    ruleRepeatIntervals,
                    eventInstant,
                    true,
                    user.startOfDayHour());
            redirectAttributes.addFlashAttribute("success", "Событие создано");
            return "redirect:/";
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("labels", labelsAPI.getAllForUser(user.id()));
            model.addAttribute("recurrenceTypes", RecurrenceType.values());
            model.addAttribute("editing", false);
            return "events/form";
        }
    }

    @GetMapping("/{id}/edit")
    String editEventForm(@AuthenticationPrincipal UserDetails userDetails, @PathVariable String id, Model model) {
        var user = usersAPI.getByUsername(userDetails.getUsername());
        var event = eventsAPI.getById(id, user.id());
        var rules = notificationsAPI.getRulesForEvent(id);
        model.addAttribute("event", event);
        model.addAttribute("rules", rules);
        model.addAttribute("labels", labelsAPI.getAllForUser(user.id()));
        model.addAttribute("recurrenceTypes", RecurrenceType.values());
        model.addAttribute("editing", true);
        return "events/form";
    }

    @PostMapping("/{id}/edit")
    String updateEvent(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable String id,
            @RequestParam String title,
            @RequestParam(required = false) String description,
            @RequestParam String eventAt,
            @RequestParam(defaultValue = "false") boolean recurring,
            @RequestParam(defaultValue = "NONE") String recurrenceType,
            @RequestParam(defaultValue = "1") int recurrenceInterval,
            @RequestParam(required = false) String recurrenceEndAt,
            @RequestParam(required = false) List<String> labelIds,
            @RequestParam(required = false) String timezone,
            @RequestParam(required = false) List<String> ruleChannels,
            @RequestParam(required = false) List<String> ruleNotifyTypes,
            @RequestParam(required = false) List<Integer> ruleMinutes,
            @RequestParam(required = false) List<Integer> ruleRepeatIntervals,
            RedirectAttributes redirectAttributes,
            Model model) {
        var user = usersAPI.getByUsername(userDetails.getUsername());
        try {
            Instant eventInstant = parseDateTime(eventAt);
            Instant endInstant =
                    recurrenceEndAt != null && !recurrenceEndAt.isBlank() ? parseDateTime(recurrenceEndAt) : null;
            RecurrenceType rt = parseRecurrenceType(recurrenceType);

            eventsAPI.update(
                    id,
                    user.id(),
                    title,
                    description,
                    eventInstant,
                    recurring,
                    rt,
                    recurrenceInterval,
                    endInstant,
                    labelIds,
                    timezone);

            notificationsAPI.deleteRulesForEvent(id);
            createRulesFromForm(
                    id,
                    ruleChannels,
                    ruleNotifyTypes,
                    ruleMinutes,
                    ruleRepeatIntervals,
                    eventInstant,
                    false,
                    user.startOfDayHour());
            redirectAttributes.addFlashAttribute("success", "Событие обновлено");
            return "redirect:/events";
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("event", eventsAPI.getById(id, user.id()));
            model.addAttribute("labels", labelsAPI.getAllForUser(user.id()));
            model.addAttribute("recurrenceTypes", RecurrenceType.values());
            model.addAttribute("editing", true);
            return "events/form";
        }
    }

    @DeleteMapping("/{id}")
    @ResponseBody
    ResponseEntity<Void> deleteEvent(@AuthenticationPrincipal UserDetails userDetails, @PathVariable String id) {
        var user = usersAPI.getByUsername(userDetails.getUsername());
        eventsAPI.delete(id, user.id());
        return ResponseEntity.noContent().build();
    }

    private void createRulesFromForm(
            String eventId,
            List<String> ruleChannels,
            List<String> ruleNotifyTypes,
            List<Integer> ruleMinutes,
            List<Integer> ruleRepeatIntervals,
            Instant eventInstant,
            boolean addDefaults,
            int startOfDayHour) {
        if (ruleChannels == null || ruleChannels.isEmpty()) {
            if (addDefaults) {
                notificationsAPI.createRule(
                        eventId,
                        NotificationChannel.EMAIL,
                        NotificationType.BEFORE,
                        0,
                        null,
                        eventInstant,
                        startOfDayHour);
                notificationsAPI.createRule(
                        eventId,
                        NotificationChannel.TELEGRAM,
                        NotificationType.BEFORE,
                        0,
                        null,
                        eventInstant,
                        startOfDayHour);
            }
            return;
        }
        for (int i = 0; i < ruleChannels.size(); i++) {
            String channelVal = ruleChannels.get(i);
            NotificationType nt = NotificationType.valueOf(
                    ruleNotifyTypes != null && i < ruleNotifyTypes.size() ? ruleNotifyTypes.get(i) : "BEFORE");
            Integer mins = (ruleMinutes != null && i < ruleMinutes.size()) ? ruleMinutes.get(i) : null;
            Integer repeat = (ruleRepeatIntervals != null
                            && i < ruleRepeatIntervals.size()
                            && ruleRepeatIntervals.get(i) != null
                            && ruleRepeatIntervals.get(i) > 0)
                    ? ruleRepeatIntervals.get(i)
                    : null;
            if ("BOTH".equals(channelVal)) {
                notificationsAPI.createRule(
                        eventId, NotificationChannel.EMAIL, nt, mins, repeat, eventInstant, startOfDayHour);
                notificationsAPI.createRule(
                        eventId, NotificationChannel.TELEGRAM, nt, mins, repeat, eventInstant, startOfDayHour);
            } else {
                notificationsAPI.createRule(
                        eventId,
                        NotificationChannel.valueOf(channelVal),
                        nt,
                        mins,
                        repeat,
                        eventInstant,
                        startOfDayHour);
            }
        }
    }

    private Instant parseDateTime(String value) {
        try {
            return DT_FORMATTER.parse(value, Instant::from);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Invalid date/time format: " + value);
        }
    }

    private RecurrenceType parseRecurrenceType(String value) {
        try {
            return RecurrenceType.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            return RecurrenceType.NONE;
        }
    }
}
