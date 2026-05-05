package ru.itis.semestr_work3.security;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import ru.itis.semestr_work3.entity.Payment;
import ru.itis.semestr_work3.service.PaymentService;

@Component("paymentSecurity")
@RequiredArgsConstructor
public class PaymentSecurity {

    private final PaymentService paymentService;

    public boolean isOwner(Long paymentId, UserDetails principal) {
        if (paymentId == null || principal == null) {
            return false;
        }
        return paymentService.findById(paymentId)
                .map(Payment::getBooking)
                .map(b -> b.getUser().getUsername().equals(principal.getUsername()))
                .orElse(false);
    }

    public boolean isOwnerByBooking(Long bookingId, UserDetails principal) {
        if (bookingId == null || principal == null) {
            return false;
        }
        return paymentService.findByBooking(bookingId)
                .map(Payment::getBooking)
                .map(b -> b.getUser().getUsername().equals(principal.getUsername()))
                .orElse(false);
    }
}
