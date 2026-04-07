package ru.itis.semestr_work3.controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import ru.itis.semestr_work3.dto.CarFilter;
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
    public String bookings(Model model) {
        model.addAttribute("bookings", bookingService.findAll());
        return "bookings";
    }

    @GetMapping("/favorites")
    public String favorites(Model model) {
        model.addAttribute("favorites", java.util.List.of());
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
        model.addAttribute("sessionId", 1);
        return "chat";
    }
}