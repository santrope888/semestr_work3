package ru.itis.semestr_work3.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.Set;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class BookingRequest {

    @NotNull(message = "Не указан автомобиль")
    private Long carId;

    @NotNull(message = "Укажите дату начала")
    @FutureOrPresent(message = "Дата начала не может быть в прошлом")
    private LocalDate startDate;

    @NotNull(message = "Укажите дату окончания")
    @Future(message = "Дата окончания должна быть в будущем")
    private LocalDate endDate;

    private Set<Long> insuranceIds;
}