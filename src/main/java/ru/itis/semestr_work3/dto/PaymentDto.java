package ru.itis.semestr_work3.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PaymentDto {
    private Long id;
    private Integer amount;
    private String currency;
    private String status;
    private String method;
    private LocalDateTime paidAt;
    private Long bookingId;
}