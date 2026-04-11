package ru.itis.semestr_work3.controllers.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.itis.semestr_work3.entity.Booking;
import ru.itis.semestr_work3.exception.ResourceNotFoundException;
import ru.itis.semestr_work3.service.BookingService;

import java.util.List;
import java.util.Set;

@Tag(name = "Bookings", description = "Управление бронированиями")
@RestController
@RequestMapping("/api/bookings")
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookingService;

    @Operation(summary = "Получить все бронирования")
    @GetMapping
    public List<Booking> findAll() {
        return bookingService.findAll();
    }

    @Operation(summary = "Получить бронирование по ID")
    @GetMapping("/{id}")
    public Booking findById(@PathVariable Long id) {
        return bookingService.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Бронирование не найдено"));
    }

    @Operation(summary = "Создать бронирование")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Booking create(@Valid @RequestBody Booking booking,
                          @RequestParam(required = false) Set<Long> insuranceIds) {
        return bookingService.create(booking, insuranceIds);
    }

    @Operation(summary = "Подтвердить бронирование")
    @PostMapping("/{id}/confirm")
    public Booking confirm(@PathVariable Long id) {
        return bookingService.confirm(id);
    }

    @Operation(summary = "Отменить бронирование")
    @PostMapping("/{id}/cancel")
    public Booking cancel(@PathVariable Long id, @RequestParam Long userId) {
        return bookingService.cancel(id, userId);
    }

    @Operation(summary = "Завершить бронирование")
    @PostMapping("/{id}/complete")
    public Booking complete(@PathVariable Long id) {
        return bookingService.complete(id);
    }
}