package ru.itis.semestr_work3.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.itis.semestr_work3.entity.Review;
import ru.itis.semestr_work3.repository.ReviewRepository;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewRepository reviewRepository;

    public List<Review> findAll() {
        return reviewRepository.findAll();
    }

    public List<Review> findByCar(Long carId) {
        return reviewRepository.findByCar(carId);
    }

    public List<Review> findByUser(Long userId) {
        return reviewRepository.findByUser(userId);
    }

    public Review create(Review review) {
        if (review.getRating() < 1 || review.getRating() > 5) {
            throw new IllegalArgumentException("Рейтинг должен быть от 1 до 5");
        }
        if (reviewRepository.exists(review.getUser().getId(), review.getCar().getId())) {
            throw new IllegalArgumentException("Вы уже оставляли отзыв на этот автомобиль");
        }
        review.setCreatedAt(LocalDate.now());
        return reviewRepository.save(review);
    }

    public void delete(Long id) {
        if (!reviewRepository.existsById(id)) {
            throw new RuntimeException("Review not found: " + id);
        }
        reviewRepository.deleteById(id);
    }
}