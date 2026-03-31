package ru.itis.semestr_work3.controllers.api;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.itis.semestr_work3.entity.Payment;
import ru.itis.semestr_work3.service.CurrencyService;
import ru.itis.semestr_work3.service.PaymentService;

import java.util.Map;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentService paymentService;
    private final CurrencyService currencyService;

    public PaymentController(PaymentService paymentService, CurrencyService currencyService) {
        this.paymentService = paymentService;
        this.currencyService = currencyService;
    }

    @GetMapping("/{id}")
    public ResponseEntity<Payment> findById(@PathVariable Long id) {
        return paymentService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/booking/{bookingId}")
    public ResponseEntity<Payment> findByBooking(@PathVariable Long bookingId) {
        return paymentService.findByBooking(bookingId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/{id}/pay")
    public Payment pay(@PathVariable Long id, @RequestParam String method) {
        return paymentService.pay(id, method);
    }

    @PostMapping("/{id}/refund")
    public Payment refund(@PathVariable Long id) {
        return paymentService.refund(id);
    }

    @GetMapping("/convert")
    public Map<String, Object> convert(@RequestParam int amount, @RequestParam String currency) {
        double converted = currencyService.convert(amount, currency);
        return Map.of(
                "original", amount,
                "currency", currency,
                "converted", converted
        );
    }
}