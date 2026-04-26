package ru.itis.semestr_work3.service;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.itis.semestr_work3.dto.BookingExtrasRequest;
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

import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class BookingService {

    public static final int GPS_PRICE_PER_DAY = 300;
    public static final int CHILD_SEAT_PRICE_PER_DAY = 400;
    public static final int DRIVER_PRICE_PER_DAY = 3500;

    public static final List<String> ALLOWED_LOCATIONS = List.of(
            "Москва — Внуково",
            "Москва — Шереметьево",
            "Москва — Домодедово",
            "Москва — Центр"
    );

    public static final Set<String> ALLOWED_PAYMENT_METHODS = Set.of("CARD", "CASH");

    private static final String BOOKING_NUMBER_PREFIX = "AM-";
    private static final String BOOKING_NUMBER_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final int BOOKING_NUMBER_LENGTH = 6;
    private static final int BOOKING_NUMBER_MAX_ATTEMPTS = 10;

    private final BookingRepository bookingRepository;
    private final InsuranceRepository insuranceRepository;
    private final CarRepository carRepository;
    private final UserRepository userRepository;
    private final EntityManager entityManager;
    private final NotificationService notificationService;
    private final SecureRandom random = new SecureRandom();

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

        return bookingRepository.save(booking);
    }

    @Transactional
    public Booking createWithExtras(BookingExtrasRequest request, Long userId) {
        validateExtrasRequest(request);

        Car car = carRepository.findById(request.getCarId())
                .orElseThrow(() -> new ResourceNotFoundException("Автомобиль не найден"));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Пользователь не найден"));

        if (Boolean.FALSE.equals(car.getAvailable())) {
            throw new IllegalArgumentException("Автомобиль недоступен для бронирования");
        }

        if (request.getStartDate().isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("Дата начала не может быть в прошлом");
        }

        if (request.getEndDate().isBefore(request.getStartDate())) {
            throw new IllegalArgumentException("Дата окончания не может быть раньше даты начала");
        }

        checkCarAvailabilityForPeriod(car.getId(), request.getStartDate(), request.getEndDate());

        long days = ChronoUnit.DAYS.between(request.getStartDate(), request.getEndDate()) + 1;

        int carPrice = car.getPricePerDay() * (int) days;
        int insurancePrice = 0;
        Set<Insurance> selectedInsurances = Set.of();

        if (request.getInsuranceIds() != null && !request.getInsuranceIds().isEmpty()) {
            List<Insurance> insurances = insuranceRepository.findAllById(request.getInsuranceIds());

            if (insurances.size() != request.getInsuranceIds().size()) {
                throw new IllegalArgumentException("Одна или несколько выбранных страховок не найдены");
            }

            insurancePrice = insurances.stream()
                    .mapToInt(i -> i.getPricePerDay() * (int) days)
                    .sum();

            selectedInsurances = Set.copyOf(insurances);
        }

        boolean gps = Boolean.TRUE.equals(request.getGpsNavigator());
        boolean seat = Boolean.TRUE.equals(request.getChildSeat());
        boolean driver = Boolean.TRUE.equals(request.getDriverService());

        int extrasPrice = 0;
        if (gps)    extrasPrice += GPS_PRICE_PER_DAY * (int) days;
        if (seat)   extrasPrice += CHILD_SEAT_PRICE_PER_DAY * (int) days;
        if (driver) extrasPrice += DRIVER_PRICE_PER_DAY * (int) days;

        Booking booking = new Booking();
        booking.setCar(car);
        booking.setUser(user);
        booking.setStartDate(request.getStartDate());
        booking.setEndDate(request.getEndDate());
        booking.setPickupLocation(request.getPickupLocation());
        booking.setReturnLocation(request.getReturnLocation());
        booking.setInsurances(selectedInsurances);
        booking.setGpsNavigator(gps);
        booking.setChildSeat(seat);
        booking.setDriverService(driver);
        booking.setTotalPrice(carPrice + insurancePrice + extrasPrice);
        booking.setStatus("PENDING");
        booking.setCreatedAt(LocalDate.now());
        booking.setBookingNumber(generateUniqueBookingNumber());

        Payment payment = new Payment();
        payment.setAmount(booking.getTotalPrice());
        payment.setCurrency("RUB");
        payment.setMethod(request.getPaymentMethod());
        payment.setStatus("PENDING");
        payment.setBooking(booking);
        booking.setPayment(payment);

        Booking saved = bookingRepository.save(booking);
        log.info("Создана бронь #{} (booking_number={}) для пользователя {}",
                saved.getId(), saved.getBookingNumber(), userId);

        String paymentNote = "CASH".equals(request.getPaymentMethod())
                ? " Оплата при выдаче автомобиля."
                : " Оплачено картой.";
        notificationService.send(
                user,
                "BOOKING_CREATED",
                String.format("Бронь %s оформлена. %s %s — %s.%s",
                        saved.getBookingNumber(),
                        car.getBrand() + " " + car.getModel(),
                        request.getStartDate(),
                        request.getEndDate(),
                        paymentNote)
        );

        return saved;
    }

    @Transactional
    public Booking confirm(Long id) {
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Бронирование не найдено"));

        booking.setStatus("CONFIRMED");
        Booking saved = bookingRepository.save(booking);

        notificationService.send(
                saved.getUser(),
                "BOOKING_CONFIRMED",
                String.format("Бронь %s подтверждена. %s %s — %s. Ждём вас!",
                        saved.getBookingNumber() != null ? saved.getBookingNumber() : "#" + saved.getId(),
                        saved.getCar().getBrand() + " " + saved.getCar().getModel(),
                        saved.getStartDate(),
                        saved.getEndDate())
        );

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

        boolean wasPaidByCard = booking.getPayment() != null
                && "CARD".equals(booking.getPayment().getMethod())
                && "PAID".equals(booking.getPayment().getStatus());

        if (booking.getPayment() != null) {
            booking.getPayment().setStatus("REFUNDED");
        }

        Booking saved = bookingRepository.save(booking);

        String tail = wasPaidByCard
                ? " Возврат средств поступит на карту в течение 3-5 рабочих дней."
                : "";

        notificationService.send(
                saved.getUser(),
                "BOOKING_CANCELLED",
                String.format("Бронь %s отменена. %s %s — %s.%s",
                        saved.getBookingNumber() != null ? saved.getBookingNumber() : "#" + saved.getId(),
                        saved.getCar().getBrand() + " " + saved.getCar().getModel(),
                        saved.getStartDate(),
                        saved.getEndDate(),
                        tail)
        );

        return saved;
    }

    @Transactional
    public Booking complete(Long id) {
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Бронирование не найдено"));

        booking.setStatus("COMPLETED");
        Booking saved = bookingRepository.save(booking);

        notificationService.send(
                saved.getUser(),
                "BOOKING_COMPLETED",
                String.format("Поездка по брони %s завершена. Спасибо, что выбрали нас! Будем рады вашему отзыву.",
                        saved.getBookingNumber() != null ? saved.getBookingNumber() : "#" + saved.getId())
        );

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

    private void validateExtrasRequest(BookingExtrasRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Данные бронирования отсутствуют");
        }

        if (request.getCarId() == null) {
            throw new IllegalArgumentException("Не указан id автомобиля");
        }

        if (request.getStartDate() == null || request.getEndDate() == null) {
            throw new IllegalArgumentException("Нужно указать даты начала и окончания");
        }

        if (request.getPickupLocation() == null
                || !ALLOWED_LOCATIONS.contains(request.getPickupLocation())) {
            throw new IllegalArgumentException("Неверное место выдачи");
        }

        if (request.getReturnLocation() == null
                || !ALLOWED_LOCATIONS.contains(request.getReturnLocation())) {
            throw new IllegalArgumentException("Неверное место возврата");
        }

        if (request.getPaymentMethod() == null
                || !ALLOWED_PAYMENT_METHODS.contains(request.getPaymentMethod())) {
            throw new IllegalArgumentException("Неверный метод оплаты");
        }
    }

    private void checkCarAvailabilityForPeriod(Long carId, LocalDate startDate, LocalDate endDate) {
        Specification<Booking> overlapSpecification =
                BookingSpecifications.overlapsCarPeriod(carId, startDate, endDate);

        if (bookingRepository.count(overlapSpecification) > 0) {
            throw new IllegalArgumentException("Автомобиль уже забронирован на выбранные даты");
        }
    }

    private String generateUniqueBookingNumber() {
        for (int attempt = 0; attempt < BOOKING_NUMBER_MAX_ATTEMPTS; attempt++) {
            String candidate = generateBookingNumberCandidate();
            if (!bookingRepository.existsByBookingNumber(candidate)) {
                return candidate;
            }
        }
        throw new IllegalStateException("Не удалось сгенерировать уникальный номер брони");
    }

    private String generateBookingNumberCandidate() {
        StringBuilder sb = new StringBuilder(BOOKING_NUMBER_PREFIX);
        for (int i = 0; i < BOOKING_NUMBER_LENGTH; i++) {
            sb.append(BOOKING_NUMBER_ALPHABET.charAt(random.nextInt(BOOKING_NUMBER_ALPHABET.length())));
        }
        return sb.toString();
    }
}