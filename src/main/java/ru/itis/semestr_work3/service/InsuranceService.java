package ru.itis.semestr_work3.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.itis.semestr_work3.entity.Insurance;
import ru.itis.semestr_work3.exception.ResourceNotFoundException;
import ru.itis.semestr_work3.repository.InsuranceRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InsuranceService {

    private final InsuranceRepository insuranceRepository;

    public List<Insurance> findAll() {
        return insuranceRepository.findAll();
    }

    public Insurance findById(Long id) {
        return insuranceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Страховка не найдена: " + id));
    }
}