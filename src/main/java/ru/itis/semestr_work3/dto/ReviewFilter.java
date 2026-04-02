package ru.itis.semestr_work3.dto;

import lombok.Data;

import java.time.LocalDate;

@Data
public class ReviewFilter {
    private Long carId;
    private Long userId;
    private Integer minRating;
    private Integer maxRating;
    private String search;
    private LocalDate createdAfter;
    private Boolean onlyWithComment;
}