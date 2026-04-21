package ru.itis.semestr_work3.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookingExtrasRequest {
    private Long carId;
    private LocalDate startDate;
    private LocalDate endDate;

    private String pickupLocation;
    private String returnLocation;

    private Set<Long> insuranceIds;

    private Boolean gpsNavigator;
    private Boolean childSeat;
    private Boolean driverService;

    private String paymentMethod;
}