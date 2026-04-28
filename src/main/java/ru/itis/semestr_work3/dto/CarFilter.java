package ru.itis.semestr_work3.dto;

import lombok.Data;

import java.util.List;

@Data
public class CarFilter {
    private String search;

    private Integer minPrice;
    private Integer maxPrice;

    private List<Integer> ratings;
    private List<Long> categoryIds;

    private Integer seats;

    private Integer minYear;
    private Integer maxYear;

    private String engine;

    private Boolean available;

    private List<String> brands;
    private List<String> colors;
    private List<String> transmissions;
    private List<String> drives;

    private String sortBy;
}