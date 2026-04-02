package ru.itis.semestr_work3.specifications;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;
import org.springframework.data.jpa.domain.Specification;
import ru.itis.semestr_work3.entity.Review;

import java.time.LocalDate;

public class ReviewSpecifications {

    public static Specification<Review> hasCar(Long carId) {
        return (Root<Review> root, CriteriaQuery<?> query, CriteriaBuilder cb) -> {
            if (carId == null) {
                return cb.conjunction();
            }
            return cb.equal(root.get("car").get("id"), carId);
        };
    }

    public static Specification<Review> hasUser(Long userId) {
        return (Root<Review> root, CriteriaQuery<?> query, CriteriaBuilder cb) -> {
            if (userId == null) {
                return cb.conjunction();
            }
            return cb.equal(root.get("user").get("id"), userId);
        };
    }

    public static Specification<Review> ratingBetween(Integer minRating, Integer maxRating) {
        return (Root<Review> root, CriteriaQuery<?> query, CriteriaBuilder cb) -> {
            if (minRating == null && maxRating == null) {
                return cb.conjunction();
            }
            if (minRating == null) {
                return cb.lessThanOrEqualTo(root.get("rating"), maxRating);
            }
            if (maxRating == null) {
                return cb.greaterThanOrEqualTo(root.get("rating"), minRating);
            }
            return cb.between(root.get("rating"), minRating, maxRating);
        };
    }

    public static Specification<Review> commentContains(String text) {
        return (Root<Review> root, CriteriaQuery<?> query, CriteriaBuilder cb) -> {
            if (text == null || text.isBlank()) {
                return cb.conjunction();
            }
            return cb.like(cb.lower(root.get("comment")), "%" + text.toLowerCase() + "%");
        };
    }

    public static Specification<Review> createdAfter(LocalDate date) {
        return (Root<Review> root, CriteriaQuery<?> query, CriteriaBuilder cb) -> {
            if (date == null) {
                return cb.conjunction();
            }
            return cb.greaterThanOrEqualTo(root.get("createdAt"), date);
        };
    }

    public static Specification<Review> withComment() {
        return (Root<Review> root, CriteriaQuery<?> query, CriteriaBuilder cb) ->
                cb.and(
                        cb.isNotNull(root.get("comment")),
                        cb.notEqual(cb.trim(root.get("comment")), "")
                );
    }
}