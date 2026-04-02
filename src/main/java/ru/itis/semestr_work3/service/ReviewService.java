package ru.itis.semestr_work3.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import ru.itis.semestr_work3.dto.ReviewFilter;
import ru.itis.semestr_work3.entity.Review;
import ru.itis.semestr_work3.exception.ResourceNotFoundException;
import ru.itis.semestr_work3.repository.ReviewRepository;
import ru.itis.semestr_work3.specifications.ReviewSpecifications;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewRepository reviewRepository;

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

    public List<Review> findAll() {
        return reviewRepository.findAll();
    }

    public List<Review> findByCar(Long carId) {
        return reviewRepository.findAll(
                ReviewSpecifications.hasCar(carId),
                Sort.by(Sort.Direction.DESC, "createdAt")
        );
    }

    public List<Review> findByUser(Long userId) {
        return reviewRepository.findAll(ReviewSpecifications.hasUser(userId));
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

        Specification<Review> existsSpecification = Specification
                .where(ReviewSpecifications.hasUser(review.getUser().getId()))
                .and(ReviewSpecifications.hasCar(review.getCar().getId()));

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
}