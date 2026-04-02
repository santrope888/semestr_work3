package ru.itis.semestr_work3.specifications;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;
import org.springframework.data.jpa.domain.Specification;
import ru.itis.semestr_work3.entity.Booking;

import java.time.LocalDate;

public class BookingSpecifications {

    public static Specification<Booking> hasUser(Long userId) {
        return (Root<Booking> root, CriteriaQuery<?> query, CriteriaBuilder cb) -> {
            if (userId == null) {
                return cb.conjunction();
            }
            return cb.equal(root.get("user").get("id"), userId);
        };
    }

    public static Specification<Booking> hasCar(Long carId) {
        return (Root<Booking> root, CriteriaQuery<?> query, CriteriaBuilder cb) -> {
            if (carId == null) {
                return cb.conjunction();
            }
            return cb.equal(root.get("car").get("id"), carId);
        };
    }

    public static Specification<Booking> hasStatus(String status) {
        return (Root<Booking> root, CriteriaQuery<?> query, CriteriaBuilder cb) -> {
            if (status == null || status.isBlank()) {
                return cb.conjunction();
            }
            return cb.equal(cb.upper(root.get("status")), status.toUpperCase());
        };
    }

    public static Specification<Booking> totalPriceBetween(Integer min, Integer max) {
        return (Root<Booking> root, CriteriaQuery<?> query, CriteriaBuilder cb) -> {
            if (min == null && max == null) {
                return cb.conjunction();
            }
            if (min == null) {
                return cb.lessThanOrEqualTo(root.get("totalPrice"), max);
            }
            if (max == null) {
                return cb.greaterThanOrEqualTo(root.get("totalPrice"), min);
            }
            return cb.between(root.get("totalPrice"), min, max);
        };
    }

    public static Specification<Booking> startDateAfter(LocalDate date) {
        return (Root<Booking> root, CriteriaQuery<?> query, CriteriaBuilder cb) -> {
            if (date == null) {
                return cb.conjunction();
            }
            return cb.greaterThanOrEqualTo(root.get("startDate"), date);
        };
    }

    public static Specification<Booking> endDateBefore(LocalDate date) {
        return (Root<Booking> root, CriteriaQuery<?> query, CriteriaBuilder cb) -> {
            if (date == null) {
                return cb.conjunction();
            }
            return cb.lessThanOrEqualTo(root.get("endDate"), date);
        };
    }

    public static Specification<Booking> createdBetween(LocalDate from, LocalDate to) {
        return (Root<Booking> root, CriteriaQuery<?> query, CriteriaBuilder cb) -> {
            if (from == null && to == null) {
                return cb.conjunction();
            }
            if (from == null) {
                return cb.lessThanOrEqualTo(root.get("createdAt"), to);
            }
            if (to == null) {
                return cb.greaterThanOrEqualTo(root.get("createdAt"), from);
            }
            return cb.between(root.get("createdAt"), from, to);
        };
    }

    public static Specification<Booking> overlapsCarPeriod(Long carId, LocalDate startDate, LocalDate endDate) {
        return (Root<Booking> root, CriteriaQuery<?> query, CriteriaBuilder cb) -> {
            if (carId == null || startDate == null || endDate == null) {
                return cb.conjunction();
            }

            return cb.and(
                    cb.equal(root.get("car").get("id"), carId),
                    cb.not(root.get("status").in("CANCELLED", "COMPLETED")),
                    cb.lessThanOrEqualTo(root.get("startDate"), endDate),
                    cb.greaterThanOrEqualTo(root.get("endDate"), startDate)
            );
        };
    }
}