package ru.itis.semestr_work3.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
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

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookingServiceTest {

    @Mock
    private BookingRepository bookingRepository;
    @Mock
    private InsuranceRepository insuranceRepository;
    @Mock
    private CarRepository carRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private EntityManager entityManager;
    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private BookingService bookingService;

    private Car car;
    private User user;
    private Booking booking;

    @BeforeEach
    void setUp() {
        car = new Car();
        car.setId(1L);
        car.setPricePerDay(1000);
        car.setAvailable(true);

        user = new User();
        user.setId(1L);

        booking = new Booking();
        booking.setId(1L);
        booking.setCar(car);
        booking.setUser(user);
        booking.setStartDate(LocalDate.now().plusDays(1));
        booking.setEndDate(LocalDate.now().plusDays(3));
    }

    @Test
    void findFilteredBookings_withNullFilter_returnsAllSorted() {
        when(bookingRepository.findAll(any(Sort.class))).thenReturn(List.of(booking));

        List<Booking> result = bookingService.findFilteredBookings(null);

        assertThat(result).containsExactly(booking);
        verify(bookingRepository).findAll(any(Sort.class));
    }

    @Test
    void findFilteredBookings_withFilter_usesSpecification() {
        when(bookingRepository.findAll(any(Specification.class), any(Sort.class))).thenReturn(List.of(booking));

        List<Booking> result = bookingService.findFilteredBookings(new BookingFilter());

        assertThat(result).containsExactly(booking);
        verify(bookingRepository).findAll(any(Specification.class), any(Sort.class));
    }

    @Test
    void findAll_returnsAllBookings() {
        when(bookingRepository.findAll()).thenReturn(List.of(booking));

        assertThat(bookingService.findAll()).containsExactly(booking);
    }

    @Test
    void findById_whenExists_returnsBooking() {
        when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));

        assertTrue(bookingService.findById(1L).isPresent());
    }

    @Test
    void findById_whenMissing_returnsEmpty() {
        when(bookingRepository.findById(99L)).thenReturn(Optional.empty());

        assertThat(bookingService.findById(99L)).isEmpty();
    }

    @Test
    void findByUser_returnsUserBookings() {
        when(bookingRepository.findAll(any(Specification.class), any(Sort.class))).thenReturn(List.of(booking));

        assertThat(bookingService.findByUser(1L)).containsExactly(booking);
    }

    @Test
    void findByCar_returnsCarBookings() {
        when(bookingRepository.findAll(any(Specification.class))).thenReturn(List.of(booking));

        assertThat(bookingService.findByCar(1L)).containsExactly(booking);
    }

    @Test
    void findByStatus_returnsBookingsByStatus() {
        when(bookingRepository.findAll(any(Specification.class))).thenReturn(List.of(booking));

        assertThat(bookingService.findByStatus("PENDING")).containsExactly(booking);
    }

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    void getBookedPeriods_returnsPeriodsForActiveBookings() {
        Root<Booking> root = mock(Root.class);
        CriteriaQuery<?> query = mock(CriteriaQuery.class);
        CriteriaBuilder cb = mock(CriteriaBuilder.class);

        Path<Object> carPath = mock(Path.class);
        Path<Object> carIdPath = mock(Path.class);
        Path<Object> statusPath = mock(Path.class);

        Predicate carPredicate = mock(Predicate.class);
        Predicate statusInPredicate = mock(Predicate.class);
        Predicate notCancelledCompleted = mock(Predicate.class);
        Predicate combinedPredicate = mock(Predicate.class);

        when(root.get("car")).thenReturn((Path) carPath);
        when(carPath.get("id")).thenReturn((Path) carIdPath);
        when(cb.equal(carIdPath, 1L)).thenReturn(carPredicate);

        when(root.get("status")).thenReturn((Path) statusPath);
        when(statusPath.in("CANCELLED", "COMPLETED")).thenReturn(statusInPredicate);
        when(cb.not(statusInPredicate)).thenReturn(notCancelledCompleted);

        when(cb.and(carPredicate, notCancelledCompleted)).thenReturn(combinedPredicate);

        when(bookingRepository.findAll(any(Specification.class))).thenAnswer(invocation -> {
            Specification<Booking> spec = invocation.getArgument(0);
            Predicate predicate = spec.toPredicate(root, query, cb);
            assertThat(predicate).isEqualTo(combinedPredicate);
            return List.of(booking);
        });

        List<Map<String, String>> result = bookingService.getBookedPeriods(1L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0)).containsEntry("start", booking.getStartDate().toString());
        assertThat(result.get(0)).containsEntry("end", booking.getEndDate().toString());

        verify(root).get("car");
        verify(carPath).get("id");
        verify(cb).equal(carIdPath, 1L);
        verify(root).get("status");
        verify(statusPath).in("CANCELLED", "COMPLETED");
        verify(cb).not(statusInPredicate);
        verify(cb).and(carPredicate, notCancelledCompleted);
    }

    @Test
    void getBookedPeriods_whenNoBookings_returnsEmptyList() {
        when(bookingRepository.findAll(any(Specification.class))).thenReturn(List.of());

        List<Map<String, String>> result = bookingService.getBookedPeriods(1L);

        assertThat(result).isEmpty();
        verify(bookingRepository).findAll(any(Specification.class));
    }

    @Test
    void create_withValidDataAndNoInsurances_createsPendingBookingAndPayment() {
        when(carRepository.findById(1L)).thenReturn(Optional.of(car));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(bookingRepository.count(any(Specification.class))).thenReturn(0L);
        when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Booking result = bookingService.create(booking, null);

        assertThat(result.getCar()).isEqualTo(car);
        assertThat(result.getUser()).isEqualTo(user);
        assertThat(result.getStatus()).isEqualTo("PENDING");
        assertThat(result.getTotalPrice()).isEqualTo(3000);
        assertThat(result.getInsurances()).isEmpty();
        assertThat(result.getCreatedAt()).isEqualTo(LocalDate.now());
        assertNotNull(result.getPayment());
        assertThat(result.getPayment().getAmount()).isEqualTo(3000);
        assertThat(result.getPayment().getCurrency()).isEqualTo("RUB");
        assertThat(result.getPayment().getStatus()).isEqualTo("PENDING");
        assertThat(result.getPayment().getBooking()).isEqualTo(result);
    }

    @Test
    void create_withValidInsurances_addsInsuranceCost() {
        Insurance insurance = new Insurance();
        insurance.setId(100L);
        insurance.setPricePerDay(200);

        when(carRepository.findById(1L)).thenReturn(Optional.of(car));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(bookingRepository.count(any(Specification.class))).thenReturn(0L);
        when(insuranceRepository.findAllById(Set.of(100L))).thenReturn(List.of(insurance));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Booking result = bookingService.create(booking, Set.of(100L));

        assertThat(result.getTotalPrice()).isEqualTo(3600);
        assertThat(result.getInsurances()).containsExactly(insurance);
        assertThat(result.getPayment().getAmount()).isEqualTo(3600);
    }

    @Test
    void create_whenBookingIsNull_throwsException() {
        assertThrows(IllegalArgumentException.class, () -> bookingService.create(null, null));
    }

    @Test
    void create_whenStartDateIsNull_throwsException() {
        booking.setStartDate(null);

        assertThrows(IllegalArgumentException.class, () -> bookingService.create(booking, null));
    }

    @Test
    void create_whenEndDateIsNull_throwsException() {
        booking.setEndDate(null);

        assertThrows(IllegalArgumentException.class, () -> bookingService.create(booking, null));
    }

    @Test
    void create_whenCarIsNull_throwsException() {
        booking.setCar(null);

        assertThrows(IllegalArgumentException.class, () -> bookingService.create(booking, null));
    }

    @Test
    void create_whenCarIdIsNull_throwsException() {
        car.setId(null);

        assertThrows(IllegalArgumentException.class, () -> bookingService.create(booking, null));
    }

    @Test
    void create_whenUserIsNull_throwsException() {
        booking.setUser(null);

        assertThrows(IllegalArgumentException.class, () -> bookingService.create(booking, null));
    }

    @Test
    void create_whenUserIdIsNull_throwsException() {
        user.setId(null);

        assertThrows(IllegalArgumentException.class, () -> bookingService.create(booking, null));
    }

    @Test
    void create_whenEndDateBeforeStartDate_throwsException() {
        booking.setStartDate(LocalDate.now().plusDays(5));
        booking.setEndDate(LocalDate.now().plusDays(3));

        assertThrows(IllegalArgumentException.class, () -> bookingService.create(booking, null));
    }

    @Test
    void create_whenStartDateInPast_throwsException() {
        booking.setStartDate(LocalDate.now().minusDays(1));

        assertThrows(IllegalArgumentException.class, () -> bookingService.create(booking, null));
    }

    @Test
    void create_whenCarNotFound_throwsException() {
        when(carRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> bookingService.create(booking, null));
    }

    @Test
    void create_whenUserNotFound_throwsException() {
        when(carRepository.findById(1L)).thenReturn(Optional.of(car));
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> bookingService.create(booking, null));
    }

    @Test
    void create_whenCarUnavailable_throwsException() {
        car.setAvailable(false);
        when(carRepository.findById(1L)).thenReturn(Optional.of(car));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        assertThrows(IllegalArgumentException.class, () -> bookingService.create(booking, null));
    }

    @Test
    void create_whenCarAlreadyBookedForPeriod_throwsException() {
        when(carRepository.findById(1L)).thenReturn(Optional.of(car));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(bookingRepository.count(any(Specification.class))).thenReturn(1L);

        assertThrows(IllegalArgumentException.class, () -> bookingService.create(booking, null));
    }

    @Test
    void create_whenSomeInsuranceMissing_throwsException() {
        when(carRepository.findById(1L)).thenReturn(Optional.of(car));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(bookingRepository.count(any(Specification.class))).thenReturn(0L);
        when(insuranceRepository.findAllById(Set.of(1L, 2L))).thenReturn(List.of());

        assertThrows(IllegalArgumentException.class, () -> bookingService.create(booking, Set.of(1L, 2L)));
    }

    @Test
    void confirm_whenBookingExists_setsConfirmedStatus() {
        when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Booking result = bookingService.confirm(1L);

        assertThat(result.getStatus()).isEqualTo("CONFIRMED");
    }

    @Test
    void confirm_whenBookingMissing_throwsException() {
        when(bookingRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> bookingService.confirm(1L));
    }

    @Test
    void cancel_whenOwnerCancelsAndPaymentExists_refundsPayment() {
        Payment payment = new Payment();
        payment.setStatus("PENDING");
        booking.setPayment(payment);

        when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Booking result = bookingService.cancel(1L, 1L);

        assertThat(result.getStatus()).isEqualTo("CANCELLED");
        assertThat(result.getPayment().getStatus()).isEqualTo("REFUNDED");
    }

    @Test
    void cancel_whenOwnerCancelsWithoutPayment_justCancelsBooking() {
        booking.setPayment(null);
        when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Booking result = bookingService.cancel(1L, 1L);

        assertThat(result.getStatus()).isEqualTo("CANCELLED");
    }

    @Test
    void cancel_whenAnotherUserCancels_throwsSecurityException() {
        when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));

        assertThrows(SecurityException.class, () -> bookingService.cancel(1L, 99L));
        verify(bookingRepository, never()).save(any(Booking.class));
    }

    @Test
    void cancel_whenBookingMissing_throwsException() {
        when(bookingRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> bookingService.cancel(1L, 1L));
    }

    @Test
    void complete_whenBookingExists_setsCompletedStatus() {
        when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Booking result = bookingService.complete(1L);

        assertThat(result.getStatus()).isEqualTo("COMPLETED");
    }

    @Test
    void complete_whenBookingMissing_throwsException() {
        when(bookingRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> bookingService.complete(1L));
    }

    @Test
    @SuppressWarnings("unchecked")
    void findMostBooked_returnsQueryResult() {
        TypedQuery<Object[]> query = mock(TypedQuery.class);
        Object[] row = new Object[]{car, 3L};

        when(entityManager.createQuery(anyString(), eq(Object[].class))).thenReturn(query);
        when(query.getResultList()).thenReturn(java.util.Collections.singletonList(row));

        List<Object[]> result = bookingService.findMostBooked();

        assertThat(result).containsExactly(row);
        verify(entityManager).createQuery(anyString(), eq(Object[].class));
    }

    private ru.itis.semestr_work3.dto.BookingExtrasRequest validExtrasRequest() {
        return ru.itis.semestr_work3.dto.BookingExtrasRequest.builder()
                .carId(1L)
                .startDate(LocalDate.now().plusDays(1))
                .endDate(LocalDate.now().plusDays(3))
                .pickupLocation("Москва — Внуково")
                .returnLocation("Москва — Шереметьево")
                .paymentMethod("CARD")
                .build();
    }

    @Test
    void createWithExtras_minimalValidRequest_createsBookingWithBookingNumber() {
        when(carRepository.findById(1L)).thenReturn(Optional.of(car));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(bookingRepository.count(any(Specification.class))).thenReturn(0L);
        when(bookingRepository.existsByBookingNumber(anyString())).thenReturn(false);
        when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> inv.getArgument(0));

        Booking result = bookingService.createWithExtras(validExtrasRequest(), 1L);

        assertThat(result.getStatus()).isEqualTo("PENDING");
        assertThat(result.getTotalPrice()).isEqualTo(3000);
        assertThat(result.getBookingNumber()).startsWith("AM-");
        assertThat(result.getBookingNumber()).hasSize(9);
        assertThat(result.getPickupLocation()).isEqualTo("Москва — Внуково");
        assertThat(result.getReturnLocation()).isEqualTo("Москва — Шереметьево");
        assertThat(result.getGpsNavigator()).isFalse();
        assertThat(result.getChildSeat()).isFalse();
        assertThat(result.getDriverService()).isFalse();
        assertThat(result.getPayment().getMethod()).isEqualTo("CARD");
        assertThat(result.getInsurances()).isEmpty();
    }

    @Test
    void createWithExtras_allOptionsEnabled_addsExtrasCost() {
        ru.itis.semestr_work3.dto.BookingExtrasRequest request = validExtrasRequest();
        request.setGpsNavigator(true);
        request.setChildSeat(true);
        request.setDriverService(true);

        when(carRepository.findById(1L)).thenReturn(Optional.of(car));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(bookingRepository.count(any(Specification.class))).thenReturn(0L);
        when(bookingRepository.existsByBookingNumber(anyString())).thenReturn(false);
        when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> inv.getArgument(0));

        Booking result = bookingService.createWithExtras(request, 1L);

        assertThat(result.getTotalPrice()).isEqualTo(15600);
        assertThat(result.getGpsNavigator()).isTrue();
        assertThat(result.getChildSeat()).isTrue();
        assertThat(result.getDriverService()).isTrue();
    }

    @Test
    void createWithExtras_withInsurances_addsInsuranceCost() {
        ru.itis.semestr_work3.dto.BookingExtrasRequest request = validExtrasRequest();
        request.setInsuranceIds(Set.of(100L));
        request.setPaymentMethod("CASH");

        Insurance insurance = new Insurance();
        insurance.setId(100L);
        insurance.setPricePerDay(500);

        when(carRepository.findById(1L)).thenReturn(Optional.of(car));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(bookingRepository.count(any(Specification.class))).thenReturn(0L);
        when(insuranceRepository.findAllById(Set.of(100L))).thenReturn(List.of(insurance));
        when(bookingRepository.existsByBookingNumber(anyString())).thenReturn(false);
        when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> inv.getArgument(0));

        Booking result = bookingService.createWithExtras(request, 1L);

        assertThat(result.getTotalPrice()).isEqualTo(4500);
        assertThat(result.getPayment().getMethod()).isEqualTo("CASH");
        assertThat(result.getInsurances()).containsExactly(insurance);
    }

    @Test
    void createWithExtras_nullRequest_throwsException() {
        assertThrows(IllegalArgumentException.class,
                () -> bookingService.createWithExtras(null, 1L));
    }

    @Test
    void createWithExtras_nullCarId_throwsException() {
        ru.itis.semestr_work3.dto.BookingExtrasRequest request = validExtrasRequest();
        request.setCarId(null);

        assertThrows(IllegalArgumentException.class,
                () -> bookingService.createWithExtras(request, 1L));
    }

    @Test
    void createWithExtras_nullStartDate_throwsException() {
        ru.itis.semestr_work3.dto.BookingExtrasRequest request = validExtrasRequest();
        request.setStartDate(null);

        assertThrows(IllegalArgumentException.class,
                () -> bookingService.createWithExtras(request, 1L));
    }

    @Test
    void createWithExtras_nullEndDate_throwsException() {
        ru.itis.semestr_work3.dto.BookingExtrasRequest request = validExtrasRequest();
        request.setEndDate(null);

        assertThrows(IllegalArgumentException.class,
                () -> bookingService.createWithExtras(request, 1L));
    }

    @Test
    void createWithExtras_invalidPickupLocation_throwsException() {
        ru.itis.semestr_work3.dto.BookingExtrasRequest request = validExtrasRequest();
        request.setPickupLocation("Несуществующий аэропорт");

        assertThrows(IllegalArgumentException.class,
                () -> bookingService.createWithExtras(request, 1L));
    }

    @Test
    void createWithExtras_nullPickupLocation_throwsException() {
        ru.itis.semestr_work3.dto.BookingExtrasRequest request = validExtrasRequest();
        request.setPickupLocation(null);

        assertThrows(IllegalArgumentException.class,
                () -> bookingService.createWithExtras(request, 1L));
    }

    @Test
    void createWithExtras_invalidReturnLocation_throwsException() {
        ru.itis.semestr_work3.dto.BookingExtrasRequest request = validExtrasRequest();
        request.setReturnLocation("Marrakech");

        assertThrows(IllegalArgumentException.class,
                () -> bookingService.createWithExtras(request, 1L));
    }

    @Test
    void createWithExtras_nullReturnLocation_throwsException() {
        ru.itis.semestr_work3.dto.BookingExtrasRequest request = validExtrasRequest();
        request.setReturnLocation(null);

        assertThrows(IllegalArgumentException.class,
                () -> bookingService.createWithExtras(request, 1L));
    }

    @Test
    void createWithExtras_invalidPaymentMethod_throwsException() {
        ru.itis.semestr_work3.dto.BookingExtrasRequest request = validExtrasRequest();
        request.setPaymentMethod("BITCOIN");

        assertThrows(IllegalArgumentException.class,
                () -> bookingService.createWithExtras(request, 1L));
    }

    @Test
    void createWithExtras_nullPaymentMethod_throwsException() {
        ru.itis.semestr_work3.dto.BookingExtrasRequest request = validExtrasRequest();
        request.setPaymentMethod(null);

        assertThrows(IllegalArgumentException.class,
                () -> bookingService.createWithExtras(request, 1L));
    }

    @Test
    void createWithExtras_carNotFound_throwsException() {
        when(carRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> bookingService.createWithExtras(validExtrasRequest(), 1L));
    }

    @Test
    void createWithExtras_userNotFound_throwsException() {
        when(carRepository.findById(1L)).thenReturn(Optional.of(car));
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> bookingService.createWithExtras(validExtrasRequest(), 1L));
    }

    @Test
    void createWithExtras_carUnavailable_throwsException() {
        car.setAvailable(false);
        when(carRepository.findById(1L)).thenReturn(Optional.of(car));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        assertThrows(IllegalArgumentException.class,
                () -> bookingService.createWithExtras(validExtrasRequest(), 1L));
    }

    @Test
    void createWithExtras_startDateInPast_throwsException() {
        ru.itis.semestr_work3.dto.BookingExtrasRequest request = validExtrasRequest();
        request.setStartDate(LocalDate.now().minusDays(1));

        when(carRepository.findById(1L)).thenReturn(Optional.of(car));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        assertThrows(IllegalArgumentException.class,
                () -> bookingService.createWithExtras(request, 1L));
    }

    @Test
    void createWithExtras_endBeforeStart_throwsException() {
        ru.itis.semestr_work3.dto.BookingExtrasRequest request = validExtrasRequest();
        request.setStartDate(LocalDate.now().plusDays(5));
        request.setEndDate(LocalDate.now().plusDays(2));

        when(carRepository.findById(1L)).thenReturn(Optional.of(car));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        assertThrows(IllegalArgumentException.class,
                () -> bookingService.createWithExtras(request, 1L));
    }

    @Test
    void createWithExtras_periodOverlap_throwsException() {
        when(carRepository.findById(1L)).thenReturn(Optional.of(car));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(bookingRepository.count(any(Specification.class))).thenReturn(1L);

        assertThrows(IllegalArgumentException.class,
                () -> bookingService.createWithExtras(validExtrasRequest(), 1L));
    }

    @Test
    void createWithExtras_someInsuranceMissing_throwsException() {
        ru.itis.semestr_work3.dto.BookingExtrasRequest request = validExtrasRequest();
        request.setInsuranceIds(Set.of(100L, 200L));

        when(carRepository.findById(1L)).thenReturn(Optional.of(car));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(bookingRepository.count(any(Specification.class))).thenReturn(0L);
        when(insuranceRepository.findAllById(Set.of(100L, 200L)))
                .thenReturn(List.of(new Insurance()));

        assertThrows(IllegalArgumentException.class,
                () -> bookingService.createWithExtras(request, 1L));
    }

    @Test
    void createWithExtras_bookingNumberCollision_retriesAndSucceeds() {
        when(carRepository.findById(1L)).thenReturn(Optional.of(car));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(bookingRepository.count(any(Specification.class))).thenReturn(0L);
        when(bookingRepository.existsByBookingNumber(anyString()))
                .thenReturn(true)
                .thenReturn(false);
        when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> inv.getArgument(0));

        Booking result = bookingService.createWithExtras(validExtrasRequest(), 1L);

        assertThat(result.getBookingNumber()).startsWith("AM-");
        verify(bookingRepository, org.mockito.Mockito.times(2)).existsByBookingNumber(anyString());
    }

    @Test
    void createWithExtras_bookingNumberAlwaysCollides_throwsException() {
        when(carRepository.findById(1L)).thenReturn(Optional.of(car));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(bookingRepository.count(any(Specification.class))).thenReturn(0L);
        when(bookingRepository.existsByBookingNumber(anyString())).thenReturn(true);

        assertThrows(IllegalStateException.class,
                () -> bookingService.createWithExtras(validExtrasRequest(), 1L));
    }

    @Test
    void createWithExtras_sendsNotification() {
        when(carRepository.findById(1L)).thenReturn(Optional.of(car));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(bookingRepository.count(any(Specification.class))).thenReturn(0L);
        when(bookingRepository.existsByBookingNumber(anyString())).thenReturn(false);
        when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> inv.getArgument(0));

        bookingService.createWithExtras(validExtrasRequest(), 1L);

        verify(notificationService).send(eq(user), eq("BOOKING_CREATED"), anyString());
    }
    @Test
    void cancel_whenOwnerCancelsPaidCardBooking_sendsRefundMessage() {
        Payment payment = new Payment();
        payment.setMethod("CARD");
        payment.setStatus("PAID");

        booking.setPayment(payment);
        booking.setBookingNumber("AM-ABC123");

        car.setBrand("BMW");
        car.setModel("X6");

        when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Booking result = bookingService.cancel(1L, 1L);

        assertThat(result.getStatus()).isEqualTo("CANCELLED");
        assertThat(result.getPayment().getStatus()).isEqualTo("REFUNDED");

        org.mockito.ArgumentCaptor<String> messageCaptor =
                org.mockito.ArgumentCaptor.forClass(String.class);

        verify(notificationService).send(
                eq(user),
                eq("BOOKING_CANCELLED"),
                messageCaptor.capture()
        );

        assertThat(messageCaptor.getValue()).contains("AM-ABC123");
        assertThat(messageCaptor.getValue()).contains("Возврат средств поступит на карту");
    }
}