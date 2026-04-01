package ru.itis.semestr_work3.controllers.api;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.itis.semestr_work3.entity.Car;
import ru.itis.semestr_work3.entity.Favorite;
import ru.itis.semestr_work3.entity.User;
import ru.itis.semestr_work3.exception.ResourceNotFoundException;
import ru.itis.semestr_work3.service.CarService;
import ru.itis.semestr_work3.service.FavoriteService;
import ru.itis.semestr_work3.service.UserService;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/favorites")
@RequiredArgsConstructor
public class FavoriteController {

    private final FavoriteService favoriteService;
    private final UserService userService;
    private final CarService carService;

    @GetMapping("/user/{userId}")
    public List<Favorite> findByUser(@PathVariable Long userId) {
        return favoriteService.findByUser(userId);
    }

    @GetMapping("/check")
    public Map<String, Boolean> check(@RequestParam Long userId,
                                      @RequestParam Long carId) {
        return Map.of("isFavorite", favoriteService.isFavorite(userId, carId));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Favorite add(@RequestParam Long userId,
                        @RequestParam Long carId) {
        User user = userService.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Пользователь не найден"));

        Car car = carService.findById(carId)
                .orElseThrow(() -> new ResourceNotFoundException("Автомобиль не найден"));

        return favoriteService.add(user, car);
    }

    @DeleteMapping
    public ResponseEntity<Void> remove(@RequestParam Long userId,
                                       @RequestParam Long carId) {
        favoriteService.remove(userId, carId);
        return ResponseEntity.noContent().build();
    }
}