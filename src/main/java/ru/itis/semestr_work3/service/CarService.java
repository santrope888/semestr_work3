package ru.itis.semestr_work3.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.itis.semestr_work3.dto.CarFilter;
import ru.itis.semestr_work3.entity.Car;
import ru.itis.semestr_work3.exception.ResourceNotFoundException;
import ru.itis.semestr_work3.repository.CarRepository;
import ru.itis.semestr_work3.specifications.CarSpecifications;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class CarService {

    private final CarRepository carRepository;

    @Transactional(readOnly = true)
    public Page<Car> findCars(CarFilter filter, Pageable pageable) {
        if (filter == null) {
            filter = new CarFilter();
        }

        Pageable sortedPageable = applySorting(filter, pageable);

        Specification<Car> specification = Specification
                .where(CarSpecifications.search(filter.getSearch()))
                .and(CarSpecifications.priceBetween(filter.getMinPrice(), filter.getMaxPrice()))
                .and(CarSpecifications.hasRatings(filter.getRatings()))
                .and(CarSpecifications.hasCategories(filter.getCategoryIds()))
                .and(CarSpecifications.hasTransmissions(filter.getTransmissions()))
                .and(CarSpecifications.hasSeats(filter.getSeats()))
                .and(CarSpecifications.yearBetween(filter.getMinYear(), filter.getMaxYear()))
                .and(CarSpecifications.hasEngine(filter.getEngine()))
                .and(CarSpecifications.hasDrives(filter.getDrives()))
                .and(CarSpecifications.isAvailable(filter.getAvailable()))
                .and(CarSpecifications.hasBrands(filter.getBrands()))
                .and(CarSpecifications.hasColors(filter.getColors()));

        return carRepository.findAll(specification, sortedPageable);
    }

    private Pageable applySorting(CarFilter filter, Pageable pageable) {
        int page = pageable != null ? pageable.getPageNumber() : 0;
        int size = pageable != null && pageable.getPageSize() > 0 ? pageable.getPageSize() : 12;

        Sort sort = resolveSort(filter.getSortBy());

        return PageRequest.of(page, size, sort);
    }

    private Sort resolveSort(String sortBy) {
        if (sortBy == null || sortBy.isBlank()) {
            return Sort.by(Sort.Direction.DESC, "createdAt");
        }

        return switch (sortBy) {
            case "priceAsc" -> Sort.by(Sort.Direction.ASC, "pricePerDay");
            case "priceDesc" -> Sort.by(Sort.Direction.DESC, "pricePerDay");
            case "yearDesc" -> Sort.by(Sort.Direction.DESC, "year");
            case "yearAsc" -> Sort.by(Sort.Direction.ASC, "year");
            case "brandAsc" -> Sort.by(Sort.Direction.ASC, "brand");
            case "modelAsc" -> Sort.by(Sort.Direction.ASC, "model");
            default -> Sort.by(Sort.Direction.DESC, "createdAt");
        };
    }

    @Transactional(readOnly = true)
    public List<String> findDistinctBrands() {
        return carRepository.findAll().stream()
                .map(Car::getBrand)
                .filter(v -> v != null && !v.isBlank())
                .distinct()
                .sorted(String::compareToIgnoreCase)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<String> findDistinctColors() {
        return carRepository.findAll().stream()
                .map(Car::getColor)
                .filter(v -> v != null && !v.isBlank())
                .distinct()
                .sorted(String::compareToIgnoreCase)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<String> findDistinctTransmissions() {
        return carRepository.findAll().stream()
                .map(Car::getTransmission)
                .filter(v -> v != null && !v.isBlank())
                .distinct()
                .sorted(String::compareToIgnoreCase)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<String> findDistinctDrives() {
        return carRepository.findAll().stream()
                .map(Car::getDrive)
                .filter(v -> v != null && !v.isBlank())
                .distinct()
                .sorted(String::compareToIgnoreCase)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<Car> findAll() {
        return carRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Optional<Car> findById(Long id) {
        return carRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public List<Car> findAvailable() {
        return carRepository.findAll(CarSpecifications.isAvailable(true));
    }

    public Car create(Car car) {
        car.setCreatedAt(LocalDate.now());
        if (car.getAvailable() == null) {
            car.setAvailable(true);
        }
        return carRepository.save(car);
    }

    public Car update(Long id, Car carData) {
        Car car = carRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Автомобиль не найден: " + id));

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
            throw new ResourceNotFoundException("Автомобиль не найден: " + id);
        }
        carRepository.deleteById(id);
    }
}