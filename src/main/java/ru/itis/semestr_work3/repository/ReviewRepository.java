package ru.itis.semestr_work3.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.itis.semestr_work3.entity.Review;

public interface ReviewRepository extends JpaRepository<Review,Long> {
}
