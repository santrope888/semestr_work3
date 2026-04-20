package ru.itis.semestr_work3.controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import ru.itis.semestr_work3.dto.BookingFilter;
import ru.itis.semestr_work3.dto.CarFilter;
import ru.itis.semestr_work3.entity.Booking;
import ru.itis.semestr_work3.entity.Car;
import ru.itis.semestr_work3.entity.ChatSession;
import ru.itis.semestr_work3.entity.Payment;
import ru.itis.semestr_work3.entity.User;
import ru.itis.semestr_work3.exception.ResourceNotFoundException;
import ru.itis.semestr_work3.service.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

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
    private final PaymentService paymentService;
    private final AiChatService aiChatService;

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
                          @PageableDefault(size = 12) Pageable pageable,
                          Model model) {
        var carsPage = carService.findCars(filter, pageable);
        var cars = carsPage.getContent();
        model.addAttribute("cars", cars);
        model.addAttribute("carsPage", carsPage);
        model.addAttribute("filter", filter);
        model.addAttribute("categories", categoryService.findAll());
        model.addAttribute("brands", carService.findDistinctBrands());
        model.addAttribute("colors", carService.findDistinctColors());

        List<Long> carIds = cars.stream().map(Car::getId).toList();
        model.addAttribute("avgRatings", reviewService.getAverageRatings(carIds));
        model.addAttribute("reviewCounts", reviewService.getReviewCounts(carIds));

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
            ru.itis.semestr_work3.entity.Review review = new ru.itis.semestr_work3.entity.Review();
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

    @GetMapping("/bookings/new")
    public String bookingForm(@RequestParam Long carId, Model model) {
        model.addAttribute("car", carService.findById(carId)
                .orElseThrow(() -> new ResourceNotFoundException("Автомобиль не найден")));
        model.addAttribute("insurances", insuranceService.findAll());
        return "booking-new";
    }

    @PostMapping("/bookings")
    public String createBooking(@AuthenticationPrincipal UserDetails userDetails,
                                @RequestParam Long carId,
                                @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
                                @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
                                @RequestParam(required = false) Set<Long> insuranceIds,
                                RedirectAttributes ra) {
        if (userDetails == null) return "redirect:/login";

        userService.findByUsername(userDetails.getUsername()).ifPresent(user -> {
            Booking booking = new Booking();
            Car car = new Car();
            car.setId(carId);
            booking.setCar(car);
            booking.setUser(user);
            booking.setStartDate(startDate);
            booking.setEndDate(endDate);
            bookingService.create(booking, insuranceIds != null ? insuranceIds : Set.of());
        });

        ra.addFlashAttribute("success", "Бронирование успешно создано");
        return "redirect:/bookings";
    }

    @GetMapping("/bookings")
    public String bookings(@AuthenticationPrincipal UserDetails userDetails,
                           BookingFilter filter,
                           Model model) {
        if (filter == null) filter = new BookingFilter();
        model.addAttribute("filter", filter);
        if (userDetails != null) {
            final BookingFilter f = filter;
            userService.findByUsername(userDetails.getUsername()).ifPresent(user -> {
                f.setUserId(user.getId());
                model.addAttribute("bookings", bookingService.findFilteredBookings(f));
                List<Long> reviewedCarIds = reviewService.findByUser(user.getId())
                        .stream().map(r -> r.getCar().getId()).toList();
                model.addAttribute("reviewedCarIds", reviewedCarIds);
            });
        } else {
            model.addAttribute("bookings", List.of());
        }
        return "bookings";
    }

    @GetMapping("/payments/{id}")
    public String paymentPage(@PathVariable Long id,
                              @AuthenticationPrincipal UserDetails userDetails,
                              Model model) {
        if (userDetails == null) return "redirect:/login";

        Payment payment = paymentService.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Платёж не найден"));

        checkPaymentOwner(payment, userDetails);

        Booking booking = bookingService.findById(payment.getBooking().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Бронирование не найдено"));

        model.addAttribute("payment", payment);
        model.addAttribute("booking", booking);
        return "payment";
    }

    @PostMapping("/payments/{id}/pay")
    public String payPayment(@PathVariable Long id,
                             @RequestParam String method,
                             @AuthenticationPrincipal UserDetails userDetails,
                             RedirectAttributes ra) {
        if (userDetails == null) return "redirect:/login";

        Payment payment = paymentService.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Платёж не найден"));

        checkPaymentOwner(payment, userDetails);

        paymentService.pay(id, method);
        ra.addFlashAttribute("success", "Оплата прошла успешно");
        return "redirect:/bookings";
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
            ra.addFlashAttribute("docSuccess", "Документ загружен и отправлен на проверку");
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

        userService.findByUsername(userDetails.getUsername()).ifPresent(user -> {
            List<ChatSession> sessions = aiChatService.getUserSessions(user.getId());
            ChatSession session = sessions.isEmpty()
                    ? aiChatService.createSession(user, "Подбор автомобиля")
                    : sessions.get(0);
            model.addAttribute("sessionId", session.getId());
            model.addAttribute("messages", aiChatService.getSessionMessages(session.getId()));
        });

        return "chat";
    }

    @PostMapping("/bookings/{id}/cancel")
    public String cancelBooking(@PathVariable Long id,
                                @AuthenticationPrincipal UserDetails userDetails,
                                RedirectAttributes ra) {
        if (userDetails == null) return "redirect:/login";

        userService.findByUsername(userDetails.getUsername()).ifPresent(user -> {
            bookingService.cancel(id, user.getId());
            ra.addFlashAttribute("success", "Бронирование отменено");
        });
        return "redirect:/bookings";
    }

    @GetMapping("/admin/bookings")
    public String adminBookings(BookingFilter filter, Model model) {
        if (filter == null) filter = new BookingFilter();
        model.addAttribute("filter", filter);
        model.addAttribute("bookings", bookingService.findFilteredBookings(filter));
        return "admin/bookings";
    }

    @PostMapping("/admin/bookings/{id}/confirm")
    public String adminConfirm(@PathVariable Long id, RedirectAttributes ra) {
        bookingService.confirm(id);
        ra.addFlashAttribute("success", "Бронирование подтверждено");
        return "redirect:/admin/bookings";
    }

    @PostMapping("/admin/bookings/{id}/complete")
    public String adminComplete(@PathVariable Long id, RedirectAttributes ra) {
        bookingService.complete(id);
        ra.addFlashAttribute("success", "Бронирование завершено");
        return "redirect:/admin/bookings";
    }

    @PostMapping("/admin/bookings/{id}/cancel")
    public String adminCancel(@PathVariable Long id, RedirectAttributes ra) {
        Booking booking = bookingService.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Бронирование не найдено"));
        bookingService.cancel(id, booking.getUser().getId());
        ra.addFlashAttribute("success", "Бронирование отменено");
        return "redirect:/admin/bookings";
    }

    @PostMapping("/admin/bookings/{id}/refund")
    public String adminRefund(@PathVariable Long id, RedirectAttributes ra) {
        Booking booking = bookingService.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Бронирование не найдено"));
        if (booking.getPayment() != null) {
            paymentService.refund(booking.getPayment().getId());
        }
        ra.addFlashAttribute("success", "Возврат оформлен");
        return "redirect:/admin/bookings";
    }

    private void checkPaymentOwner(Payment payment, UserDetails principal) {
        boolean isAdmin = principal.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ADMIN"));
        if (!isAdmin && !payment.getBooking().getUser().getUsername().equals(principal.getUsername())) {
            throw new AccessDeniedException("Доступ запрещён");
        }
    }
}