package ru.bakanov.eventreminder.web.controllers;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import ru.bakanov.eventreminder.events.EventsAPI;
import ru.bakanov.eventreminder.labels.LabelsAPI;
import ru.bakanov.eventreminder.users.UsersAPI;

@Controller
@RequestMapping("/")
class DashboardController {

    private final EventsAPI eventsAPI;
    private final LabelsAPI labelsAPI;
    private final UsersAPI usersAPI;

    DashboardController(EventsAPI eventsAPI, LabelsAPI labelsAPI, UsersAPI usersAPI) {
        this.eventsAPI = eventsAPI;
        this.labelsAPI = labelsAPI;
        this.usersAPI = usersAPI;
    }

    @GetMapping
    String dashboard(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        var user = usersAPI.getByUsername(userDetails.getUsername());
        model.addAttribute("upcoming", eventsAPI.getUpcoming(user.id()));
        model.addAttribute("labels", labelsAPI.getAllForUser(user.id()));
        model.addAttribute("user", user);
        return "dashboard/index";
    }
}
