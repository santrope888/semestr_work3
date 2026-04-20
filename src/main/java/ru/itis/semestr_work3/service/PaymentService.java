package ru.itis.semestr_work3.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.itis.semestr_work3.entity.Payment;
import ru.itis.semestr_work3.exception.ResourceNotFoundException;
import ru.itis.semestr_work3.repository.PaymentRepository;

import java.time.LocalDateTime;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class PaymentService {

    private final PaymentRepository paymentRepository;

    @Transactional(readOnly = true)
    public Optional<Payment> findById(Long id) {
        return paymentRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public Optional<Payment> findByBooking(Long bookingId) {
        return paymentRepository.findByBooking(bookingId);
    }

    public Payment pay(Long paymentId, String method) {
        if (method == null || method.isBlank()) {
            throw new IllegalArgumentException("Нужно указать способ оплаты");
        }

        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Платёж не найден"));

        payment.setStatus("PAID");
        log.info("Платёж #{} оплачен методом {}", paymentId, method);
        payment.setMethod(method);
        payment.setPaidAt(LocalDateTime.now());
        return paymentRepository.save(payment);
    }

    public Payment refund(Long paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Платёж не найден"));

        payment.setStatus("REFUNDED");
        return paymentRepository.save(payment);
    }
}