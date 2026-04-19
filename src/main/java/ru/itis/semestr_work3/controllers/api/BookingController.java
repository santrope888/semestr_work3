package ru.itis.semestr_work3.controllers.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import ru.itis.semestr_work3.converter.BookingMapper;
import ru.itis.semestr_work3.dto.BookingDto;
import ru.itis.semestr_work3.dto.BookingRequest;
import ru.itis.semestr_work3.entity.Booking;
import ru.itis.semestr_work3.entity.Car;
import ru.itis.semestr_work3.entity.User;
import ru.itis.semestr_work3.exception.ResourceNotFoundException;
import ru.itis.semestr_work3.service.BookingService;
import ru.itis.semestr_work3.service.UserService;

import java.util.List;
import java.util.Map;

@Tag(name = "Bookings", description = "Управление бронированиями")
@RestController
@RequestMapping("/api/bookings")
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookingService;
    private final BookingMapper bookingMapper;
    private final UserService userService;

    @Operation(summary = "Занятые периоды автомобиля")
    @GetMapping("/car/{carId}/booked-periods")
    public List<Map<String, String>> bookedPeriods(@PathVariable Long carId) {
        return bookingService.getBookedPeriods(carId);
    }

    @Operation(summary = "Получить все бронирования")
    @GetMapping
    public List<BookingDto> findAll(@AuthenticationPrincipal UserDetails principal) {
        User user = resolveUser(principal);
        if (isAdmin(principal)) {
            return bookingMapper.toDtoList(bookingService.findAll());
        }
        return bookingMapper.toDtoList(bookingService.findByUser(user.getId()));
    }

    @Operation(summary = "Получить бронирование по ID")
    @GetMapping("/{id}")
    public BookingDto findById(@PathVariable Long id,
                               @AuthenticationPrincipal UserDetails principal) {
        Booking booking = bookingService.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Бронирование не найдено"));
        if (!isAdmin(principal) && !booking.getUser().getUsername().equals(principal.getUsername())) {
            throw new AccessDeniedException("Доступ запрещён");
        }
        return bookingMapper.toDto(booking);
    }

    @Operation(summary = "Создать бронирование (текущий пользователь)")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public BookingDto create(@Valid @RequestBody BookingRequest request,
                             @AuthenticationPrincipal UserDetails principal) {
        User user = resolveUser(principal);

        Booking booking = new Booking();
        Car car = new Car();
        car.setId(request.getCarId());
        booking.setCar(car);
        booking.setUser(user);
        booking.setStartDate(request.getStartDate());
        booking.setEndDate(request.getEndDate());

        return bookingMapper.toDto(bookingService.create(booking, request.getInsuranceIds()));
    }

    @Operation(summary = "Подтвердить бронирование")
    @PostMapping("/{id}/confirm")
    public BookingDto confirm(@PathVariable Long id) {
        return bookingMapper.toDto(bookingService.confirm(id));
    }

    @Operation(summary = "Отменить бронирование (текущий пользователь)")
    @PostMapping("/{id}/cancel")
    public BookingDto cancel(@PathVariable Long id,
                             @AuthenticationPrincipal UserDetails principal) {
        User user = resolveUser(principal);
        return bookingMapper.toDto(bookingService.cancel(id, user.getId()));
    }

    @Operation(summary = "Завершить бронирование")
    @PostMapping("/{id}/complete")
    public BookingDto complete(@PathVariable Long id) {
        return bookingMapper.toDto(bookingService.complete(id));
    }

    private User resolveUser(UserDetails principal) {
        return userService.findByUsername(principal.getUsername())
                .orElseThrow(() -> new ResourceNotFoundException("Пользователь не найден"));
    }

    private boolean isAdmin(UserDetails principal) {
        return principal.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ADMIN"));
    }
}