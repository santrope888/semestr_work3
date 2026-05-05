package ru.itis.semestr_work3.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.itis.semestr_work3.entity.Payment;
import ru.itis.semestr_work3.exception.ResourceNotFoundException;
import ru.itis.semestr_work3.repository.PaymentRepository;

import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class PaymentService {

    private static final String STATUS_PAID = "PAID";
    private static final String STATUS_REFUNDED = "REFUNDED";

    private static final Set<String> ALLOWED_PAYMENT_METHODS = Set.of("CARD", "CASH");

    private final PaymentRepository paymentRepository;

    @Transactional(readOnly = true)
    public Optional<Payment> findById(Long id) {
        return paymentRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public Optional<Payment> findByBooking(Long bookingId) {
        return paymentRepository.findByBookingId(bookingId);
    }

    public Payment pay(Long paymentId, String method) {
        if (method == null || method.isBlank()) {
            throw new IllegalArgumentException("Нужно указать способ оплаты");
        }

        String normalizedMethod = method.trim().toUpperCase(Locale.ROOT);
        if (!ALLOWED_PAYMENT_METHODS.contains(normalizedMethod)) {
            throw new IllegalArgumentException(
                    "Недопустимый способ оплаты: " + method
                            + ". Допустимые: " + ALLOWED_PAYMENT_METHODS);
        }

        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Платёж не найден"));

        if (STATUS_PAID.equals(payment.getStatus())) {
            log.warn("Попытка повторной оплаты платежа #{}", paymentId);
            throw new IllegalStateException("Платёж уже оплачен");
        }
        if (STATUS_REFUNDED.equals(payment.getStatus())) {
            log.warn("Попытка оплатить возвращённый платёж #{}", paymentId);
            throw new IllegalStateException("Возвращённый платёж нельзя оплатить повторно");
        }

        payment.setStatus(STATUS_PAID);
        payment.setMethod(normalizedMethod);
        payment.setPaidAt(LocalDateTime.now());
        log.info("Платёж #{} оплачен методом {}", paymentId, normalizedMethod);
        return paymentRepository.save(payment);
    }

    public Payment refund(Long paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Платёж не найден"));

        if (STATUS_REFUNDED.equals(payment.getStatus())) {
            log.warn("Платёж #{} уже был возвращён, повторный refund игнорируется", paymentId);
            return payment;
        }

        payment.setStatus(STATUS_REFUNDED);
        log.info("Платёж #{} возвращён", paymentId);
        return paymentRepository.save(payment);
    }
}