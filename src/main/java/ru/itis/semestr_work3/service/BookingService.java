package ru.itis.semestr_work3.service;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import ru.itis.semestr_work3.dto.BookingFilter;
import ru.itis.semestr_work3.entity.Booking;
import ru.itis.semestr_work3.entity.Car;
import ru.itis.semestr_work3.entity.Insurance;
import ru.itis.semestr_work3.entity.Payment;
import ru.itis.semestr_work3.entity.User;
import ru.itis.semestr_work3.exception.ResourceNotFoundException;
import ru.itis.semestr_work3.repository.BookingRepository;
import ru.itis.semestr_work3.repository.CarRepository;
import ru.itis.semestr_work3.repository.InsuranceRepository;
import ru.itis.semestr_work3.repository.UserRepository;
import ru.itis.semestr_work3.specifications.BookingSpecifications;

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
    private final CarRepository carRepository;
    private final UserRepository userRepository;
    private final EntityManager entityManager;

    public List<Booking> findFilteredBookings(BookingFilter filter) {
        if (filter == null) {
            return bookingRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt"));
        }

        Specification<Booking> specification = Specification
                .where(BookingSpecifications.hasUser(filter.getUserId()))
                .and(BookingSpecifications.hasCar(filter.getCarId()))
                .and(BookingSpecifications.hasStatus(filter.getStatus()))
                .and(BookingSpecifications.totalPriceBetween(filter.getMinTotalPrice(), filter.getMaxTotalPrice()))
                .and(BookingSpecifications.startDateAfter(filter.getStartDateFrom()))
                .and(BookingSpecifications.endDateBefore(filter.getEndDateTo()))
                .and(BookingSpecifications.createdBetween(filter.getCreatedFrom(), filter.getCreatedTo()));

        return bookingRepository.findAll(specification, Sort.by(Sort.Direction.DESC, "createdAt"));
    }

    public List<Booking> findAll() {
        return bookingRepository.findAll();
    }

    public Optional<Booking> findById(Long id) {
        return bookingRepository.findById(id);
    }

    public List<Booking> findByUser(Long userId) {
        return bookingRepository.findAll(
                BookingSpecifications.hasUser(userId),
                Sort.by(Sort.Direction.DESC, "createdAt")
        );
    }

    public List<Booking> findByCar(Long carId) {
        return bookingRepository.findAll(BookingSpecifications.hasCar(carId));
    }

    public List<Booking> findByStatus(String status) {
        return bookingRepository.findAll(BookingSpecifications.hasStatus(status));
    }

    public Booking create(Booking booking, Set<Long> insuranceIds) {
        validateBookingForCreate(booking);

        Long carId = booking.getCar().getId();
        Long userId = booking.getUser().getId();

        Car actualCar = carRepository.findById(carId)
                .orElseThrow(() -> new ResourceNotFoundException("Автомобиль не найден"));

        User actualUser = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Пользователь не найден"));

        LocalDate startDate = booking.getStartDate();
        LocalDate endDate = booking.getEndDate();

        if (Boolean.FALSE.equals(actualCar.getAvailable())) {
            throw new IllegalArgumentException("Автомобиль недоступен для бронирования");
        }

        checkCarAvailabilityForPeriod(actualCar.getId(), startDate, endDate);

        long days = ChronoUnit.DAYS.between(startDate, endDate) + 1;
        int carPrice = actualCar.getPricePerDay() * (int) days;

        int insurancePrice = 0;
        Set<Insurance> selectedInsurances = Set.of();

        if (insuranceIds != null && !insuranceIds.isEmpty()) {
            List<Insurance> insurances = insuranceRepository.findAllById(insuranceIds);

            if (insurances.size() != insuranceIds.size()) {
                throw new IllegalArgumentException("Одна или несколько выбранных страховок не найдены");
            }

            insurancePrice = insurances.stream()
                    .mapToInt(i -> i.getPricePerDay() * (int) days)
                    .sum();

            selectedInsurances = Set.copyOf(insurances);
        }

        booking.setCar(actualCar);
        booking.setUser(actualUser);
        booking.setInsurances(selectedInsurances);
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
        return entityManager.createQuery("""
                SELECT b.car, COUNT(b)
                FROM Booking b
                WHERE b.status = 'COMPLETED'
                GROUP BY b.car
                ORDER BY COUNT(b) DESC
                """, Object[].class)
                .getResultList();
    }

    private void validateBookingForCreate(Booking booking) {
        if (booking == null) {
            throw new IllegalArgumentException("Данные бронирования отсутствуют");
        }

        if (booking.getStartDate() == null || booking.getEndDate() == null) {
            throw new IllegalArgumentException("Нужно указать даты начала и окончания бронирования");
        }

        if (booking.getCar() == null) {
            throw new IllegalArgumentException("Для бронирования необходимо указать автомобиль");
        }

        if (booking.getCar().getId() == null) {
            throw new IllegalArgumentException("Не указан id автомобиля");
        }

        if (booking.getUser() == null) {
            throw new IllegalArgumentException("Для бронирования необходимо указать пользователя");
        }

        if (booking.getUser().getId() == null) {
            throw new IllegalArgumentException("Не указан id пользователя");
        }

        if (booking.getEndDate().isBefore(booking.getStartDate())) {
            throw new IllegalArgumentException("Дата окончания не может быть раньше даты начала");
        }

        if (booking.getStartDate().isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("Дата начала не может быть в прошлом");
        }
    }

    private void checkCarAvailabilityForPeriod(Long carId, LocalDate startDate, LocalDate endDate) {
        Specification<Booking> overlapSpecification =
                BookingSpecifications.overlapsCarPeriod(carId, startDate, endDate);

        if (bookingRepository.count(overlapSpecification) > 0) {
            throw new IllegalArgumentException("Автомобиль уже забронирован на выбранные даты");
        }
    }
}