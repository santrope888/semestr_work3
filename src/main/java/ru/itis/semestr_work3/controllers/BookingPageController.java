package ru.itis.semestr_work3.controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import ru.itis.semestr_work3.dto.BookingFilter;
import ru.itis.semestr_work3.entity.Booking;
import ru.itis.semestr_work3.entity.Car;
import ru.itis.semestr_work3.service.BookingService;
import ru.itis.semestr_work3.service.CarService;
import ru.itis.semestr_work3.service.ReviewService;
import ru.itis.semestr_work3.service.UserService;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

@Controller
@RequiredArgsConstructor
public class BookingPageController {

    private final CarService carService;
    private final BookingService bookingService;
    private final ReviewService reviewService;
    private final UserService userService;

    @GetMapping("/bookings/new")
    public String bookingForm(@RequestParam Long carId) {
        return "redirect:/bookings/wizard/" + carId;
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
}
