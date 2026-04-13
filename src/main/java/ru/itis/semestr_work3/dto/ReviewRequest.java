package ru.itis.semestr_work3.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ReviewRequest {

    @NotNull(message = "Не указан автомобиль")
    private Long carId;

    @NotNull(message = "Укажите рейтинг")
    @Min(value = 1, message = "Рейтинг от 1 до 5")
    @Max(value = 5, message = "Рейтинг от 1 до 5")
    private Integer rating;

    @Size(max = 1000, message = "Комментарий не более 1000 символов")
    private String comment;
}