package ru.itis.semestr_work3.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class BookingDto {
    private Long id;
    private LocalDate startDate;
    private LocalDate endDate;
    private Integer totalPrice;
    private String status;
    private LocalDate createdAt;

    private Long userId;
    private String username;

    private Long carId;
    private String carBrand;
    private String carModel;
    private String carImagePath;

    private PaymentDto payment;
    private List<InsuranceDto> insurances;
}