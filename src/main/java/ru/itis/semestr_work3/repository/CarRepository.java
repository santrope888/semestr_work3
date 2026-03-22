package ru.itis.semestr_work3.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.itis.semestr_work3.entity.Car;

@Repository
public interface CarRepository extends JpaRepository<Car,Long> {
//    List<Car> findAll();
}
