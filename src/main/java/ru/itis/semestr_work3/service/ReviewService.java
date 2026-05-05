package ru.itis.semestr_work3.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.itis.semestr_work3.dto.ReviewFilter;
import ru.itis.semestr_work3.entity.Car;
import ru.itis.semestr_work3.entity.Review;
import ru.itis.semestr_work3.entity.User;
import ru.itis.semestr_work3.exception.ResourceNotFoundException;
import ru.itis.semestr_work3.repository.CarRepository;
import ru.itis.semestr_work3.repository.ReviewRepository;
import ru.itis.semestr_work3.repository.UserRepository;
import ru.itis.semestr_work3.specifications.ReviewSpecifications;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final CarRepository carRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<Review> findFilteredReviews(ReviewFilter filter) {
        if (filter == null) {
            return reviewRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt"));
        }

        Specification<Review> specification = Specification
                .where(ReviewSpecifications.hasCar(filter.getCarId()))
                .and(ReviewSpecifications.hasUser(filter.getUserId()))
                .and(ReviewSpecifications.ratingBetween(filter.getMinRating(), filter.getMaxRating()))
                .and(ReviewSpecifications.commentContains(filter.getSearch()))
                .and(ReviewSpecifications.createdAfter(filter.getCreatedAfter()));

        if (Boolean.TRUE.equals(filter.getOnlyWithComment())) {
            specification = specification.and(ReviewSpecifications.withComment());
        }

        return reviewRepository.findAll(specification, Sort.by(Sort.Direction.DESC, "createdAt"));
    }

    @Transactional(readOnly = true)
    public List<Review> findAll() {
        return reviewRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<Review> findByCar(Long carId) {
        return reviewRepository.findAll(
                ReviewSpecifications.hasCar(carId),
                Sort.by(Sort.Direction.DESC, "createdAt")
        );
    }

    @Transactional(readOnly = true)
    public List<Review> findByUser(Long userId) {
        return reviewRepository.findAll(ReviewSpecifications.hasUser(userId));
    }

    @Transactional(readOnly = true)
    public double getAverageRating(Long carId) {
        List<Review> reviews = findByCar(carId);
        if (reviews.isEmpty()) return 0.0;
        return reviews.stream()
                .mapToInt(Review::getRating)
                .average()
                .orElse(0.0);
    }

    @Transactional(readOnly = true)
    public long getReviewCount(Long carId) {
        return reviewRepository.count(ReviewSpecifications.hasCar(carId));
    }

    @Transactional(readOnly = true)
    public Map<Long, Double> getAverageRatings(List<Long> carIds) {
        Map<Long, Double> result = new HashMap<>();
        List<Review> allReviews = reviewRepository.findAll();
        for (Long carId : carIds) {
            double avg = allReviews.stream()
                    .filter(r -> r.getCar().getId().equals(carId))
                    .mapToInt(Review::getRating)
                    .average()
                    .orElse(0.0);
            result.put(carId, avg);
        }
        return result;
    }

    @Transactional(readOnly = true)
    public Map<Long, Long> getReviewCounts(List<Long> carIds) {
        Map<Long, Long> result = new HashMap<>();
        List<Review> allReviews = reviewRepository.findAll();
        for (Long carId : carIds) {
            long count = allReviews.stream()
                    .filter(r -> r.getCar().getId().equals(carId))
                    .count();
            result.put(carId, count);
        }
        return result;
    }

    public Review create(Review review) {
        if (review.getRating() < 1 || review.getRating() > 5) {
            throw new IllegalArgumentException("Рейтинг должен быть от 1 до 5");
        }

        if (review.getUser() == null || review.getUser().getId() == null) {
            throw new IllegalArgumentException("Не указан пользователь");
        }

        if (review.getCar() == null || review.getCar().getId() == null) {
            throw new IllegalArgumentException("Не указан автомобиль");
        }

        User user = userRepository.findById(review.getUser().getId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Пользователь не найден: " + review.getUser().getId()));

        Car car = carRepository.findById(review.getCar().getId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Автомобиль не найден: " + review.getCar().getId()));

        review.setUser(user);
        review.setCar(car);

        Specification<Review> existsSpecification = Specification
                .where(ReviewSpecifications.hasUser(user.getId()))
                .and(ReviewSpecifications.hasCar(car.getId()));

        if (reviewRepository.count(existsSpecification) > 0) {
            throw new IllegalArgumentException("Вы уже оставляли отзыв на этот автомобиль");
        }

        review.setCreatedAt(LocalDate.now());
        return reviewRepository.save(review);
    }

    public void delete(Long id) {
        if (!reviewRepository.existsById(id)) {
            throw new ResourceNotFoundException("Отзыв не найден: " + id);
        }

        reviewRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public Optional<Review> findById(Long id) {
        return reviewRepository.findById(id);
    }
}