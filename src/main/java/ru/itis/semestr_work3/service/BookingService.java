package ru.itis.semestr_work3.service;

import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;
import ru.itis.semestr_work3.entity.Booking;
import ru.itis.semestr_work3.repository.BookingRepository;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class BookingService {
    private BookingRepository bookingRepository;

    public BookingService(BookingRepository bookingRepository) {
        this.bookingRepository = bookingRepository;
    }

    public List<Booking> findAll() {
        return bookingRepository.findAll();
    }

    public Optional<Booking> findById(Long id) {
        return bookingRepository.findById(id);
    }

    public Booking save(Booking booking) {
        return bookingRepository.save(booking);
    }

    public Optional<Booking> update(Long id, Booking booking) {
        if (!bookingRepository.existsById(id)) {
            return Optional.empty();
        }
        booking.setId(id);
        Booking bookingUpdated = bookingRepository.save(booking);
        return Optional.of(bookingUpdated);
    }

    public boolean deleteById(Long id) {
        if (!bookingRepository.existsById(id)) {
            return false;
        }

        bookingRepository.deleteById(id);

        return true;
    }
}
