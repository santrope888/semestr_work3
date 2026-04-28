package ru.itis.semestr_work3.specifications;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.jpa.domain.Specification;
import ru.itis.semestr_work3.entity.Car;
import ru.itis.semestr_work3.entity.Review;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class CarSpecifications {

    public static Specification<Car> search(String text) {
        return (Root<Car> root, CriteriaQuery<?> query, CriteriaBuilder cb) -> {
            if (text == null || text.isBlank()) {
                return cb.conjunction();
            }

            String normalizedText = text.trim()
                    .toLowerCase()
                    .replace('ё', 'е')
                    .replace('–', '-');

            String[] words = normalizedText.split("\\s+");

            Expression<String> brand = cb.lower(cb.trim(root.get("brand")));
            Expression<String> model = cb.lower(cb.trim(root.get("model")));

            Expression<String> fullName = cb.lower(
                    cb.concat(
                            cb.concat(cb.trim(root.get("brand")), " "),
                            cb.trim(root.get("model"))
                    )
            );

            List<Predicate> predicates = new ArrayList<>();

            for (String word : words) {
                if (word.isBlank()) {
                    continue;
                }

                String pattern = "%" + word + "%";

                predicates.add(cb.or(
                        cb.like(brand, pattern),
                        cb.like(model, pattern),
                        cb.like(fullName, pattern)
                ));
            }

            if (predicates.isEmpty()) {
                return cb.conjunction();
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    public static Specification<Car> priceBetween(Integer min, Integer max) {
        return (Root<Car> root, CriteriaQuery<?> query, CriteriaBuilder cb) -> {
            if (min == null && max == null) {
                return cb.conjunction();
            }
            if (min == null) {
                return cb.lessThanOrEqualTo(root.get("pricePerDay"), max);
            }
            if (max == null) {
                return cb.greaterThanOrEqualTo(root.get("pricePerDay"), min);
            }
            return cb.between(root.get("pricePerDay"), min, max);
        };
    }

    public static Specification<Car> hasCategories(List<Long> categoryIds) {
        return (Root<Car> root, CriteriaQuery<?> query, CriteriaBuilder cb) -> {
            if (categoryIds == null || categoryIds.isEmpty()) {
                return cb.conjunction();
            }

            List<Long> normalized = categoryIds.stream()
                    .filter(v -> v != null)
                    .toList();

            if (normalized.isEmpty()) {
                return cb.conjunction();
            }

            Expression<Long> categoryExpr = root.get("category").get("id");
            return categoryExpr.in(normalized);
        };
    }

    public static Specification<Car> hasTransmissions(List<String> transmissions) {
        return (Root<Car> root, CriteriaQuery<?> query, CriteriaBuilder cb) -> {
            if (transmissions == null || transmissions.isEmpty()) {
                return cb.conjunction();
            }

            List<String> normalized = transmissions.stream()
                    .filter(v -> v != null && !v.isBlank())
                    .map(String::toLowerCase)
                    .collect(Collectors.toList());

            if (normalized.isEmpty()) {
                return cb.conjunction();
            }

            Expression<String> transmissionExpr = cb.lower(root.get("transmission"));
            return transmissionExpr.in(normalized);
        };
    }

    public static Specification<Car> hasDrives(List<String> drives) {
        return (Root<Car> root, CriteriaQuery<?> query, CriteriaBuilder cb) -> {
            if (drives == null || drives.isEmpty()) {
                return cb.conjunction();
            }

            List<String> normalized = drives.stream()
                    .filter(v -> v != null && !v.isBlank())
                    .map(String::toLowerCase)
                    .collect(Collectors.toList());

            if (normalized.isEmpty()) {
                return cb.conjunction();
            }

            Expression<String> driveExpr = cb.lower(root.get("drive"));
            return driveExpr.in(normalized);
        };
    }

    public static Specification<Car> hasSeats(Integer seats) {
        return (Root<Car> root, CriteriaQuery<?> query, CriteriaBuilder cb) -> {
            if (seats == null) {
                return cb.conjunction();
            }
            return cb.equal(root.get("seats"), seats);
        };
    }

    public static Specification<Car> yearBetween(Integer minYear, Integer maxYear) {
        return (Root<Car> root, CriteriaQuery<?> query, CriteriaBuilder cb) -> {
            if (minYear == null && maxYear == null) {
                return cb.conjunction();
            }
            if (minYear == null) {
                return cb.lessThanOrEqualTo(root.get("year"), maxYear);
            }
            if (maxYear == null) {
                return cb.greaterThanOrEqualTo(root.get("year"), minYear);
            }
            return cb.between(root.get("year"), minYear, maxYear);
        };
    }

    public static Specification<Car> hasEngine(String engine) {
        return (Root<Car> root, CriteriaQuery<?> query, CriteriaBuilder cb) -> {
            if (engine == null || engine.isBlank()) {
                return cb.conjunction();
            }

            return cb.equal(cb.lower(root.get("engine")), engine.trim().toLowerCase());
        };
    }

    public static Specification<Car> hasDrive(String drive) {
        return (Root<Car> root, CriteriaQuery<?> query, CriteriaBuilder cb) -> {
            if (drive == null || drive.isBlank()) {
                return cb.conjunction();
            }
            return cb.like(cb.lower(root.get("drive")), "%" + drive.toLowerCase() + "%");
        };
    }

    public static Specification<Car> isAvailable(Boolean available) {
        return (Root<Car> root, CriteriaQuery<?> query, CriteriaBuilder cb) -> {
            if (available == null) {
                return cb.conjunction();
            }
            return cb.equal(root.get("available"), available);
        };
    }

    public static Specification<Car> hasBrands(List<String> brands) {
        return (Root<Car> root, CriteriaQuery<?> query, CriteriaBuilder cb) -> {
            if (brands == null || brands.isEmpty()) {
                return cb.conjunction();
            }

            List<String> normalized = brands.stream()
                    .filter(v -> v != null && !v.isBlank())
                    .map(String::toLowerCase)
                    .collect(Collectors.toList());

            if (normalized.isEmpty()) {
                return cb.conjunction();
            }

            Expression<String> brandExpr = cb.lower(root.get("brand"));
            return brandExpr.in(normalized);
        };
    }

    public static Specification<Car> hasColors(List<String> colors) {
        return (Root<Car> root, CriteriaQuery<?> query, CriteriaBuilder cb) -> {
            if (colors == null || colors.isEmpty()) {
                return cb.conjunction();
            }

            List<String> normalized = colors.stream()
                    .filter(v -> v != null && !v.isBlank())
                    .map(String::toLowerCase)
                    .collect(Collectors.toList());

            if (normalized.isEmpty()) {
                return cb.conjunction();
            }

            Expression<String> colorExpr = cb.lower(root.get("color"));
            return colorExpr.in(normalized);
        };
    }

    public static Specification<Car> hasRatings(List<Integer> ratings) {
        return (Root<Car> root, CriteriaQuery<?> query, CriteriaBuilder cb) -> {
            if (ratings == null || ratings.isEmpty()) {
                return cb.conjunction();
            }

            List<Integer> normalized = ratings.stream()
                    .filter(v -> v != null && v >= 1 && v <= 5)
                    .distinct()
                    .toList();

            if (normalized.isEmpty()) {
                return cb.conjunction();
            }

            query.distinct(true);

            List<Predicate> predicates = new ArrayList<>();

            for (Integer rating : normalized) {
                Subquery<Double> subquery = query.subquery(Double.class);
                Root<Review> reviewRoot = subquery.from(Review.class);

                subquery.select(cb.avg(reviewRoot.get("rating")))
                        .where(cb.equal(reviewRoot.get("car"), root));

                predicates.add(cb.ge(subquery, rating.doubleValue()));
            }

            return cb.or(predicates.toArray(new Predicate[0]));
        };
    }
}