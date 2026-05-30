package ru.bakanov.eventreminder.labels.rest.controllers;

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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import ru.bakanov.eventreminder.labels.LabelsAPI;
import ru.bakanov.eventreminder.labels.domain.models.LabelInfo;
import ru.bakanov.eventreminder.users.UsersAPI;

@Controller
@RequestMapping("/labels")
class LabelController {

    private final LabelsAPI labelsAPI;
    private final UsersAPI usersAPI;

    LabelController(LabelsAPI labelsAPI, UsersAPI usersAPI) {
        this.labelsAPI = labelsAPI;
        this.usersAPI = usersAPI;
    }

    @GetMapping
    String labelsPage(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        var user = usersAPI.getByUsername(userDetails.getUsername());
        model.addAttribute("labels", labelsAPI.getAllForUser(user.id()));
        return "labels/list";
    }

    @PostMapping
    String createLabel(
            @AuthenticationPrincipal UserDetails userDetails,
            @org.springframework.web.bind.annotation.RequestParam String name,
            @org.springframework.web.bind.annotation.RequestParam(defaultValue = "#6c757d") String color) {
        var user = usersAPI.getByUsername(userDetails.getUsername());
        labelsAPI.create(user.id(), name, color);
        return "redirect:/labels";
    }

    @PutMapping("/{id}")
    @ResponseBody
    ResponseEntity<LabelInfo> updateLabel(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable String id,
            @RequestBody LabelUpdateRequest req) {
        var user = usersAPI.getByUsername(userDetails.getUsername());
        return ResponseEntity.ok(labelsAPI.update(id, user.id(), req.name(), req.color()));
    }

    @DeleteMapping("/{id}")
    @ResponseBody
    ResponseEntity<Void> deleteLabel(@AuthenticationPrincipal UserDetails userDetails, @PathVariable String id) {
        var user = usersAPI.getByUsername(userDetails.getUsername());
        labelsAPI.delete(id, user.id());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/api")
    @ResponseBody
    List<LabelInfo> getLabelsApi(@AuthenticationPrincipal UserDetails userDetails) {
        var user = usersAPI.getByUsername(userDetails.getUsername());
        return labelsAPI.getAllForUser(user.id());
    }

    record LabelUpdateRequest(String name, String color) {}
}
