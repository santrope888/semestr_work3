package ru.itis.semestr_work3.controllers;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import ru.itis.semestr_work3.dto.BookingExtrasRequest;
import ru.itis.semestr_work3.entity.Booking;
import ru.itis.semestr_work3.exception.ResourceNotFoundException;
import ru.itis.semestr_work3.service.BookingService;
import ru.itis.semestr_work3.service.CarService;
import ru.itis.semestr_work3.service.InsuranceService;
import ru.itis.semestr_work3.service.UserService;

import java.time.LocalDate;
import java.util.Set;

@Slf4j
@Controller
@RequiredArgsConstructor
@RequestMapping("/bookings/wizard")
public class BookingWizardController {

    private final CarService carService;
    private final InsuranceService insuranceService;
    private final BookingService bookingService;
    private final UserService userService;

    @GetMapping("/{carId}")
    public String showWizard(@PathVariable Long carId, Model model) {
        var car = carService.findById(carId)
                .orElseThrow(() -> new ResourceNotFoundException("Автомобиль не найден"));

        model.addAttribute("car", car);
        model.addAttribute("insurances", insuranceService.findAll());
        model.addAttribute("locations", BookingService.ALLOWED_LOCATIONS);
        model.addAttribute("gpsPrice", BookingService.GPS_PRICE_PER_DAY);
        model.addAttribute("childSeatPrice", BookingService.CHILD_SEAT_PRICE_PER_DAY);
        model.addAttribute("driverPrice", BookingService.DRIVER_PRICE_PER_DAY);

        return "booking-wizard";
    }

    @PostMapping("/{carId}")
    public String submitWizard(@PathVariable Long carId,
                               @AuthenticationPrincipal UserDetails userDetails,
                               @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
                               @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
                               @RequestParam String pickupLocation,
                               @RequestParam String returnLocation,
                               @RequestParam(required = false) Set<Long> insuranceIds,
                               @RequestParam(required = false, defaultValue = "false") Boolean gpsNavigator,
                               @RequestParam(required = false, defaultValue = "false") Boolean childSeat,
                               @RequestParam(required = false, defaultValue = "false") Boolean driverService,
                               @RequestParam String paymentMethod,
                               RedirectAttributes ra,
                               HttpServletRequest request) {
        if (userDetails == null) {
            return "redirect:/login";
        }

        var user = userService.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new ResourceNotFoundException("Пользователь не найден"));

        BookingExtrasRequest req = BookingExtrasRequest.builder()
                .carId(carId)
                .startDate(startDate)
                .endDate(endDate)
                .pickupLocation(pickupLocation)
                .returnLocation(returnLocation)
                .insuranceIds(insuranceIds)
                .gpsNavigator(gpsNavigator)
                .childSeat(childSeat)
                .driverService(driverService)
                .paymentMethod(paymentMethod)
                .build();

        try {
            Booking booking = bookingService.createWithExtras(req, user.getId());
            return "redirect:/bookings/wizard/success/" + booking.getBookingNumber();
        } catch (IllegalArgumentException e) {
            log.warn("Booking validation failed: {}", e.getMessage());
            ra.addFlashAttribute("error", e.getMessage());
            return "redirect:/bookings/wizard/" + carId;
        }
    }

    @GetMapping("/success/{bookingNumber}")
    public String success(@PathVariable String bookingNumber, Model model) {
        model.addAttribute("bookingNumber", bookingNumber);
        return "booking-success";
    }
}