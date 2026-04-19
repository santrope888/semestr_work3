package ru.itis.semestr_work3.controllers.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
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
    public PaymentDto findById(@PathVariable Long id,
                               @AuthenticationPrincipal UserDetails principal) {
        Payment payment = paymentService.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Платёж не найден"));
        checkOwner(payment, principal);
        return paymentMapper.toDto(payment);
    }

    @Operation(summary = "Получить платёж по ID бронирования")
    @GetMapping("/booking/{bookingId}")
    public PaymentDto findByBooking(@PathVariable Long bookingId,
                                    @AuthenticationPrincipal UserDetails principal) {
        Payment payment = paymentService.findByBooking(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Платёж по бронированию не найден"));
        checkOwner(payment, principal);
        return paymentMapper.toDto(payment);
    }

    @Operation(summary = "Оплатить (метод: CARD или CASH)")
    @PostMapping("/{id}/pay")
    public PaymentDto pay(@PathVariable Long id,
                          @RequestParam String method,
                          @AuthenticationPrincipal UserDetails principal) {
        Payment payment = paymentService.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Платёж не найден"));
        checkOwner(payment, principal);
        return paymentMapper.toDto(paymentService.pay(id, method));
    }

    @Operation(summary = "Вернуть платёж")
    @PostMapping("/{id}/refund")
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

    private void checkOwner(Payment payment, UserDetails principal) {
        boolean isAdmin = principal.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ADMIN"));
        if (!isAdmin && !payment.getBooking().getUser().getUsername().equals(principal.getUsername())) {
            throw new AccessDeniedException("Доступ запрещён");
        }
    }
}