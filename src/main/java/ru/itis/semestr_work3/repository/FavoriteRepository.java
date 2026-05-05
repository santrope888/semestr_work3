package ru.itis.semestr_work3.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.itis.semestr_work3.entity.Favorite;

import java.util.List;
import java.util.Optional;

public interface FavoriteRepository extends JpaRepository<Favorite, Long> {

    List<Favorite> findByUserId(Long userId);

    @Query("SELECT f FROM Favorite f WHERE f.user.id = :userId AND f.car.id = :carId")
    Optional<Favorite> findOne(@Param("userId") Long userId, @Param("carId") Long carId);

    @Query("SELECT CASE WHEN COUNT(f) > 0 THEN true ELSE false END " +
            "FROM Favorite f WHERE f.user.id = :userId AND f.car.id = :carId")
    boolean exists(@Param("userId") Long userId, @Param("carId") Long carId);

    @Modifying
    @Query("DELETE FROM Favorite f WHERE f.user.id = :userId AND f.car.id = :carId")
    void remove(@Param("userId") Long userId, @Param("carId") Long carId);
}