package ru.itis.semestr_work3.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.itis.semestr_work3.entity.Booking;

import java.util.List;

public interface BookingRepository extends JpaRepository<Booking, Long> {

    @Query("SELECT b FROM Booking b WHERE b.user.id = :userId ORDER BY b.createdAt DESC")
    List<Booking> findByUser(@Param("userId") Long userId);

    @Query("SELECT b FROM Booking b WHERE b.car.id = :carId")
    List<Booking> findByCar(@Param("carId") Long carId);

    @Query("SELECT b FROM Booking b WHERE b.status = :status")
    List<Booking> findByStatus(@Param("status") String status);

    @Query("SELECT b.car, COUNT(b) FROM Booking b " +
            "WHERE b.status = 'COMPLETED' GROUP BY b.car " +
            "HAVING COUNT(b) > (SELECT AVG(sub.cnt) FROM " +
            "(SELECT COUNT(b2) as cnt FROM Booking b2 " +
            "WHERE b2.status = 'COMPLETED' GROUP BY b2.car) sub)")
    List<Object[]> findMostBooked();
}