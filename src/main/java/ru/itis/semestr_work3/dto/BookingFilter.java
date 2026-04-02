package ru.itis.semestr_work3.dto;

import lombok.Data;

import java.time.LocalDate;

@Data
public class BookingFilter {
    private Long userId;
    private Long carId;
    private String status;
    private Integer minTotalPrice;
    private Integer maxTotalPrice;
    private LocalDate startDateFrom;
    private LocalDate endDateTo;
    private LocalDate createdFrom;
    private LocalDate createdTo;
}