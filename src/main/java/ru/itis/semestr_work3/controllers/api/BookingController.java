package ru.itis.semestr_work3.controllers.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.itis.semestr_work3.converter.BookingMapper;
import ru.itis.semestr_work3.dto.BookingDto;
import ru.itis.semestr_work3.entity.Booking;
import ru.itis.semestr_work3.exception.ResourceNotFoundException;
import ru.itis.semestr_work3.service.BookingService;

import java.util.List;
import java.util.Map;
import java.util.Set;

@Tag(name = "Bookings", description = "Управление бронированиями")
@RestController
@RequestMapping("/api/bookings")
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookingService;
    private final BookingMapper bookingMapper;

    @Operation(summary = "Занятые периоды автомобиля")
    @GetMapping("/car/{carId}/booked-periods")
    public List<Map<String, String>> bookedPeriods(@PathVariable Long carId) {
        return bookingService.getBookedPeriods(carId);
    }

    @Operation(summary = "Получить все бронирования")
    @GetMapping
    public List<BookingDto> findAll() {
        return bookingMapper.toDtoList(bookingService.findAll());
    }

    @Operation(summary = "Получить бронирование по ID")
    @GetMapping("/{id}")
    public BookingDto findById(@PathVariable Long id) {
        Booking booking = bookingService.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Бронирование не найдено"));
        return bookingMapper.toDto(booking);
    }

    @Operation(summary = "Создать бронирование")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public BookingDto create(@Valid @RequestBody Booking booking,
                             @RequestParam(required = false) Set<Long> insuranceIds) {
        return bookingMapper.toDto(bookingService.create(booking, insuranceIds));
    }

    @Operation(summary = "Подтвердить бронирование")
    @PostMapping("/{id}/confirm")
    public BookingDto confirm(@PathVariable Long id) {
        return bookingMapper.toDto(bookingService.confirm(id));
    }

    @Operation(summary = "Отменить бронирование")
    @PostMapping("/{id}/cancel")
    public BookingDto cancel(@PathVariable Long id, @RequestParam Long userId) {
        return bookingMapper.toDto(bookingService.cancel(id, userId));
    }

    @Operation(summary = "Завершить бронирование")
    @PostMapping("/{id}/complete")
    public BookingDto complete(@PathVariable Long id) {
        return bookingMapper.toDto(bookingService.complete(id));
    }
}