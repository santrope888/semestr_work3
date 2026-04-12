package ru.itis.semestr_work3.converter;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import ru.itis.semestr_work3.dto.ReviewDto;
import ru.itis.semestr_work3.entity.Review;

import java.util.List;

@Mapper(componentModel = "spring")
public interface ReviewMapper {

    @Mapping(source = "user.id", target = "userId")
    @Mapping(source = "user.username", target = "username")
    @Mapping(source = "car.id", target = "carId")
    @Mapping(source = "car.brand", target = "carBrand")
    @Mapping(source = "car.model", target = "carModel")
    ReviewDto toDto(Review review);

    List<ReviewDto> toDtoList(List<Review> reviews);
}