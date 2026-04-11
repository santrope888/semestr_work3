package ru.itis.semestr_work3.controllers.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.itis.semestr_work3.dto.CarFilter;
import ru.itis.semestr_work3.entity.Car;
import ru.itis.semestr_work3.exception.ResourceNotFoundException;
import ru.itis.semestr_work3.service.CarService;

import java.util.List;

@Tag(name = "Cars", description = "Управление автомобилями")
@RestController
@RequestMapping("/api/cars")
@RequiredArgsConstructor
public class CarController {

    private final CarService carService;

    @Operation(summary = "Получить все автомобили")
    @GetMapping("/all")
    public List<Car> findAll() {
        return carService.findAll();
    }

    @Operation(summary = "Поиск автомобилей с фильтрами и пагинацией")
    @GetMapping
    public Page<Car> find(@ModelAttribute CarFilter filter, Pageable pageable) {
        return carService.findCars(filter, pageable);
    }

    @Operation(summary = "Получить автомобиль по ID")
    @ApiResponse(responseCode = "404", description = "Автомобиль не найден")
    @GetMapping("/{id}")
    public Car findById(@Parameter(description = "ID автомобиля") @PathVariable Long id) {
        return carService.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Автомобиль не найден"));
    }

    @Operation(summary = "Получить доступные для аренды автомобили")
    @GetMapping("/available")
    public List<Car> findAvailable() {
        return carService.findAvailable();
    }

    @Operation(summary = "Создать автомобиль (только ADMIN)")
    @ApiResponse(responseCode = "201", description = "Автомобиль создан")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Car create(@RequestBody Car car) {
        return carService.create(car);
    }

    @Operation(summary = "Обновить автомобиль (только ADMIN)")
    @PutMapping("/{id}")
    public Car update(@PathVariable Long id, @RequestBody Car car) {
        return carService.update(id, car);
    }

    @Operation(summary = "Удалить автомобиль (только ADMIN)")
    @ApiResponse(responseCode = "204", description = "Удалено")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        carService.delete(id);
        return ResponseEntity.noContent().build();
    }
}