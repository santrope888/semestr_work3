package ru.itis.semestr_work3.controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import ru.itis.semestr_work3.entity.Booking;
import ru.itis.semestr_work3.entity.Payment;
import ru.itis.semestr_work3.exception.ResourceNotFoundException;
import ru.itis.semestr_work3.service.BookingService;
import ru.itis.semestr_work3.service.PaymentService;

@Controller
@RequiredArgsConstructor
public class PaymentPageController {

    private final PaymentService paymentService;
    private final BookingService bookingService;

    @GetMapping("/payments/{id}")
    @PreAuthorize("hasAuthority('ADMIN') or @paymentSecurity.isOwner(#id, principal)")
    public String paymentPage(@PathVariable Long id,
                              @AuthenticationPrincipal UserDetails userDetails,
                              Model model) {
        if (userDetails == null) return "redirect:/login";

        Payment payment = paymentService.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Платёж не найден"));

        Booking booking = bookingService.findById(payment.getBooking().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Бронирование не найдено"));

        model.addAttribute("payment", payment);
        model.addAttribute("booking", booking);
        return "payment";
    }
}