package ru.itis.semestr_work3.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.itis.semestr_work3.entity.Review;

import java.util.List;

public interface ReviewRepository extends JpaRepository<Review, Long> {

    @Query("SELECT r FROM Review r WHERE r.car.id = :carId ORDER BY r.createdAt DESC")
    List<Review> findByCar(@Param("carId") Long carId);

    @Query("SELECT r FROM Review r WHERE r.user.id = :userId")
    List<Review> findByUser(@Param("userId") Long userId);

    @Query("SELECT CASE WHEN COUNT(r) > 0 THEN true ELSE false END " +
            "FROM Review r WHERE r.user.id = :userId AND r.car.id = :carId")
    boolean exists(@Param("userId") Long userId, @Param("carId") Long carId);
}