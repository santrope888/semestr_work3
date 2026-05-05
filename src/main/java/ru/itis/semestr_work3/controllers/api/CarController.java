package ru.itis.semestr_work3.controllers.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.itis.semestr_work3.converter.CarMapper;
import ru.itis.semestr_work3.dto.CarDto;
import ru.itis.semestr_work3.dto.CarFilter;
import ru.itis.semestr_work3.dto.CarRequest;
import ru.itis.semestr_work3.entity.Car;
import ru.itis.semestr_work3.entity.Category;
import ru.itis.semestr_work3.exception.ResourceNotFoundException;
import ru.itis.semestr_work3.service.CarService;
import ru.itis.semestr_work3.service.CategoryService;

import java.util.List;

@Tag(name = "Cars", description = "Управление автомобилями")
@RestController
@RequestMapping("/api/cars")
@RequiredArgsConstructor
public class CarController {

    private final CarService carService;
    private final CarMapper carMapper;
    private final CategoryService categoryService;

    @Operation(summary = "Получить все автомобили")
    @GetMapping("/all")
    public List<CarDto> findAll() {
        return carMapper.toDtoList(carService.findAll());
    }

    @Operation(summary = "Поиск автомобилей с фильтрами и пагинацией")
    @GetMapping
    public Page<CarDto> find(@ModelAttribute CarFilter filter, Pageable pageable) {
        return carService.findCars(filter, pageable).map(carMapper::toDto);
    }

    @Operation(summary = "Получить автомобиль по ID")
    @ApiResponse(responseCode = "404", description = "Автомобиль не найден")
    @GetMapping("/{id}")
    public CarDto findById(@Parameter(description = "ID автомобиля") @PathVariable Long id) {
        Car car = carService.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Автомобиль не найден"));
        return carMapper.toDto(car);
    }

    @Operation(summary = "Получить доступные для аренды автомобили")
    @GetMapping("/available")
    public List<CarDto> findAvailable() {
        return carMapper.toDtoList(carService.findAvailable());
    }

    @Operation(summary = "Создать автомобиль (только ADMIN)")
    @ApiResponse(responseCode = "201", description = "Автомобиль создан")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CarDto create(@Valid @RequestBody CarRequest request) {
        Car car = mapRequestToCar(request, new Car());
        return carMapper.toDto(carService.create(car));
    }

    @Operation(summary = "Обновить автомобиль (только ADMIN)")
    @PutMapping("/{id}")
    public CarDto update(@PathVariable Long id, @Valid @RequestBody CarRequest request) {
        Car car = mapRequestToCar(request, new Car());
        return carMapper.toDto(carService.update(id, car));
    }

    @Operation(summary = "Удалить автомобиль (только ADMIN)")
    @ApiResponse(responseCode = "204", description = "Удалено")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        carService.delete(id);
        return ResponseEntity.noContent().build();
    }

    private Car mapRequestToCar(CarRequest request, Car car) {
        car.setBrand(request.getBrand());
        car.setModel(request.getModel());
        car.setYear(request.getYear());
        car.setColor(request.getColor());
        car.setPricePerDay(request.getPricePerDay());
        car.setSeats(request.getSeats());
        car.setTransmission(request.getTransmission());
        car.setEngine(request.getEngine());
        car.setDrive(request.getDrive());
        car.setDescription(request.getDescription());
        car.setImagePath(request.getImagePath());
        car.setAvailable(request.getAvailable());

        if (request.getCategoryId() != null) {
            Category category = categoryService.findById(request.getCategoryId());
            car.setCategory(category);
        }

        return car;
    }
}