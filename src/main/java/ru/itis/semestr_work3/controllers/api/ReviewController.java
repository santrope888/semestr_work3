package ru.itis.semestr_work3.controllers.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import ru.itis.semestr_work3.converter.ReviewMapper;
import ru.itis.semestr_work3.dto.ReviewDto;
import ru.itis.semestr_work3.dto.ReviewRequest;
import ru.itis.semestr_work3.entity.Car;
import ru.itis.semestr_work3.entity.Review;
import ru.itis.semestr_work3.entity.User;
import ru.itis.semestr_work3.exception.ResourceNotFoundException;
import ru.itis.semestr_work3.service.ReviewService;
import ru.itis.semestr_work3.service.UserService;

import java.util.List;

@Tag(name = "Reviews", description = "Отзывы об автомобилях")
@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;
    private final ReviewMapper reviewMapper;
    private final UserService userService;

    @Operation(summary = "Получить все отзывы")
    @GetMapping
    public List<ReviewDto> findAll() {
        return reviewMapper.toDtoList(reviewService.findAll());
    }

    @Operation(summary = "Отзывы по автомобилю")
    @GetMapping("/car/{carId}")
    public List<ReviewDto> findByCar(@PathVariable Long carId) {
        return reviewMapper.toDtoList(reviewService.findByCar(carId));
    }

    @Operation(summary = "Отзывы по пользователю")
    @GetMapping("/user/{userId}")
    public List<ReviewDto> findByUser(@PathVariable Long userId) {
        return reviewMapper.toDtoList(reviewService.findByUser(userId));
    }

    @Operation(summary = "Оставить отзыв (текущий пользователь)")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ReviewDto create(@Valid @RequestBody ReviewRequest request,
                            @AuthenticationPrincipal UserDetails principal) {
        User user = userService.findByUsername(principal.getUsername())
                .orElseThrow(() -> new ResourceNotFoundException("Пользователь не найден"));

        Review review = new Review();
        review.setUser(user);

        Car car = new Car();
        car.setId(request.getCarId());
        review.setCar(car);

        review.setRating(request.getRating());
        review.setComment(request.getComment());

        return reviewMapper.toDto(reviewService.create(review));
    }

    @Operation(summary = "Удалить отзыв")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id,
                                       @AuthenticationPrincipal UserDetails principal) {
        Review review = reviewService.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Отзыв не найден"));
        boolean isAdmin = principal.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ADMIN"));
        if (!isAdmin && !review.getUser().getUsername().equals(principal.getUsername())) {
            throw new AccessDeniedException("Доступ запрещён");
        }
        reviewService.delete(id);
        return ResponseEntity.noContent().build();
    }
}