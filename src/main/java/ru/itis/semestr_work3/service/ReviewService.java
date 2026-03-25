package ru.itis.semestr_work3.service;

import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;
import ru.itis.semestr_work3.entity.Review;
import ru.itis.semestr_work3.repository.ReviewRepository;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class ReviewService {
    private ReviewRepository reviewRepository;

    public ReviewService(ReviewRepository reviewRepository) {
        this.reviewRepository = reviewRepository;
    }

    public List<Review> findAll() {
        return reviewRepository.findAll();
    }

    public Optional<Review> findById(Long id) {
        return reviewRepository.findById(id);
    }

    public Review save(Review review) {
        return reviewRepository.save(review);
    }

    public Optional<Review> update(Long id, Review review) {
        if (!reviewRepository.existsById(id)) {
            return Optional.empty();
        }
        review.setId(id);
        Review reviewUpdated = reviewRepository.save(review);
        return Optional.of(reviewUpdated);
    }

    public boolean deleteById(Long id) {
        if (!reviewRepository.existsById(id)) {
            return false;
        }

        reviewRepository.deleteById(id);

        return true;
    }
}
