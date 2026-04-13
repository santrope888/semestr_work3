package ru.itis.semestr_work3.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CarRequest {

    @NotBlank(message = "Укажите марку")
    private String brand;

    @NotBlank(message = "Укажите модель")
    private String model;

    @NotNull(message = "Укажите год")
    @Min(value = 1900, message = "Некорректный год")
    private Integer year;

    @NotBlank(message = "Укажите цвет")
    private String color;

    @NotNull(message = "Укажите цену за день")
    @Min(value = 1, message = "Цена должна быть положительной")
    private Integer pricePerDay;

    @NotNull(message = "Укажите количество мест")
    @Min(value = 1, message = "Минимум 1 место")
    private Integer seats;

    @NotBlank(message = "Укажите тип трансмиссии")
    private String transmission;

    @NotBlank(message = "Укажите тип двигателя")
    private String engine;

    @NotBlank(message = "Укажите тип привода")
    private String drive;

    private String imagePath;
    private String description;
    private Boolean available;
    private Long categoryId;
}