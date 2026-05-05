package ru.itis.semestr_work3.controllers.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.itis.semestr_work3.converter.PaymentMapper;
import ru.itis.semestr_work3.dto.PaymentDto;
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
    private final PaymentMapper paymentMapper;

    @Operation(summary = "Получить платёж по ID")
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('ADMIN') or @paymentSecurity.isOwner(#id, principal)")
    public PaymentDto findById(@PathVariable Long id) {
        Payment payment = paymentService.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Платёж не найден"));
        return paymentMapper.toDto(payment);
    }

    @Operation(summary = "Получить платёж по ID бронирования")
    @GetMapping("/booking/{bookingId}")
    @PreAuthorize("hasAuthority('ADMIN') or @paymentSecurity.isOwnerByBooking(#bookingId, principal)")
    public PaymentDto findByBooking(@PathVariable Long bookingId) {
        Payment payment = paymentService.findByBooking(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Платёж по бронированию не найден"));
        return paymentMapper.toDto(payment);
    }

    @Operation(summary = "Оплатить (метод: CARD или CASH)")
    @PostMapping("/{id}/pay")
    @PreAuthorize("hasAuthority('ADMIN') or @paymentSecurity.isOwner(#id, principal)")
    public PaymentDto pay(@PathVariable Long id,
                          @RequestParam String method) {
        return paymentMapper.toDto(paymentService.pay(id, method));
    }

    @Operation(summary = "Вернуть платёж (только ADMIN)")
    @PostMapping("/{id}/refund")
    @PreAuthorize("hasAuthority('ADMIN')")
    public PaymentDto refund(@PathVariable Long id) {
        return paymentMapper.toDto(paymentService.refund(id));
    }

    @Operation(summary = "Конвертировать сумму из RUB в указанную валюту")
    @GetMapping("/convert")
    public Map<String, Object> convert(@RequestParam int amount,
                                       @RequestParam String currency) {
        double converted = currencyService.convert(amount, currency);
        return Map.of("original", amount, "currency", currency, "converted", converted);
    }
}