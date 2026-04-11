package ru.itis.semestr_work3.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.itis.semestr_work3.entity.Insurance;
import ru.itis.semestr_work3.exception.ResourceNotFoundException;
import ru.itis.semestr_work3.repository.InsuranceRepository;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InsuranceServiceTest {

    @Mock
    private InsuranceRepository insuranceRepository;

    @InjectMocks
    private InsuranceService insuranceService;

    @Test
    void findAll_returnsInsurances() {
        Insurance insurance = new Insurance();
        insurance.setId(1L);
        when(insuranceRepository.findAll()).thenReturn(List.of(insurance));

        assertThat(insuranceService.findAll()).containsExactly(insurance);
    }

    @Test
    void findById_whenExists_returnsInsurance() {
        Insurance insurance = new Insurance();
        insurance.setId(1L);
        when(insuranceRepository.findById(1L)).thenReturn(Optional.of(insurance));

        Insurance result = insuranceService.findById(1L);

        assertThat(result).isEqualTo(insurance);
    }

    @Test
    void findById_whenMissing_throwsException() {
        when(insuranceRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> insuranceService.findById(99L));
    }
}
