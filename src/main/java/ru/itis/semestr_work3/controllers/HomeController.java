package ru.itis.semestr_work3.controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import ru.itis.semestr_work3.dto.CarFilter;
import ru.itis.semestr_work3.entity.Car;
import ru.itis.semestr_work3.entity.Review;
import ru.itis.semestr_work3.exception.ResourceNotFoundException;
import ru.itis.semestr_work3.service.CarService;
import ru.itis.semestr_work3.service.CategoryService;
import ru.itis.semestr_work3.service.FavoriteService;
import ru.itis.semestr_work3.service.ReviewService;
import ru.itis.semestr_work3.service.UserService;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Controller
@RequiredArgsConstructor
public class HomeController {

    private static final int CATALOG_PAGE_SIZE = 12;

    private final CarService carService;
    private final CategoryService categoryService;
    private final ReviewService reviewService;
    private final FavoriteService favoriteService;
    private final UserService userService;

    @GetMapping("/")
    public String index(Model model) {
        CarFilter popularFilter = new CarFilter();
        popularFilter.setSearch("Taycan");
        popularFilter.setAvailable(true);
        List<Car> popularCars = carService.findCars(
                popularFilter, PageRequest.of(0, 4)
        ).getContent();

        List<Car> fleetCars = carService.findAvailable().stream()
                .filter(c -> c.getModel() == null
                        || !c.getModel().toLowerCase().contains("taycan"))
                .limit(4)
                .toList();

        model.addAttribute("popularCars", popularCars);
        model.addAttribute("fleetCars", fleetCars);
        return "index";
    }

    @GetMapping("/catalog")
    public String catalog(CarFilter filter,
                          @PageableDefault(size = CATALOG_PAGE_SIZE) Pageable pageable,
                          @AuthenticationPrincipal UserDetails userDetails,
                          Model model) {
        var carsPage = carService.findCars(filter, pageable);
        var cars = carsPage.getContent();
        model.addAttribute("cars", cars);
        model.addAttribute("carsPage", carsPage);
        model.addAttribute("filter", filter);
        model.addAttribute("categories", categoryService.findAll());
        model.addAttribute("brands", carService.findDistinctBrands());
        model.addAttribute("colors", carService.findDistinctColors());
        model.addAttribute("transmissions", carService.findDistinctTransmissions());
        model.addAttribute("drives", carService.findDistinctDrives());

        List<Long> carIds = cars.stream().map(Car::getId).toList();
        model.addAttribute("avgRatings", reviewService.getAverageRatings(carIds));
        model.addAttribute("reviewCounts", reviewService.getReviewCounts(carIds));

        Set<Long> favoriteIds = Collections.emptySet();
        if (userDetails != null) {
            var userOpt = userService.findByUsername(userDetails.getUsername());
            if (userOpt.isPresent()) {
                favoriteIds = favoriteService.findByUser(userOpt.get().getId()).stream()
                        .map(f -> f.getCar().getId())
                        .collect(Collectors.toSet());
            }
        }
        model.addAttribute("favoriteIds", favoriteIds);

        return "catalog";
    }

    @GetMapping("/cars/{id}")
    public String carDetail(@PathVariable Long id,
                            @AuthenticationPrincipal UserDetails userDetails,
                            Model model) {
        model.addAttribute("car", carService.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Автомобиль не найден")));
        model.addAttribute("reviews", reviewService.findByCar(id));
        model.addAttribute("avgRating", reviewService.getAverageRating(id));
        model.addAttribute("reviewCount", reviewService.getReviewCount(id));

        boolean isFavorite = false;
        if (userDetails != null) {
            var userOpt = userService.findByUsername(userDetails.getUsername());
            if (userOpt.isPresent()) {
                isFavorite = favoriteService.isFavorite(userOpt.get().getId(), id);
            }
        }
        model.addAttribute("isFavorite", isFavorite);

        return "car-detail";
    }

    @PostMapping("/cars/{id}/reviews")
    public String submitReview(@PathVariable Long id,
                               @RequestParam Integer rating,
                               @RequestParam(required = false) String comment,
                               @AuthenticationPrincipal UserDetails userDetails,
                               RedirectAttributes ra) {
        if (userDetails == null) return "redirect:/login";

        userService.findByUsername(userDetails.getUsername()).ifPresent(user -> {
            Review review = new Review();
            review.setUser(user);
            Car car = new Car();
            car.setId(id);
            review.setCar(car);
            review.setRating(rating);
            review.setComment(comment);
            try {
                reviewService.create(review);
                ra.addFlashAttribute("reviewSuccess", "Отзыв опубликован!");
            } catch (IllegalArgumentException e) {
                ra.addFlashAttribute("reviewError", e.getMessage());
            }
        });

        return "redirect:/bookings";
    }
}