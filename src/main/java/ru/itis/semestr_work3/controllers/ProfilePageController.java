package ru.itis.semestr_work3.controllers;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import ru.itis.semestr_work3.service.BookingService;
import ru.itis.semestr_work3.service.FavoriteService;
import ru.itis.semestr_work3.service.ReviewService;
import ru.itis.semestr_work3.service.UserService;

import java.time.LocalDate;
import java.util.List;

@Controller
@RequiredArgsConstructor
public class ProfilePageController {

    private final FavoriteService favoriteService;
    private final BookingService bookingService;
    private final ReviewService reviewService;
    private final UserService userService;

    @GetMapping("/favorites")
    public String favorites(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        if (userDetails != null) {
            userService.findByUsername(userDetails.getUsername()).ifPresent(user ->
                    model.addAttribute("favorites", favoriteService.findByUser(user.getId())));
        } else {
            model.addAttribute("favorites", List.of());
        }
        return "favorites";
    }

    @GetMapping("/profile")
    public String profile(@AuthenticationPrincipal UserDetails userDetails,
                          @RequestParam(required = false) String section,
                          Model model) {
        if (userDetails == null) return "redirect:/login";
        userService.findByUsername(userDetails.getUsername()).ifPresent(user -> {
            model.addAttribute("user", user);
            model.addAttribute("bookingsCount", bookingService.findByUser(user.getId()).size());
            model.addAttribute("reviewsCount", reviewService.findByUser(user.getId()).size());
        });
        model.addAttribute("section", section != null ? section : "info");
        return "profile";
    }

    @PostMapping("/profile")
    public String profileSave(@AuthenticationPrincipal UserDetails userDetails,
                              @RequestParam(required = false) String firstName,
                              @RequestParam(required = false) String lastName,
                              @RequestParam(required = false) String patronymic,
                              @RequestParam(required = false)
                              @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate birthDate,
                              @RequestParam(required = false) String city,
                              @RequestParam(required = false) String country,
                              @RequestParam(required = false) String phoneNumber,
                              @RequestParam(required = false) MultipartFile avatar,
                              RedirectAttributes ra) {
        if (userDetails == null) return "redirect:/login";
        try {
            userService.findByUsername(userDetails.getUsername()).ifPresent(user ->
                    userService.updateFullProfile(user.getId(),
                            firstName, lastName, patronymic,
                            birthDate, city, country,
                            phoneNumber, avatar));
            ra.addAttribute("saved", "true");
        } catch (IllegalArgumentException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        ra.addAttribute("section", "info");
        return "redirect:/profile";
    }

    @PostMapping("/profile/document")
    public String uploadDocument(@AuthenticationPrincipal UserDetails userDetails,
                                 @RequestParam String docType,
                                 @RequestParam MultipartFile file,
                                 RedirectAttributes ra) {
        if (userDetails == null) return "redirect:/login";
        try {
            userService.findByUsername(userDetails.getUsername()).ifPresent(user ->
                    userService.uploadDocument(user.getId(), docType, file));
            ra.addFlashAttribute("docSuccess", "Документ загружен и отправлен на проверку");
        } catch (IllegalArgumentException e) {
            ra.addFlashAttribute("docError", e.getMessage());
        }
        ra.addAttribute("section", "docs");
        return "redirect:/profile";
    }

    @PostMapping("/profile/delete")
    public String deleteAccount(@AuthenticationPrincipal UserDetails userDetails,
                                HttpServletRequest request) {
        if (userDetails == null) return "redirect:/login";
        userService.findByUsername(userDetails.getUsername())
                .ifPresent(user -> userService.deleteById(user.getId()));
        try {
            request.logout();
        } catch (Exception ignored) {
        }
        return "redirect:/login?deleted=true";
    }
}
