package ru.itis.semestr_work3.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import ru.itis.semestr_work3.dto.ReviewFilter;
import ru.itis.semestr_work3.entity.Car;
import ru.itis.semestr_work3.entity.Review;
import ru.itis.semestr_work3.entity.User;
import ru.itis.semestr_work3.exception.ResourceNotFoundException;
import ru.itis.semestr_work3.repository.ReviewRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReviewServiceTest {

    @Mock
    private ReviewRepository reviewRepository;

    @InjectMocks
    private ReviewService reviewService;

    private Review review;

    @BeforeEach
    void setUp() {
        User user = new User();
        user.setId(1L);

        Car car = new Car();
        car.setId(2L);

        review = new Review();
        review.setId(1L);
        review.setUser(user);
        review.setCar(car);
        review.setRating(5);
        review.setComment("Отличный автомобиль");
    }

    @Test
    void findFilteredReviews_withNullFilter_returnsAllSorted() {
        when(reviewRepository.findAll(any(Sort.class))).thenReturn(List.of(review));

        assertThat(reviewService.findFilteredReviews(null)).containsExactly(review);
    }

    @Test
    void findFilteredReviews_withFilter_usesSpecification() {
        when(reviewRepository.findAll(any(Specification.class), any(Sort.class))).thenReturn(List.of(review));

        assertThat(reviewService.findFilteredReviews(new ReviewFilter())).containsExactly(review);
    }

    @Test
    void findFilteredReviews_withOnlyWithComment_addsCommentSpecification() {
        ReviewFilter filter = new ReviewFilter();
        filter.setOnlyWithComment(true);
        when(reviewRepository.findAll(any(Specification.class), any(Sort.class))).thenReturn(List.of(review));

        assertThat(reviewService.findFilteredReviews(filter)).containsExactly(review);
    }

    @Test
    void findAll_returnsAllReviews() {
        when(reviewRepository.findAll()).thenReturn(List.of(review));

        assertThat(reviewService.findAll()).containsExactly(review);
    }

    @Test
    void findByCar_returnsReviewsForCar() {
        when(reviewRepository.findAll(any(Specification.class), any(Sort.class))).thenReturn(List.of(review));

        assertThat(reviewService.findByCar(2L)).containsExactly(review);
    }

    @Test
    void findByUser_returnsReviewsForUser() {
        when(reviewRepository.findAll(any(Specification.class))).thenReturn(List.of(review));

        assertThat(reviewService.findByUser(1L)).containsExactly(review);
    }

    @Test
    void getAverageRating_whenReviewsExist_returnsAverage() {
        Review second = new Review();
        second.setCar(review.getCar());
        second.setRating(3);

        when(reviewRepository.findAll(any(Specification.class), any(Sort.class)))
                .thenReturn(List.of(review, second));

        double result = reviewService.getAverageRating(2L);

        assertThat(result).isEqualTo(4.0);
    }

    @Test
    void getAverageRating_whenNoReviews_returnsZero() {
        when(reviewRepository.findAll(any(Specification.class), any(Sort.class)))
                .thenReturn(List.of());

        double result = reviewService.getAverageRating(2L);

        assertThat(result).isEqualTo(0.0);
    }

    @Test
    void getReviewCount_returnsCount() {
        when(reviewRepository.count(any(Specification.class))).thenReturn(3L);

        long result = reviewService.getReviewCount(2L);

        assertThat(result).isEqualTo(3L);
    }

    @Test
    void getAverageRatings_returnsMapWithAverages() {
        Car secondCar = new Car();
        secondCar.setId(3L);

        Review secondReviewForFirstCar = new Review();
        secondReviewForFirstCar.setCar(review.getCar());
        secondReviewForFirstCar.setRating(3);

        Review reviewForSecondCar = new Review();
        reviewForSecondCar.setCar(secondCar);
        reviewForSecondCar.setRating(4);

        when(reviewRepository.findAll()).thenReturn(List.of(review, secondReviewForFirstCar, reviewForSecondCar));

        var result = reviewService.getAverageRatings(List.of(2L, 3L, 4L));

        assertThat(result.get(2L)).isEqualTo(4.0);
        assertThat(result.get(3L)).isEqualTo(4.0);
        assertThat(result.get(4L)).isEqualTo(0.0);
    }

    @Test
    void getReviewCounts_returnsMapWithCounts() {
        Car secondCar = new Car();
        secondCar.setId(3L);

        Review secondReviewForFirstCar = new Review();
        secondReviewForFirstCar.setCar(review.getCar());

        Review reviewForSecondCar = new Review();
        reviewForSecondCar.setCar(secondCar);

        when(reviewRepository.findAll()).thenReturn(List.of(review, secondReviewForFirstCar, reviewForSecondCar));

        var result = reviewService.getReviewCounts(List.of(2L, 3L, 4L));

        assertThat(result.get(2L)).isEqualTo(2L);
        assertThat(result.get(3L)).isEqualTo(1L);
        assertThat(result.get(4L)).isEqualTo(0L);
    }

    @Test
    void findById_whenExists_returnsReview() {
        when(reviewRepository.findById(1L)).thenReturn(Optional.of(review));

        assertThat(reviewService.findById(1L)).contains(review);
    }

    @Test
    void findById_whenMissing_returnsEmpty() {
        when(reviewRepository.findById(99L)).thenReturn(Optional.empty());

        assertThat(reviewService.findById(99L)).isEmpty();
    }

    @Test
    void create_withValidReview_setsCreatedAtAndSaves() {
        when(reviewRepository.count(any(Specification.class))).thenReturn(0L);
        when(reviewRepository.save(any(Review.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Review result = reviewService.create(review);

        assertThat(result.getCreatedAt()).isEqualTo(LocalDate.now());
        assertThat(result.getRating()).isEqualTo(5);
    }

    @Test
    void create_whenRatingBelowRange_throwsException() {
        review.setRating(0);

        assertThrows(IllegalArgumentException.class, () -> reviewService.create(review));
    }

    @Test
    void create_whenRatingAboveRange_throwsException() {
        review.setRating(6);

        assertThrows(IllegalArgumentException.class, () -> reviewService.create(review));
    }

    @Test
    void create_whenUserIsNull_throwsException() {
        review.setUser(null);

        assertThrows(IllegalArgumentException.class, () -> reviewService.create(review));
    }

    @Test
    void create_whenUserIdIsNull_throwsException() {
        review.getUser().setId(null);

        assertThrows(IllegalArgumentException.class, () -> reviewService.create(review));
    }

    @Test
    void create_whenCarIsNull_throwsException() {
        review.setCar(null);

        assertThrows(IllegalArgumentException.class, () -> reviewService.create(review));
    }

    @Test
    void create_whenCarIdIsNull_throwsException() {
        review.getCar().setId(null);

        assertThrows(IllegalArgumentException.class, () -> reviewService.create(review));
    }

    @Test
    void create_whenReviewAlreadyExists_throwsException() {
        when(reviewRepository.count(any(Specification.class))).thenReturn(1L);

        assertThrows(IllegalArgumentException.class, () -> reviewService.create(review));
    }

    @Test
    void delete_whenReviewExists_deletesById() {
        when(reviewRepository.existsById(1L)).thenReturn(true);

        reviewService.delete(1L);

        verify(reviewRepository).deleteById(1L);
    }

    @Test
    void delete_whenReviewMissing_throwsException() {
        when(reviewRepository.existsById(99L)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class, () -> reviewService.delete(99L));
    }
}