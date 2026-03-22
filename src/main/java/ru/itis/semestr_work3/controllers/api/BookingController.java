package ru.itis.semestr_work3.controllers.api;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.itis.semestr_work3.entity.Booking;
import ru.itis.semestr_work3.repository.BookingRepository;

import java.util.List;

@RestController
@RequestMapping("/booking")
public class BookingController {
    private final BookingRepository bookingRepository;

    public BookingController(BookingRepository bookingRepository) {
        this.bookingRepository = bookingRepository;
    }

    @GetMapping
    public List<Booking> findAll(){
        return bookingRepository.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Booking> findById(@PathVariable Long id){
        return bookingRepository.findById(id).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Booking create(@Valid @RequestBody Booking booking){
        return bookingRepository.save(booking);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Booking> update(@PathVariable Long id, @Valid @RequestBody Booking booking){
        if(!bookingRepository.existsById(id)){
            return ResponseEntity.notFound().build();
        }
        Booking bookingUpdated = bookingRepository.save(booking);
        return ResponseEntity.ok(bookingUpdated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteById(@PathVariable Long id){
        if(!bookingRepository.existsById(id)){
            return ResponseEntity.notFound().build();
        }
        bookingRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
