package ru.itis.semestr_work3.controllers.api;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import ru.itis.semestr_work3.entity.Payment;
import ru.itis.semestr_work3.exception.ResourceNotFoundException;
import ru.itis.semestr_work3.service.CurrencyService;
import ru.itis.semestr_work3.service.PaymentService;

import java.util.Map;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;
    private final CurrencyService currencyService;

    @GetMapping("/{id}")
    public Payment findById(@PathVariable Long id) {
        return paymentService.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Платёж не найден"));
    }

    @GetMapping("/booking/{bookingId}")
    public Payment findByBooking(@PathVariable Long bookingId) {
        return paymentService.findByBooking(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Платёж по бронированию не найден"));
    }

    @PostMapping("/{id}/pay")
    public Payment pay(@PathVariable Long id,
                       @RequestParam String method) {
        return paymentService.pay(id, method);
    }

    @PostMapping("/{id}/refund")
    public Payment refund(@PathVariable Long id) {
        return paymentService.refund(id);
    }

    @GetMapping("/convert")
    public Map<String, Object> convert(@RequestParam int amount,
                                       @RequestParam String currency) {
        double converted = currencyService.convert(amount, currency);
        return Map.of(
                "original", amount,
                "currency", currency,
                "converted", converted
        );
    }
}