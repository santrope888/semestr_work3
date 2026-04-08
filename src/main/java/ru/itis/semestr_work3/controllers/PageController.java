package ru.itis.semestr_work3.controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import ru.itis.semestr_work3.dto.BookingFilter;
import ru.itis.semestr_work3.dto.CarFilter;
import ru.itis.semestr_work3.service.*;

import java.time.LocalDate;
import java.util.List;

@Controller
@RequiredArgsConstructor
public class PageController {

    private final CarService carService;
    private final CategoryService categoryService;
    private final InsuranceService insuranceService;
    private final BookingService bookingService;
    private final FavoriteService favoriteService;
    private final ReviewService reviewService;
    private final UserService userService;

    @GetMapping("/")
    public String index() {
        return "index";
    }

    @GetMapping("/catalog")
    public String catalog(CarFilter filter,
                          @PageableDefault(size = 12) Pageable pageable,
                          Model model) {
        var carsPage = carService.findCars(filter, pageable);
        model.addAttribute("cars", carsPage.getContent());
        model.addAttribute("carsPage", carsPage);
        model.addAttribute("filter", filter);
        model.addAttribute("categories", categoryService.findAll());
        model.addAttribute("brands", carService.findDistinctBrands());
        model.addAttribute("colors", carService.findDistinctColors());
        return "catalog";
    }

    @GetMapping("/cars/{id}")
    public String carDetail(@PathVariable Long id, Model model) {
        model.addAttribute("car", carService.findById(id)
                .orElseThrow(() -> new RuntimeException("Car not found")));
        model.addAttribute("reviews", reviewService.findByCar(id));
        return "car-detail";
    }

    @GetMapping("/bookings/new")
    public String bookingForm(@RequestParam Long carId, Model model) {
        model.addAttribute("car", carService.findById(carId)
                .orElseThrow(() -> new RuntimeException("Car not found")));
        model.addAttribute("insurances", insuranceService.findAll());
        return "booking-new";
    }

    @GetMapping("/bookings")
    public String bookings(@AuthenticationPrincipal UserDetails userDetails,
                           BookingFilter filter, Model model) {
        if (filter == null) filter = new BookingFilter();
        model.addAttribute("filter", filter);
        if (userDetails != null) {
            final BookingFilter f = filter;
            userService.findByUsername(userDetails.getUsername()).ifPresent(user -> {
                f.setUserId(user.getId());
                model.addAttribute("bookings", bookingService.findFilteredBookings(f));
            });
        } else {
            model.addAttribute("bookings", List.of());
        }
        return "bookings";
    }

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
            ra.addFlashAttribute("docSuccess",
                    "Документ загружен и отправлен на проверку");
        } catch (IllegalArgumentException e) {
            ra.addFlashAttribute("docError", e.getMessage());
        }
        ra.addAttribute("section", "docs");
        return "redirect:/profile";
    }

    @PostMapping("/profile/delete")
    public String deleteAccount(@AuthenticationPrincipal UserDetails userDetails,
                                jakarta.servlet.http.HttpServletRequest request) {
        if (userDetails == null) return "redirect:/login";
        userService.findByUsername(userDetails.getUsername())
                .ifPresent(user -> userService.deleteById(user.getId()));
        try { request.logout(); } catch (Exception ignored) {}
        return "redirect:/login?deleted=true";
    }

    @GetMapping("/chat")
    public String chat(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        if (userDetails == null) return "redirect:/login";
        model.addAttribute("sessionId", 1);
        return "chat";
    }
}