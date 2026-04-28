package ru.itis.semestr_work3.controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import ru.itis.semestr_work3.entity.User;
import ru.itis.semestr_work3.service.UserService;

import java.util.Comparator;
import java.util.List;

@Controller
@RequestMapping("/admin/users")
@RequiredArgsConstructor
public class AdminUserController {

    private final UserService userService;

    @GetMapping
    public String list(Model model) {
        List<User> sorted = userService.findAll().stream()
                .filter(u -> u.getRole() != null && "USER".equals(u.getRole().getName()))
                .sorted(Comparator.comparingInt(this::statusPriority)
                        .thenComparing(User::getUsername, Comparator.nullsLast(String::compareTo)))
                .toList();

        model.addAttribute("users", sorted);
        return "admin/users";
    }

    @PostMapping("/{userId}/documents/{docType}/approve")
    public String approveDocument(@PathVariable Long userId,
                                  @PathVariable String docType,
                                  RedirectAttributes ra) {
        try {
            userService.approveDocument(userId, docType);
            ra.addFlashAttribute("adminSuccess", "Документ подтверждён");
        } catch (Exception e) {
            ra.addFlashAttribute("adminError", e.getMessage());
        }
        return "redirect:/admin/users";
    }

    @PostMapping("/{userId}/documents/{docType}/reject")
    public String rejectDocument(@PathVariable Long userId,
                                 @PathVariable String docType,
                                 RedirectAttributes ra) {
        try {
            userService.rejectDocument(userId, docType);
            ra.addFlashAttribute("adminSuccess", "Документ отклонён");
        } catch (Exception e) {
            ra.addFlashAttribute("adminError", e.getMessage());
        }
        return "redirect:/admin/users";
    }

    private int statusPriority(User user) {
        boolean hasPending = "PENDING".equals(user.getLicenseStatus())
                || "PENDING".equals(user.getPassportStatus());
        return hasPending ? 0 : 1;
    }
}