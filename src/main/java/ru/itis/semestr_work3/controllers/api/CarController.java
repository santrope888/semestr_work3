package ru.itis.semestr_work3.controllers.api;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.itis.semestr_work3.entity.Car;
import ru.itis.semestr_work3.repository.CarRepository;

import java.util.List;

@RestController
@RequestMapping("/car")
public class CarController {
    private final CarRepository carRepository;

    public CarController(CarRepository carRepository) {
        this.carRepository = carRepository;
    }

    @GetMapping
    public List<Car> findAll(){
        return carRepository.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Car> findById(@PathVariable Long id){
        return carRepository.findById(id).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Car create(@Valid @RequestBody Car car){
        return carRepository.save(car);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Car> update(@PathVariable Long id, @Valid @RequestBody Car car){
        if(!carRepository.existsById(id)){
            return ResponseEntity.notFound().build();
        }
        Car carUpdated = carRepository.save(car);
        return ResponseEntity.ok(carUpdated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteById(@PathVariable Long id){
        if(!carRepository.existsById(id)){
            return ResponseEntity.notFound().build();
        }
        carRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
