package ru.itis.semestr_work3.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class InsuranceDto {
    private Long id;
    private String name;
    private String description;
    private Integer pricePerDay;
    private String icon;
}