package ru.itis.semestr_work3.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.itis.semestr_work3.entity.Insurance;

public interface InsuranceRepository extends JpaRepository<Insurance, Long> {
}