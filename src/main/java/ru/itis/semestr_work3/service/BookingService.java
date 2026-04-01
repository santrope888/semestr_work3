package ru.itis.semestr_work3.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.itis.semestr_work3.entity.Booking;
import ru.itis.semestr_work3.entity.Insurance;
import ru.itis.semestr_work3.entity.Payment;
import ru.itis.semestr_work3.exception.ResourceNotFoundException;
import ru.itis.semestr_work3.repository.BookingRepository;
import ru.itis.semestr_work3.repository.InsuranceRepository;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class BookingService {

    private final BookingRepository bookingRepository;
    private final InsuranceRepository insuranceRepository;

    public List<Booking> findAll() {
        return bookingRepository.findAll();
    }

    public Optional<Booking> findById(Long id) {
        return bookingRepository.findById(id);
    }

    public List<Booking> findByUser(Long userId) {
        return bookingRepository.findByUser(userId);
    }

    public List<Booking> findByCar(Long carId) {
        return bookingRepository.findByCar(carId);
    }

    public List<Booking> findByStatus(String status) {
        return bookingRepository.findByStatus(status);
    }

    public Booking create(Booking booking, Set<Long> insuranceIds) {
        if (booking.getStartDate() == null || booking.getEndDate() == null) {
            throw new IllegalArgumentException("Нужно указать даты начала и окончания бронирования");
        }
        if (booking.getCar() == null) {
            throw new IllegalArgumentException("Для бронирования необходимо указать автомобиль");
        }
        if (booking.getUser() == null) {
            throw new IllegalArgumentException("Для бронирования необходимо указать пользователя");
        }
        if (booking.getEndDate().isBefore(booking.getStartDate())) {
            throw new IllegalArgumentException("Дата окончания не может быть раньше даты начала");
        }
        if (booking.getStartDate().isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("Дата начала не может быть в прошлом");
        }

        long days = ChronoUnit.DAYS.between(booking.getStartDate(), booking.getEndDate()) + 1;
        int carPrice = booking.getCar().getPricePerDay() * (int) days;

        int insurancePrice = 0;
        if (insuranceIds != null && !insuranceIds.isEmpty()) {
            List<Insurance> insurances = insuranceRepository.findAllById(insuranceIds);
            booking.setInsurances(Set.copyOf(insurances));
            insurancePrice = insurances.stream()
                    .mapToInt(i -> i.getPricePerDay() * (int) days)
                    .sum();
        }

        booking.setTotalPrice(carPrice + insurancePrice);
        booking.setStatus("PENDING");
        booking.setCreatedAt(LocalDate.now());

        Payment payment = new Payment();
        payment.setAmount(booking.getTotalPrice());
        payment.setCurrency("RUB");
        payment.setStatus("PENDING");
        payment.setBooking(booking);
        booking.setPayment(payment);

        return bookingRepository.save(booking);
    }

    public Booking confirm(Long id) {
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Бронирование не найдено"));
        booking.setStatus("CONFIRMED");
        return bookingRepository.save(booking);
    }

    public Booking cancel(Long id, Long userId) {
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Бронирование не найдено"));

        if (!booking.getUser().getId().equals(userId)) {
            throw new SecurityException("Нельзя отменить чужое бронирование");
        }

        booking.setStatus("CANCELLED");
        if (booking.getPayment() != null) {
            booking.getPayment().setStatus("REFUNDED");
        }

        return bookingRepository.save(booking);
    }

    public Booking complete(Long id) {
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Бронирование не найдено"));
        booking.setStatus("COMPLETED");
        return bookingRepository.save(booking);
    }

    public List<Object[]> findMostBooked() {
        return bookingRepository.findMostBooked();
    }
}