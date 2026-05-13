package ru.itis.semestr_work3.controllers;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import ru.itis.semestr_work3.dto.BookingFilter;
import ru.itis.semestr_work3.entity.Booking;
import ru.itis.semestr_work3.exception.ResourceNotFoundException;
import ru.itis.semestr_work3.service.BookingService;
import ru.itis.semestr_work3.service.PaymentService;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

@Controller
@RequestMapping("/admin/bookings")
@RequiredArgsConstructor
public class AdminBookingController {

    private final BookingService bookingService;
    private final PaymentService paymentService;

    @GetMapping
    public String adminBookings(BookingFilter filter, Model model) {
        if (filter == null) {
            filter = new BookingFilter();
        }

        model.addAttribute("filter", filter);
        model.addAttribute("bookings", bookingService.findFilteredBookings(filter));
        model.addAttribute("today", LocalDate.now());

        return "admin/bookings";
    }

    @PostMapping("/{id}/confirm")
    public Object adminConfirm(@PathVariable Long id,
                               HttpServletRequest request,
                               RedirectAttributes ra) {
        try {
            Booking booking = bookingService.confirm(id);
            return successResponse(request, ra, "Бронирование подтверждено", booking);
        } catch (ResourceNotFoundException | IllegalStateException e) {
            return errorResponse(request, ra, e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    @PostMapping("/{id}/complete")
    public Object adminComplete(@PathVariable Long id,
                                HttpServletRequest request,
                                RedirectAttributes ra) {
        try {
            Booking booking = bookingService.complete(id);
            return successResponse(request, ra, "Бронирование завершено", booking);
        } catch (ResourceNotFoundException | IllegalStateException e) {
            return errorResponse(request, ra, e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    @PostMapping("/{id}/cancel")
    public Object adminCancel(@PathVariable Long id,
                              HttpServletRequest request,
                              RedirectAttributes ra) {
        try {
            Booking booking = bookingService.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Бронирование не найдено"));

            Booking updatedBooking = bookingService.cancel(id, booking.getUser().getId());
            return successResponse(request, ra, "Бронирование отменено", updatedBooking);
        } catch (ResourceNotFoundException | IllegalStateException | SecurityException e) {
            return errorResponse(request, ra, e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    @PostMapping("/{id}/refund")
    public Object adminRefund(@PathVariable Long id,
                              HttpServletRequest request,
                              RedirectAttributes ra) {
        try {
            Booking booking = bookingService.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Бронирование не найдено"));

            if (booking.getPayment() == null) {
                throw new IllegalStateException("У бронирования нет связанного платежа");
            }

            paymentService.refund(booking.getPayment().getId());

            Booking updatedBooking = bookingService.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Бронирование не найдено"));

            return successResponse(request, ra, "Возврат оформлен", updatedBooking);
        } catch (ResourceNotFoundException | IllegalStateException e) {
            return errorResponse(request, ra, e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    private Object successResponse(HttpServletRequest request,
                                   RedirectAttributes ra,
                                   String message,
                                   Booking booking) {
        if (isAjax(request)) {
            return ResponseEntity.ok(toPayload(message, booking));
        }

        ra.addFlashAttribute("success", message);
        return "redirect:/admin/bookings";
    }

    private Object errorResponse(HttpServletRequest request,
                                 RedirectAttributes ra,
                                 String message,
                                 HttpStatus status) {
        if (isAjax(request)) {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("success", false);
            body.put("message", message);
            return ResponseEntity.status(status).body(body);
        }

        ra.addFlashAttribute("error", message);
        return "redirect:/admin/bookings";
    }

    private boolean isAjax(HttpServletRequest request) {
        return "XMLHttpRequest".equals(request.getHeader("X-Requested-With"));
    }

    private Map<String, Object> toPayload(String message, Booking booking) {
        Map<String, Object> body = new LinkedHashMap<>();

        String status = booking.getStatus();
        String paymentStatus = booking.getPayment() != null
                ? booking.getPayment().getStatus()
                : null;

        boolean canConfirm = "PENDING".equals(status);
        boolean canCancel = "PENDING".equals(status) || "CONFIRMED".equals(status);
        boolean canComplete = "CONFIRMED".equals(status)
                && !booking.getEndDate().isAfter(LocalDate.now());

        body.put("success", true);
        body.put("message", message);

        body.put("id", booking.getId());
        body.put("username", booking.getUser().getUsername());

        body.put("carBrand", booking.getCar().getBrand());
        body.put("carModel", booking.getCar().getModel());

        body.put("startDate", booking.getStartDate().toString());
        body.put("endDate", booking.getEndDate().toString());
        body.put("totalPrice", booking.getTotalPrice());

        body.put("status", status);
        body.put("paymentStatus", paymentStatus);

        body.put("canConfirm", canConfirm);
        body.put("canComplete", canComplete);
        body.put("canCancel", canCancel);

        return body;
    }
}