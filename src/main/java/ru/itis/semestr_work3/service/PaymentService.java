package ru.itis.semestr_work3.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.itis.semestr_work3.entity.Payment;
import ru.itis.semestr_work3.repository.PaymentRepository;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;

    public Optional<Payment> findById(Long id) {
        return paymentRepository.findById(id);
    }

    public Optional<Payment> findByBooking(Long bookingId) {
        return paymentRepository.findByBooking(bookingId);
    }

    public Payment pay(Long paymentId, String method) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new RuntimeException("Payment not found"));
        payment.setStatus("PAID");
        payment.setMethod(method);
        payment.setPaidAt(LocalDateTime.now());
        return paymentRepository.save(payment);
    }

    public Payment refund(Long paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new RuntimeException("Payment not found"));
        payment.setStatus("REFUNDED");
        return paymentRepository.save(payment);
    }
}