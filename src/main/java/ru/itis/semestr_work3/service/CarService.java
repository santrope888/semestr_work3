package ru.itis.semestr_work3.service;

import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;
import ru.itis.semestr_work3.entity.Car;
import ru.itis.semestr_work3.repository.CarRepository;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class CarService {
    private CarRepository carRepository;

    public CarService(CarRepository carRepository) {
        this.carRepository = carRepository;
    }

    public List<Car> findAll() {
        return carRepository.findAll();
    }

    public Optional<Car> findById(Long id) {
        return carRepository.findById(id);
    }

    public Car save(Car car) {
        return carRepository.save(car);
    }

    public Optional<Car> update(Long id, Car car) {
        if (!carRepository.existsById(id)) {
            return Optional.empty();
        }
        car.setId(id);
        Car carUpdated = carRepository.save(car);
        return Optional.of(carUpdated);
    }

    public boolean deleteById(Long id) {
        if (!carRepository.existsById(id)) {
            return false;
        }

        carRepository.deleteById(id);

        return true;
    }
}
