// ─── ReviewController.java ────────────────────────────────────────────────────
package ru.itis.semestr_work3.controllers.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.itis.semestr_work3.entity.Review;
import ru.itis.semestr_work3.service.ReviewService;

import java.util.List;

@Tag(name = "Reviews", description = "Отзывы об автомобилях")
@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    @Operation(summary = "Получить все отзывы")
    @GetMapping
    public List<Review> findAll() {
        return reviewService.findAll();
    }

    @Operation(summary = "Отзывы по автомобилю")
    @GetMapping("/car/{carId}")
    public List<Review> findByCar(@PathVariable Long carId) {
        return reviewService.findByCar(carId);
    }

    @Operation(summary = "Отзывы по пользователю")
    @GetMapping("/user/{userId}")
    public List<Review> findByUser(@PathVariable Long userId) {
        return reviewService.findByUser(userId);
    }

    @Operation(summary = "Оставить отзыв")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Review create(@RequestBody Review review) {
        return reviewService.create(review);
    }

    @Operation(summary = "Удалить отзыв")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        reviewService.delete(id);
        return ResponseEntity.noContent().build();
    }
}