package ru.itis.semestr_work3.controllers.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import ru.itis.semestr_work3.entity.Payment;
import ru.itis.semestr_work3.exception.ResourceNotFoundException;
import ru.itis.semestr_work3.service.CurrencyService;
import ru.itis.semestr_work3.service.PaymentService;

import java.util.Map;

@Tag(name = "Payments", description = "Управление платежами и конвертация валют")
@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;
    private final CurrencyService currencyService;

    @Operation(summary = "Получить платёж по ID")
    @GetMapping("/{id}")
    public Payment findById(@PathVariable Long id) {
        return paymentService.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Платёж не найден"));
    }

    @Operation(summary = "Получить платёж по ID бронирования")
    @GetMapping("/booking/{bookingId}")
    public Payment findByBooking(@PathVariable Long bookingId) {
        return paymentService.findByBooking(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Платёж по бронированию не найден"));
    }

    @Operation(summary = "Оплатить (метод: CARD или CASH)")
    @PostMapping("/{id}/pay")
    public Payment pay(@PathVariable Long id, @RequestParam String method) {
        return paymentService.pay(id, method);
    }

    @Operation(summary = "Вернуть платёж")
    @PostMapping("/{id}/refund")
    public Payment refund(@PathVariable Long id) {
        return paymentService.refund(id);
    }

    @Operation(summary = "Конвертировать сумму из RUB в указанную валюту")
    @GetMapping("/convert")
    public Map<String, Object> convert(@RequestParam int amount,
                                       @RequestParam String currency) {
        double converted = currencyService.convert(amount, currency);
        return Map.of("original", amount, "currency", currency, "converted", converted);
    }
}