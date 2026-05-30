package ru.bakanov.eventreminder.users.rest.controllers;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import ru.bakanov.eventreminder.shared.exception.DomainException;
import ru.bakanov.eventreminder.users.UsersAPI;
import ru.bakanov.eventreminder.users.rest.dtos.RegisterRequest;

@Controller
class AuthController {

    private final UsersAPI usersAPI;

    AuthController(UsersAPI usersAPI) {
        this.usersAPI = usersAPI;
    }

    @GetMapping("/login")
    String loginPage() {
        return "auth/login";
    }

    @GetMapping("/register")
    String registerPage(Model model) {
        model.addAttribute("registerRequest", new RegisterRequest("", "", "", ""));
        return "auth/register";
    }

    @PostMapping("/register")
    String register(
            @ModelAttribute RegisterRequest registerRequest,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            return "auth/register";
        }
        if (!registerRequest.password().equals(registerRequest.confirmPassword())) {
            model.addAttribute("error", "Пароли не совпадают");
            return "auth/register";
        }
        try {
            usersAPI.register(registerRequest.username(), registerRequest.email(), registerRequest.password());
            redirectAttributes.addFlashAttribute("success", "Аккаунт создан! Войдите в систему.");
            return "redirect:/login";
        } catch (DomainException e) {
            model.addAttribute("error", e.getMessage());
            return "auth/register";
        }
    }
}
