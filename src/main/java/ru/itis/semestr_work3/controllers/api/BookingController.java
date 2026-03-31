package ru.itis.semestr_work3.controllers.api;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.itis.semestr_work3.entity.Booking;
import ru.itis.semestr_work3.service.BookingService;

import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/booking")
public class BookingController {
    private final BookingService bookingService;

    public BookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @GetMapping
    public List<Booking> findAll() {
        return bookingService.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Booking> findById(@PathVariable Long id) {
        return bookingService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Booking create(@Valid @RequestBody Booking booking,
                          @RequestParam(required = false) Set<Long> insuranceIds) {
        return bookingService.create(booking, insuranceIds);
    }

    @PostMapping("/{id}/confirm")
    public Booking confirm(@PathVariable Long id) {
        return bookingService.confirm(id);
    }

    @PostMapping("/{id}/cancel")
    public Booking cancel(@PathVariable Long id, @RequestParam Long userId) {
        return bookingService.cancel(id, userId);
    }

    @PostMapping("/{id}/complete")
    public Booking complete(@PathVariable Long id) {
        return bookingService.complete(id);
    }
}