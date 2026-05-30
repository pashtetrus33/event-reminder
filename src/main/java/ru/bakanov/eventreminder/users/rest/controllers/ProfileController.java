package ru.bakanov.eventreminder.users.rest.controllers;

import java.util.List;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import ru.bakanov.eventreminder.shared.exception.DomainException;
import ru.bakanov.eventreminder.users.UsersAPI;

@Controller
@RequestMapping("/profile")
class ProfileController {

    private static final List<String> TIMEZONES = List.of(
            "UTC",
            "Europe/Moscow",
            "Europe/Kaliningrad",
            "Europe/Samara",
            "Asia/Yekaterinburg",
            "Asia/Omsk",
            "Asia/Krasnoyarsk",
            "Asia/Irkutsk",
            "Asia/Yakutsk",
            "Asia/Vladivostok",
            "Asia/Magadan",
            "Asia/Kamchatka",
            "Europe/London",
            "Europe/Paris",
            "Europe/Berlin",
            "Europe/Kiev",
            "Europe/Istanbul",
            "Asia/Dubai",
            "Asia/Almaty",
            "Asia/Tashkent",
            "Asia/Novosibirsk",
            "Asia/Bangkok",
            "Asia/Shanghai",
            "Asia/Tokyo",
            "America/New_York",
            "America/Chicago",
            "America/Denver",
            "America/Los_Angeles");

    private final UsersAPI usersAPI;

    ProfileController(UsersAPI usersAPI) {
        this.usersAPI = usersAPI;
    }

    @GetMapping
    String profilePage(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        var user = usersAPI.getByUsername(userDetails.getUsername());
        model.addAttribute("user", user);
        model.addAttribute("timezones", TIMEZONES);
        return "profile/settings";
    }

    @PostMapping("/update")
    String updateProfile(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(required = false) String telegramChatId,
            @RequestParam(defaultValue = "false") boolean emailNotificationsEnabled,
            @RequestParam(defaultValue = "false") boolean telegramNotificationsEnabled,
            @RequestParam(defaultValue = "9") int startOfDayHour,
            @RequestParam(defaultValue = "UTC") String timezone,
            RedirectAttributes redirectAttributes) {
        var user = usersAPI.getByUsername(userDetails.getUsername());
        usersAPI.updateProfile(
                user.id(),
                telegramChatId,
                emailNotificationsEnabled,
                telegramNotificationsEnabled,
                startOfDayHour,
                timezone);
        redirectAttributes.addFlashAttribute("success", "Профиль обновлён");
        return "redirect:/profile";
    }

    @PostMapping("/change-password")
    String changePassword(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam String currentPassword,
            @RequestParam String newPassword,
            @RequestParam String confirmNewPassword,
            RedirectAttributes redirectAttributes) {
        if (!newPassword.equals(confirmNewPassword)) {
            redirectAttributes.addFlashAttribute("error", "Новые пароли не совпадают");
            return "redirect:/profile";
        }
        try {
            var user = usersAPI.getByUsername(userDetails.getUsername());
            usersAPI.changePassword(user.id(), currentPassword, newPassword);
            redirectAttributes.addFlashAttribute("success", "Пароль изменён");
        } catch (DomainException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/profile";
    }
}
