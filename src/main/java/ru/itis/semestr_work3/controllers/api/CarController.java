package ru.itis.semestr_work3.controllers.api;

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

@RestController
@RequestMapping("/api/cars")
@RequiredArgsConstructor
public class CarController {

    private final CarService carService;

    @GetMapping("/all")
    public List<Car> findAll() {
        return carService.findAll();
    }

    @GetMapping
    public Page<Car> find(@ModelAttribute CarFilter filter, Pageable pageable) {
        return carService.findCars(filter, pageable);
    }

    @GetMapping("/{id}")
    public Car findById(@PathVariable Long id) {
        return carService.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Автомобиль не найден"));
    }

    @GetMapping("/available")
    public List<Car> findAvailable() {
        return carService.findAvailable();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Car create(@RequestBody Car car) {
        return carService.create(car);
    }

    @PutMapping("/{id}")
    public Car update(@PathVariable Long id, @RequestBody Car car) {
        return carService.update(id, car);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        carService.delete(id);
        return ResponseEntity.noContent().build();
    }
}