package ru.itis.semestr_work3.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.itis.semestr_work3.entity.Car;
import ru.itis.semestr_work3.repository.CarRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CarService {

    private final CarRepository carRepository;

    public List<Car> findAll() {
        return carRepository.findAll();
    }

    public Optional<Car> findById(Long id) {
        return carRepository.findById(id);
    }

    public List<Car> findByCategory(Long categoryId) {
        return carRepository.findByCategory(categoryId);
    }

    public List<Car> findAvailable() {
        return carRepository.findAvailable();
    }

    public List<Car> findTopRated(double minRating) {
        return carRepository.findTopRated(minRating);
    }

    public Car create(Car car) {
        car.setCreatedAt(LocalDate.now());
        car.setAvailable(true);
        return carRepository.save(car);
    }

    public Car update(Long id, Car carData) {
        Car car = carRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Car not found: " + id));
        car.setBrand(carData.getBrand());
        car.setModel(carData.getModel());
        car.setYear(carData.getYear());
        car.setColor(carData.getColor());
        car.setPricePerDay(carData.getPricePerDay());
        car.setSeats(carData.getSeats());
        car.setTransmission(carData.getTransmission());
        car.setEngine(carData.getEngine());
        car.setDrive(carData.getDrive());
        car.setDescription(carData.getDescription());
        car.setAvailable(carData.getAvailable());
        car.setCategory(carData.getCategory());
        if (carData.getImagePath() != null) {
            car.setImagePath(carData.getImagePath());
        }
        return carRepository.save(car);
    }

    public void delete(Long id) {
        if (!carRepository.existsById(id)) {
            throw new RuntimeException("Car not found: " + id);
        }
        carRepository.deleteById(id);
    }
}