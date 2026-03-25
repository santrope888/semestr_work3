package ru.itis.semestr_work3.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.itis.semestr_work3.entity.Car;

import java.util.List;

public interface CarRepository extends JpaRepository<Car, Long> {

    @Query("SELECT c FROM Car c WHERE c.category.id = :categoryId")
    List<Car> findByCategory(@Param("categoryId") Long categoryId);

    @Query("SELECT c FROM Car c WHERE c.available = true")
    List<Car> findAvailable();

    @Query("SELECT c FROM Car c JOIN c.reviews r " +
            "GROUP BY c HAVING AVG(r.rating) >= :min " +
            "ORDER BY AVG(r.rating) DESC")
    List<Car> findTopRated(@Param("min") double min);
}