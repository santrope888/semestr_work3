package ru.itis.semestr_work3.service;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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
import java.util.Map;
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
    private final NotificationService notificationService;

    @Transactional(readOnly = true)
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

    @Transactional(readOnly = true)
    public List<Booking> findAll() {
        return bookingRepository.findAll();
    }

    @Transactional(readOnly = true)
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

    public List<Map<String, String>> getBookedPeriods(Long carId) {
        Specification<Booking> spec = Specification
                .where(BookingSpecifications.hasCar(carId))
                .and((root, query, cb) ->
                        cb.not(root.get("status").in("CANCELLED", "COMPLETED")));

        return bookingRepository.findAll(spec).stream()
                .map(b -> Map.of(
                        "start", b.getStartDate().toString(),
                        "end", b.getEndDate().toString()))
                .toList();
    }

    @Transactional
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

        Booking saved = bookingRepository.save(booking);

        notificationService.send(actualUser, "BOOKING_CREATED",
                "Бронирование #" + saved.getId() + " создано: "
                        + actualCar.getBrand() + " " + actualCar.getModel()
                        + " (" + startDate + " — " + endDate + "). Ожидает оплаты.");

        return saved;
    }

    @Transactional
    public Booking confirm(Long id) {
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Бронирование не найдено"));

        booking.setStatus("CONFIRMED");
        Booking saved = bookingRepository.save(booking);

        notificationService.send(booking.getUser(), "BOOKING_CONFIRMED",
                "Бронирование #" + id + " подтверждено: "
                        + booking.getCar().getBrand() + " " + booking.getCar().getModel() + ".");

        return saved;
    }

    @Transactional
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

        Booking saved = bookingRepository.save(booking);

        notificationService.send(booking.getUser(), "BOOKING_CANCELLED",
                "Бронирование #" + id + " отменено: "
                        + booking.getCar().getBrand() + " " + booking.getCar().getModel() + ".");

        return saved;
    }

    @Transactional
    public Booking complete(Long id) {
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Бронирование не найдено"));

        booking.setStatus("COMPLETED");
        Booking saved = bookingRepository.save(booking);

        notificationService.send(booking.getUser(), "BOOKING_COMPLETED",
                "Бронирование #" + id + " завершено. Спасибо за использование "
                        + booking.getCar().getBrand() + " " + booking.getCar().getModel()
                        + "! Оставьте отзыв.");

        return saved;
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
        if (booking.getCar() == null || booking.getCar().getId() == null) {
            throw new IllegalArgumentException("Для бронирования необходимо указать автомобиль");
        }
        if (booking.getUser() == null || booking.getUser().getId() == null) {
            throw new IllegalArgumentException("Для бронирования необходимо указать пользователя");
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