package ru.itis.semestr_work3.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.itis.semestr_work3.entity.Booking;

import java.time.LocalDate;
import java.util.List;

public interface BookingRepository extends JpaRepository<Booking, Long> {

    @Query("SELECT b FROM Booking b WHERE b.user.id = :userId ORDER BY b.createdAt DESC")
    List<Booking> findByUser(@Param("userId") Long userId);

    @Query("SELECT b FROM Booking b WHERE b.car.id = :carId")
    List<Booking> findByCar(@Param("carId") Long carId);

    @Query("SELECT b FROM Booking b WHERE b.status = :status")
    List<Booking> findByStatus(@Param("status") String status);

    @Query("""
        SELECT b
        FROM Booking b
        WHERE b.car.id = :carId
          AND UPPER(b.status) NOT IN ('CANCELLED', 'COMPLETED')
          AND b.startDate <= :endDate
          AND b.endDate >= :startDate
    """)
    List<Booking> findOverlappingBookings(@Param("carId") Long carId,
                                          @Param("startDate") LocalDate startDate,
                                          @Param("endDate") LocalDate endDate);

    @Query("""
        SELECT b.car, COUNT(b)
        FROM Booking b
        WHERE b.status = 'COMPLETED'
        GROUP BY b.car
        ORDER BY COUNT(b) DESC
    """)
    List<Object[]> findMostBooked();
}