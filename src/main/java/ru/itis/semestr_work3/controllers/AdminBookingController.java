package ru.itis.semestr_work3.controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import ru.itis.semestr_work3.dto.BookingFilter;
import ru.itis.semestr_work3.entity.Booking;
import ru.itis.semestr_work3.exception.ResourceNotFoundException;
import ru.itis.semestr_work3.service.BookingService;
import ru.itis.semestr_work3.service.PaymentService;

@Controller
@RequestMapping("/admin/bookings")
@RequiredArgsConstructor
public class AdminBookingController {

    private final BookingService bookingService;
    private final PaymentService paymentService;

    @GetMapping
    public String adminBookings(BookingFilter filter, Model model) {
        if (filter == null) filter = new BookingFilter();
        model.addAttribute("filter", filter);
        model.addAttribute("bookings", bookingService.findFilteredBookings(filter));
        return "admin/bookings";
    }

    @PostMapping("/{id}/confirm")
    public String adminConfirm(@PathVariable Long id, RedirectAttributes ra) {
        bookingService.confirm(id);
        ra.addFlashAttribute("success", "Бронирование подтверждено");
        return "redirect:/admin/bookings";
    }

    @PostMapping("/{id}/complete")
    public String adminComplete(@PathVariable Long id, RedirectAttributes ra) {
        bookingService.complete(id);
        ra.addFlashAttribute("success", "Бронирование завершено");
        return "redirect:/admin/bookings";
    }

    @PostMapping("/{id}/cancel")
    public String adminCancel(@PathVariable Long id, RedirectAttributes ra) {
        Booking booking = bookingService.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Бронирование не найдено"));
        bookingService.cancel(id, booking.getUser().getId());
        ra.addFlashAttribute("success", "Бронирование отменено");
        return "redirect:/admin/bookings";
    }

    @PostMapping("/{id}/refund")
    public String adminRefund(@PathVariable Long id, RedirectAttributes ra) {
        Booking booking = bookingService.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Бронирование не найдено"));
        if (booking.getPayment() != null) {
            paymentService.refund(booking.getPayment().getId());
        }
        ra.addFlashAttribute("success", "Возврат оформлен");
        return "redirect:/admin/bookings";
    }
}
