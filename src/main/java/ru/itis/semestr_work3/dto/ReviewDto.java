package ru.itis.semestr_work3.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ReviewDto {
    private Long id;
    private Integer rating;
    private String comment;
    private LocalDate createdAt;

    private Long userId;
    private String username;

    private Long carId;
    private String carBrand;
    private String carModel;
}