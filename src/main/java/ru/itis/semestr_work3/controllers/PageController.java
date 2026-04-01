package ru.itis.semestr_work3.controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import ru.itis.semestr_work3.exception.ResourceNotFoundException;
import ru.itis.semestr_work3.service.*;

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
                .orElseThrow(() -> new ResourceNotFoundException("Автомобиль не найден")));
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
                           @RequestParam(required = false) String phoneNumber) {
        userService.register(username, email, password, phoneNumber);
        return "redirect:/login";
    }

    @GetMapping("/bookings/new")
    public String bookingForm(@RequestParam Long carId, Model model) {
        model.addAttribute("car", carService.findById(carId)
                .orElseThrow(() -> new ResourceNotFoundException("Автомобиль не найден")));
        model.addAttribute("insurances", insuranceService.findAll());
        return "booking-new";
    }

    @GetMapping("/bookings")
    public String bookings(Model model) {
        model.addAttribute("bookings", bookingService.findAll());
        return "bookings";
    }

    @GetMapping("/favorites")
    public String favorites(Model model) {
        model.addAttribute("favorites", List.of());
        return "favorites";
    }

    @GetMapping("/profile")
    public String profile(Model model) {
        model.addAttribute("bookingsCount", 0);
        model.addAttribute("reviewsCount", 0);
        return "profile";
    }

    @GetMapping("/chat")
    public String chat(Model model) {
        model.addAttribute("sessionId", 1L);
        return "chat";
    }
}