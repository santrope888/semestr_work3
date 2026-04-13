package ru.itis.semestr_work3.controllers.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import ru.itis.semestr_work3.converter.FavoriteMapper;
import ru.itis.semestr_work3.dto.FavoriteDto;
import ru.itis.semestr_work3.entity.Car;
import ru.itis.semestr_work3.entity.User;
import ru.itis.semestr_work3.exception.ResourceNotFoundException;
import ru.itis.semestr_work3.service.CarService;
import ru.itis.semestr_work3.service.FavoriteService;
import ru.itis.semestr_work3.service.UserService;

import java.util.List;
import java.util.Map;

@Tag(name = "Favorites", description = "Избранные автомобили пользователя")
@RestController
@RequestMapping("/api/favorites")
@RequiredArgsConstructor
public class FavoriteController {

    private final FavoriteService favoriteService;
    private final UserService userService;
    private final CarService carService;
    private final FavoriteMapper favoriteMapper;

    @Operation(summary = "Получить избранное текущего пользователя")
    @GetMapping
    public List<FavoriteDto> findMy(@AuthenticationPrincipal UserDetails principal) {
        User user = resolveUser(principal);
        return favoriteMapper.toDtoList(favoriteService.findByUser(user.getId()));
    }

    @Operation(summary = "Проверить, добавлен ли автомобиль в избранное")
    @GetMapping("/check")
    public Map<String, Boolean> check(@AuthenticationPrincipal UserDetails principal,
                                      @RequestParam Long carId) {
        User user = resolveUser(principal);
        return Map.of("isFavorite", favoriteService.isFavorite(user.getId(), carId));
    }

    @Operation(summary = "Добавить в избранное")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public FavoriteDto add(@AuthenticationPrincipal UserDetails principal,
                           @RequestParam Long carId) {
        User user = resolveUser(principal);
        Car car = carService.findById(carId)
                .orElseThrow(() -> new ResourceNotFoundException("Автомобиль не найден"));
        return favoriteMapper.toDto(favoriteService.add(user, car));
    }

    @Operation(summary = "Удалить из избранного")
    @DeleteMapping
    public ResponseEntity<Void> remove(@AuthenticationPrincipal UserDetails principal,
                                       @RequestParam Long carId) {
        User user = resolveUser(principal);
        favoriteService.remove(user.getId(), carId);
        return ResponseEntity.noContent().build();
    }

    private User resolveUser(UserDetails principal) {
        return userService.findByUsername(principal.getUsername())
                .orElseThrow(() -> new ResourceNotFoundException("Пользователь не найден"));
    }
}