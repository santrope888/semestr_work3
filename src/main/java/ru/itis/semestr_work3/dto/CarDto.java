package ru.itis.semestr_work3.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CarDto {
    private Long id;
    private String brand;
    private String model;
    private Integer year;
    private String color;
    private Integer pricePerDay;
    private Integer seats;
    private String transmission;
    private String engine;
    private String drive;
    private String imagePath;
    private String description;
    private Boolean available;
    private LocalDate createdAt;
    private Long categoryId;
    private String categoryName;
}