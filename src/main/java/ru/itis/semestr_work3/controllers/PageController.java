package ru.itis.semestr_work3.controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import ru.itis.semestr_work3.service.*;

@Controller
@RequiredArgsConstructor
public class PageController {

    private final CarService carService;
    private final CategoryService categoryService;
    private final InsuranceService insuranceService;
    private final BookingService bookingService;
    private final FavoriteService favoriteService;
    private final ReviewService reviewService;

    @GetMapping("/")
    public String index() {
        return "index";
    }

    @GetMapping("/catalog")
    public String catalog(@RequestParam(required = false) Long category, Model model) {
        if (category != null) {
            model.addAttribute("cars", carService.findByCategory(category));
            model.addAttribute("selectedCategory", category);
        } else {
            model.addAttribute("cars", carService.findAvailable());
        }
        model.addAttribute("categories", categoryService.findAll());
        return "catalog";
    }

    @GetMapping("/cars/{id}")
    public String carDetail(@PathVariable Long id, Model model) {
        model.addAttribute("car", carService.findById(id)
                .orElseThrow(() -> new RuntimeException("Car not found")));
        model.addAttribute("reviews", reviewService.findByCar(id));
        return "car-detail";
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping("/register")
    public String registerForm() {
        return "register";
    }

    @PostMapping("/register")
    public String register(@RequestParam String username,
                           @RequestParam String email,
                           @RequestParam String password,
                           @RequestParam(required = false) String phoneNumber,
                           Model model) {
        try {
            // TODO: вызвать userService.register() после настройки Security
            return "redirect:/login";
        } catch (IllegalArgumentException e) {
            model.addAttribute("error", e.getMessage());
            return "register";
        }
    }

    @GetMapping("/bookings/new")
    public String bookingForm(@RequestParam Long carId, Model model) {
        model.addAttribute("car", carService.findById(carId)
                .orElseThrow(() -> new RuntimeException("Car not found")));
        model.addAttribute("insurances", insuranceService.findAll());
        return "booking-new";
    }

    @GetMapping("/bookings")
    public String bookings(Model model) {
        // TODO: получить userId из Spring Security
        model.addAttribute("bookings", bookingService.findAll());
        return "bookings";
    }

    @GetMapping("/favorites")
    public String favorites(Model model) {
        // TODO: получить userId из Spring Security
        model.addAttribute("favorites", java.util.List.of());
        return "favorites";
    }

    @GetMapping("/profile")
    public String profile(Model model) {
        // TODO: получить user из Spring Security
        model.addAttribute("bookingsCount", 0);
        model.addAttribute("reviewsCount", 0);
        return "profile";
    }

    @GetMapping("/chat")
    public String chat(Model model) {
        // TODO: создать/получить сессию через AiChatService
        model.addAttribute("sessionId", 1);
        return "chat";
    }
}