package ru.itis.semestr_work3.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import ru.itis.semestr_work3.entity.Car;

public interface CarRepository extends JpaRepository<Car, Long>, JpaSpecificationExecutor<Car> {
}